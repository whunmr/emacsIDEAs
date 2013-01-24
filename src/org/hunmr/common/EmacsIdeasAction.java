package org.hunmr.common;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
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
        if (e.getData(PlatformDataKeys.EDITOR) == null) {
            return false;
        }

        switchEditorIfNeed(e);

        if (_isStillRunning) {
            cleanupSetupsInAndBackToNormalEditingMode();
        }

        initMemberVariableForConvenientAccess();
        disableAllExistingKeyListeners();

        return true;
    }

    protected void switchEditorIfNeed(AnActionEvent e) {
        Editor newEditor = e.getData(PlatformDataKeys.EDITOR);
        if (_editor != null && _editor != newEditor) {
            cleanupSetupsInAndBackToNormalEditingMode();
        }

        _editor = newEditor;
    }

    protected void initMemberVariableForConvenientAccess() {
        _isStillRunning = true;
        _document = _editor.getDocument();
        _action = this;
        _contentComponent = _editor.getContentComponent();
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
}
