package org.hunmr.util;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;

public class AppUtil {
    public static void runWriteAction(final Runnable runnable, final Editor editor) {
        try {
            if (editor.getProject() != null) {
                WriteCommandAction.runWriteCommandAction(editor.getProject(), runnable);
            } else {
                ApplicationManager.getApplication().runWriteAction(runnable);
            }
        } catch (Exception e) {
            HintManager.getInstance().showInformationHint(editor, e.getMessage());
        }
    }
}
