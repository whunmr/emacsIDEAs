package org.hunmr.location;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;

import javax.swing.SwingConstants;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
        if (project == null || text == null || text.isEmpty()) {
            return null;
        }

        VirtualFile outputFile = findOpenOutputFile(project);
        if (outputFile == null) {
            outputFile = createNewOutputFile(project);
            openInRightSplit(project, outputFile);
        }

        appendText(project, outputFile, text);
        FileDocumentManager.getInstance().saveDocument(getOrCreateDocument(outputFile));
        return outputFile;
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

    private static void appendText(Project project, VirtualFile outputFile, String text) throws IOException {
        Document document = getOrCreateDocument(outputFile);
        if (document == null) {
            Files.write(new File(outputFile.getPath()).toPath(), text.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);
            outputFile.refresh(false, false);
            return;
        }

        final Document targetDocument = document;
        WriteCommandAction.runWriteCommandAction(project, new Runnable() {
            @Override
            public void run() {
                targetDocument.insertString(targetDocument.getTextLength(), text);
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
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
            return;
        }

        manager.createSplitter(SwingConstants.VERTICAL, currentWindow);
        com.intellij.openapi.fileEditor.impl.EditorWindow nextWindow = manager.getNextWindow(currentWindow);
        if (nextWindow != null) {
            manager.openFileWithProviders(outputFile, true, nextWindow);
            manager.setCurrentWindow(nextWindow);
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
            return;
        }

        FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
    }

    private static Path getOutputDirectory(Project project) {
        String systemPath = PathManager.getSystemPath();
        if (systemPath != null && !systemPath.isEmpty()) {
            return new File(systemPath, OUTPUT_DIR_NAME).toPath();
        }

        return new File(System.getProperty("java.io.tmpdir"), OUTPUT_DIR_NAME).toPath();
    }
}
