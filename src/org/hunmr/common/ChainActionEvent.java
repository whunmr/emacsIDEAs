package org.hunmr.common;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;

public class ChainActionEvent extends AnActionEvent {
    private Runnable _pendingAction;
    private final Editor _editor;

    public ChainActionEvent(AnActionEvent e, Runnable runnable, Editor _editor) {
        super(e.getInputEvent(), e.getDataContext(), e.getPlace(), e.getPresentation(), e.getActionManager(), e.getModifiers());
        this._pendingAction = runnable;
        this._editor = _editor;
    }

    public Runnable getPendingAction() {
        return _pendingAction;
    }

    public Editor getEditor() {
        return _editor;
    }
}
