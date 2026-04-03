package org.hunmr.location;

import com.intellij.openapi.command.WriteCommandAction;
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
    private static final String OUTPUT_FILE_NAME = "emacsJump-collected-context.txt";

    private CollectedOutputFileManager() {
    }

    public static VirtualFile appendAndOpen(Project project, String text) throws IOException {
        if (project == null || text == null || text.isEmpty()) {
            return null;
        }

        Path outputPath = new File(System.getProperty("java.io.tmpdir"), OUTPUT_FILE_NAME).toPath();
        Path parent = outputPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        if (!Files.exists(outputPath)) {
            Files.createFile(outputPath);
        }
        Files.write(outputPath, text.getBytes(StandardCharsets.UTF_8), java.nio.file.StandardOpenOption.APPEND);

        VirtualFile outputFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outputPath.toFile());
        if (outputFile == null) {
            return null;
        }

        openInRightSplit(project, outputFile);
        return outputFile;
    }

    private static void openInRightSplit(Project project, VirtualFile outputFile) {
        FileEditorManagerEx manager = FileEditorManagerEx.getInstanceEx(project);
        if (manager == null) {
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project, outputFile), true);
            return;
        }

        if (manager.isFileOpen(outputFile)) {
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
}
