package org.hunmr.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbAwareAction;

public abstract class SimpleEditorAction extends DumbAwareAction {

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(e.getData(CommonDataKeys.EDITOR) != null);
    }

    protected final Editor getEditor(AnActionEvent e) {
        return e.getData(CommonDataKeys.EDITOR);
    }
}
