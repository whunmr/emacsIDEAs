package org.hunmr.util;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;

public class AppUtil {
    public static Runnable getRunnableWrapper(final Runnable runnable, final Editor editor) {
        return new Runnable() {
            @Override
            public void run() {
                CommandProcessor.getInstance().executeCommand(editor.getProject(), runnable, "", ActionGroup.EMPTY_GROUP);
            }
        };
    }
}
