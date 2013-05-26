package org.hunmr.common;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import org.hunmr.acejump.runnable.ShowMarkersRunnable;

import javax.swing.*;
import java.awt.event.KeyListener;

public abstract class EmacsIdeasAction extends AnAction {
    protected volatile boolean _isStillRunning = false;
    protected EmacsIdeasAction _action;
    protected Editor _editor;
    protected JComponent _contentComponent;
    protected Document _document;
    protected KeyListener[] _keyListeners;
    protected AnActionEvent _event;
    protected Project _project;


    public void cleanupSetupsInAndBackToNormalEditingMode() {
        restoreOldKeyListeners();
        _contentComponent.repaint();
        _isStillRunning = false;
    }

    protected void restoreOldKeyListeners() {
        for (KeyListener kl : _keyListeners) {
            _contentComponent.addKeyListener(kl);
        }
    }

    public boolean initAction(AnActionEvent e) {
        if (getEditorFrom(e) == null) {
            return false;
        }

        switchEditorIfNeed(e);

        if (_isStillRunning) {
            cleanupSetupsInAndBackToNormalEditingMode();
        }

        initMemberVariableForConvenientAccess(e);
        disableAllExistingKeyListeners();

        return true;
    }

    private Editor getEditorFrom(AnActionEvent e) {
        if (e instanceof ChainActionEvent) {
            ChainActionEvent chainActionEvent = (ChainActionEvent) e;
            Editor editor = chainActionEvent.getEditor();
            if (editor != null) {
                return editor;
            }
        }

        return e.getData(PlatformDataKeys.EDITOR);
    }

    protected void switchEditorIfNeed(AnActionEvent e) {
        Editor newEditor = getEditorFrom(e);
        if (_editor != null && _editor != newEditor) {
            cleanupSetupsInAndBackToNormalEditingMode();
        }

        _editor = newEditor;
    }

    protected void initMemberVariableForConvenientAccess(AnActionEvent e) {
        _event = e;
        _isStillRunning = true;
        _document = _editor.getDocument();
        _action = this;
        _contentComponent = _editor.getContentComponent();
        _project = getProjectFrom(e);
    }

    protected void disableAllExistingKeyListeners() {
        _keyListeners = _contentComponent.getKeyListeners();
        for (KeyListener kl : _keyListeners) {
            _contentComponent.removeKeyListener(kl);
        }
    }

    public Editor getEditor() {
        return _editor;
    }

    protected void runReadAction(ShowMarkersRunnable action) {
        ApplicationManager.getApplication().runReadAction(action);
    }

    protected void handlePendingActionOnSuccess() {
        if (_event instanceof ChainActionEvent) {
            ChainActionEvent chainActionEvent = (ChainActionEvent) _event;
            chainActionEvent.getPendingAction().run();
        }
    }

    protected Project getProjectFrom(AnActionEvent e) {
        if (e instanceof ChainActionEvent) {
            ChainActionEvent chainActionEvent = (ChainActionEvent) e;
            Project project = chainActionEvent.getProject();
            if (project != null) {
                return project;
            }
        }

        return e.getData(PlatformDataKeys.PROJECT);
    }
}
