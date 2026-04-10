package org.hunmr.location;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;

import javax.swing.SwingConstants;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

public final class CollectedOutputFileManager {
    private static final String OUTPUT_DIR_NAME = "emacsJump";
    private static final String OUTPUT_FILE_PREFIX = "emacsJump-collected-context-";
    private static final String OUTPUT_FILE_SUFFIX = ".txt";

    private CollectedOutputFileManager() {
    }

    public static String getCurrentText(Project project) {
        VirtualFile outputFile = findOpenOutputFile(project);
        if (outputFile == null) {
            return "";
        }

        Document document = FileDocumentManager.getInstance().getDocument(outputFile);
        if (document != null) {
            return document.getText();
        }

        try {
            return new String(outputFile.contentsToByteArray(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    public static VirtualFile appendAndOpen(Project project, String text) throws IOException {
        return replaceAndOpen(project, mergeCurrentText(project, text), null);
    }

    public static VirtualFile appendAndOpen(Project project, String text, Editor sourceEditor) throws IOException {
        return replaceAndOpen(project, mergeCurrentText(project, text), sourceEditor);
    }

    public static Path getOutputDirectoryPath() {
        String tempDirectory = System.getProperty("java.io.tmpdir");
        if (tempDirectory != null && !tempDirectory.isEmpty()) {
            return new File(tempDirectory, OUTPUT_DIR_NAME).toPath();
        }

        String systemPath = PathManager.getSystemPath();
        if (systemPath != null && !systemPath.isEmpty()) {
            return new File(systemPath, OUTPUT_DIR_NAME).toPath();
        }

        return new File(new File(".").getAbsoluteFile(), OUTPUT_DIR_NAME).toPath();
    }

    public static VirtualFile replaceAndOpen(Project project, String text) throws IOException {
        return replaceAndOpen(project, text, null);
    }

    public static VirtualFile replaceAndOpen(Project project, String text, Editor sourceEditor) throws IOException {
        if (project == null || text == null || text.isEmpty()) {
            return null;
        }

        FocusRestoreState focusRestoreState = FocusRestoreState.capture(project, sourceEditor);
        VirtualFile outputFile = null;
        try {
            outputFile = findOpenOutputFile(project);
            if (outputFile == null) {
                outputFile = createNewOutputFile(project);
            }

            allowWritingWithoutDialog(outputFile);
            replaceText(project, outputFile, text);
            Document document = getOrCreateDocument(outputFile);
            if (document != null) {
                FileDocumentManager.getInstance().saveDocument(document);
            }
            openInRightSplit(project, outputFile);
            return outputFile;
        } finally {
            focusRestoreState.restore(project);
        }
    }

    private static String mergeCurrentText(Project project, String appendedText) {
        String currentText = getCurrentText(project);
        if (currentText.isEmpty()) {
            return appendedText;
        }
        return currentText + appendedText;
    }

    private static VirtualFile findOpenOutputFile(Project project) {
        if (project == null) {
            return null;
        }

        VirtualFile[] openFiles = FileEditorManager.getInstance(project).getOpenFiles();
        for (int i = openFiles.length - 1; i >= 0; i--) {
            VirtualFile file = openFiles[i];
            if (isCollectedOutputFile(file)) {
                return file;
            }
        }
        return null;
    }

    private static boolean isCollectedOutputFile(VirtualFile file) {
        if (file == null) {
            return false;
        }

        String name = file.getName();
        return name.startsWith(OUTPUT_FILE_PREFIX) && name.endsWith(OUTPUT_FILE_SUFFIX);
    }

    private static VirtualFile createNewOutputFile(Project project) throws IOException {
        Path outputDirectory = getOutputDirectory(project);
        Files.createDirectories(outputDirectory);
        Path outputPath = Files.createTempFile(outputDirectory, OUTPUT_FILE_PREFIX, OUTPUT_FILE_SUFFIX);
        VirtualFile outputFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputPath.toFile());
        if (outputFile == null) {
            throw new IOException("Cannot open output file in local file system: " + outputPath);
        }
        return outputFile;
    }

    private static void replaceText(Project project, VirtualFile outputFile, String text) throws IOException {
        Document document = getOrCreateDocument(outputFile);
        if (document == null) {
            Files.write(new File(outputFile.getPath()).toPath(), text.getBytes(StandardCharsets.UTF_8));
            outputFile.refresh(false, false);
            return;
        }

        final Document targetDocument = document;
        final String safeText = text;
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                targetDocument.setText(safeText);
            }
        });
    }

    private static Document getOrCreateDocument(VirtualFile outputFile) {
        Document document = FileDocumentManager.getInstance().getDocument(outputFile);
        if (document != null) {
            return document;
        }
        return FileDocumentManager.getInstance().getCachedDocument(outputFile);
    }

    private static void openInRightSplit(Project project, VirtualFile outputFile) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager == null) {
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
            return;
        }

        com.intellij.openapi.fileEditor.impl.EditorWindow currentWindow = manager.getCurrentWindow();
        if (currentWindow == null) {
            com.intellij.openapi.fileEditor.impl.EditorWindow[] windows = manager.getWindows();
            if (windows != null && windows.length > 0) {
                currentWindow = windows[0];
            }
        }

        if (currentWindow == null) {
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
            return;
        }

        com.intellij.openapi.fileEditor.impl.EditorWindow targetWindow = manager.getNextWindow(currentWindow);
        if (targetWindow == null) {
            manager.setCurrentWindow(currentWindow);
            manager.createSplitter(SwingConstants.VERTICAL, currentWindow);
            targetWindow = manager.getNextWindow(currentWindow);
        }

        if (targetWindow != null) {
            manager.setCurrentWindow(targetWindow);
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
            return;
        }

        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
    }

    private static Path getOutputDirectory(Project project) {
        return getOutputDirectoryPath();
    }

    private static void allowWritingWithoutDialog(VirtualFile outputFile) {
        if (outputFile == null || !outputFile.isValid()) {
            return;
        }

        invokeAllowWriting(outputFile);

        VirtualFile parent = outputFile.getParent();
        if (parent != null && parent.isValid()) {
            invokeAllowWriting(parent);
        }
    }

    private static void invokeAllowWriting(VirtualFile file) {
        try {
            Class<?> providerClass = Class.forName("com.intellij.openapi.fileEditor.impl.NonProjectFileWritingAccessProvider");
            tryInvokeAllowWriting(providerClass, file);
        } catch (Throwable ignored) {
        }
    }

    private static void tryInvokeAllowWriting(Class<?> providerClass, VirtualFile file) throws ReflectiveOperationException {
        try {
            Method iterableMethod = providerClass.getMethod("allowWriting", Iterable.class);
            iterableMethod.invoke(null, Collections.singleton(file));
            return;
        } catch (NoSuchMethodException ignored) {
        }

        Method arrayMethod = providerClass.getMethod("allowWriting", VirtualFile[].class);
        arrayMethod.invoke(null, new Object[]{new VirtualFile[]{file}});
    }

    private static final class FocusRestoreState {
        private final Editor editor;
        private final VirtualFile file;
        private final int caretOffset;
        private final boolean hasSelection;
        private final int selectionStart;
        private final int selectionEnd;

        private FocusRestoreState(Editor editor,
                                  VirtualFile file,
                                  int caretOffset,
                                  boolean hasSelection,
                                  int selectionStart,
                                  int selectionEnd) {
            this.editor = editor;
            this.file = file;
            this.caretOffset = caretOffset;
            this.hasSelection = hasSelection;
            this.selectionStart = selectionStart;
            this.selectionEnd = selectionEnd;
        }

        private static FocusRestoreState capture(Project project, Editor sourceEditor) {
            Editor editor = isUsableEditor(sourceEditor) ? sourceEditor : FileEditorManager.getInstance(project).getSelectedTextEditor();
            if (!isUsableEditor(editor)) {
                return new FocusRestoreState(null, null, 0, false, 0, 0);
            }

            VirtualFile file = FileDocumentManager.getInstance().getFile(editor.getDocument());
            if (file == null || !file.isValid() || isCollectedOutputFile(file)) {
                return new FocusRestoreState(null, null, 0, false, 0, 0);
            }

            SelectionModel selectionModel = editor.getSelectionModel();
            return new FocusRestoreState(
                    editor,
                    file,
                    editor.getCaretModel().getOffset(),
                    selectionModel.hasSelection(),
                    selectionModel.getSelectionStart(),
                    selectionModel.getSelectionEnd()
            );
        }

        private void restore(Project project) {
            Editor targetEditor = resolveEditor(project);
            if (!isUsableEditor(targetEditor)) {
                return;
            }

            Document document = targetEditor.getDocument();
            int textLength = document.getTextLength();
            int safeCaretOffset = Math.max(0, Math.min(caretOffset, textLength));
            targetEditor.getCaretModel().moveToOffset(safeCaretOffset);

            SelectionModel selectionModel = targetEditor.getSelectionModel();
            if (hasSelection) {
                int safeSelectionStart = Math.max(0, Math.min(selectionStart, textLength));
                int safeSelectionEnd = Math.max(safeSelectionStart, Math.min(selectionEnd, textLength));
                selectionModel.setSelection(safeSelectionStart, safeSelectionEnd);
            } else {
                selectionModel.removeSelection();
            }

            targetEditor.getScrollingModel().scrollToCaret(ScrollType.MAKE_VISIBLE);
            IdeFocusManager.getGlobalInstance().requestFocus(targetEditor.getContentComponent(), true);
        }

        private Editor resolveEditor(Project project) {
            if (isUsableEditor(editor)) {
                VirtualFile currentFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
                if (currentFile != null && currentFile.isValid() && !isCollectedOutputFile(currentFile)) {
                    return editor;
                }
            }

            if (project == null || file == null || !file.isValid()) {
                return null;
            }

            return FileEditorManager.getInstance(project).openTextEditor(
                    new OpenFileDescriptor(project, file, Math.max(0, caretOffset)),
                    true
            );
        }

        private static boolean isUsableEditor(Editor editor) {
            return editor != null && !editor.isDisposed();
        }
    }
}
