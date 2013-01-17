package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.keylistener.KeyEventDispatcher;
import org.hunmr.acejump.keylistener.KeyEventListener;
import org.hunmr.acejump.marker.Marker;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;
import org.hunmr.acejump.runnable.ShowMarkers;
import org.hunmr.util.EditorUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class AceJumpAction extends AnAction {
    private Stage _stage = Stage.INITIAL;
    private MarkerCollection _markerCollection;
    private AceJumpAction _action;
    private Editor _editor;
    private JComponent _contentComponent;
    private KeyListener[] _keyListeners;
    private Document _document;
    private MarkersPanel _markersPanel;
    private char _originalTargetKey;

    @Override
    public synchronized void actionPerformed(AnActionEvent e) {
        if ( _stage.equals(Stage.RUNNING) || ! initMemberVariableForConvenientAccess(e)) {
            return;
        }

        disableAllExistingKeyListeners();
        AddKeyEventListener(createShowMarkupKeyListener());
    }

    private boolean handleShowMarkersKey(char key) {
        if (Character.isLetterOrDigit(key)) {
            _originalTargetKey = key;

            ArrayList<Integer> offsets = null;
            offsets = getOffsetsOfCurrentKey(key);

            runReadAction(new ShowMarkers(offsets, _action));

            if (!_markerCollection.isEmpty()) {
                AddKeyEventListener(createJumpToMarupKeyListener());
            } else {
                backToNormalEditingMode();
            }

            return true;
        }
        return false;
    }

    private TextRange getMarkerRange(char key) {
        Marker marker = _markerCollection.get(key);
        if (marker == null) {
            return EditorUtils.getVisibleTextRange(_editor);
        }

        return marker.getTextRange();
    }

    private boolean handleJumpToMarkerKey(char key) {
        if (Character.isLetterOrDigit(key) || _markerCollection.containsKey(key)) {
            if (_markerCollection.keyMappingToMultipleMarkers(key)) {

                ArrayList<Integer> offsets = null;
                offsets = getOffsetsOfOriginalKey(key);

                _markerCollection.clear();
                runReadAction(new ShowMarkers(offsets, _action));
                return false;
            }

            final int offsetToJump = _markerCollection.get(key).getOffset();
            ApplicationManager.getApplication().runReadAction(new Runnable() {
                @Override
                public void run() {
                    _action.getEditor().getCaretModel().moveToOffset(offsetToJump);
                    _action.setStage(Stage.INITIAL);
                }
            });

            return true;
        }

        return false;
    }

    private ArrayList<Integer> getOffsetsOfCurrentKey(char key) {
        ArrayList<Integer> offsets;Marker marker = _markerCollection.get(key);
        if (marker == null) {
            offsets = EditorUtils.getOffsetsOfCharIgnoreCase(key, EditorUtils.getVisibleTextRange(_editor), _document);
        } else {
            offsets = marker.getOffsets();
        }
        return offsets;
    }

    private ArrayList<Integer> getOffsetsOfOriginalKey(char key) {
        ArrayList<Integer> offsets;

        Marker marker = _markerCollection.get(key);
        if (marker == null) {
            offsets = EditorUtils.getOffsetsOfCharIgnoreCase(_originalTargetKey, EditorUtils.getVisibleTextRange(_editor), _document);
        } else {
            offsets = marker.getOffsets();
        }

        return offsets;
    }

    private void runReadAction(ShowMarkers action) {
        ApplicationManager.getApplication().runReadAction(action);
    }

    private KeyEventListener<AceJumpAction> createJumpToMarupKeyListener() {
        return new KeyEventListener<AceJumpAction>() {
            @Override
            public void keyTyped(KeyEvent keyEvent, KeyEventDispatcher<AceJumpAction> tKeyEventDispatcher, AceJumpAction paramObject) {
                keyEvent.consume();
                boolean jumpFinished = handleJumpToMarkerKey(keyEvent.getKeyChar());
                if (jumpFinished) {
                    getContentComponent().removeKeyListener(tKeyEventDispatcher);
                    backToNormalEditingMode();
                }
            }
        };
    }

    private KeyEventListener<AceJumpAction> createShowMarkupKeyListener() {
        return new KeyEventListener<AceJumpAction>() {
            @Override
            public void keyTyped(KeyEvent keyEvent, KeyEventDispatcher<AceJumpAction> tKeyEventDispatcher, AceJumpAction paramObject) {
                keyEvent.consume();
                boolean showMarkupFinished = handleShowMarkersKey(keyEvent.getKeyChar());
                if (showMarkupFinished) {
                    getContentComponent().removeKeyListener(tKeyEventDispatcher);
                }
            }
        };
    }

    private void AddKeyEventListener(KeyEventListener<AceJumpAction> keyEventListener) {
        KeyEventDispatcher<AceJumpAction> keyListener = new KeyEventDispatcher<AceJumpAction>(this, keyEventListener);
        getContentComponent().addKeyListener(keyListener);
    }

    private void backToNormalEditingMode() {
        restoreOldKeyListeners();

        if (_markersPanel != null) {
            _contentComponent.remove(_markersPanel);
        }
        _contentComponent.repaint();
        _stage = Stage.INITIAL;
    }

    private void restoreOldKeyListeners() {
        for (KeyListener kl : _keyListeners) {
            _contentComponent.addKeyListener(kl);
        }
    }

    private void disableAllExistingKeyListeners() {
        _keyListeners = _contentComponent.getKeyListeners();
        for (KeyListener kl : _keyListeners) {
            _contentComponent.removeKeyListener(kl);
        }
    }

    private boolean initMemberVariableForConvenientAccess(AnActionEvent e) {
        _editor = e.getData(PlatformDataKeys.EDITOR);
        if (_editor == null) {
            return false;
        }

        _stage = Stage.RUNNING;
        _document = _editor.getDocument();
        _action = this;
        _contentComponent = _editor.getContentComponent();
        _markerCollection = new MarkerCollection();

        return true;
    }

    public void setStage(Stage stage) {
        this._stage = stage;
    }

    public Editor getEditor() {
        return _editor;
    }

    public JComponent getContentComponent() {
        return _contentComponent;
    }

    public void showNewMarkersPanel(MarkersPanel markersPanel) {
        if (_markersPanel != null) {
            _contentComponent.remove(_markersPanel);
            _contentComponent.repaint();
        }

        _markersPanel = markersPanel;
        _contentComponent.add(markersPanel);
        _contentComponent.repaint();
    }

    public MarkerCollection getMarkerCollection() {
        return _markerCollection;
    }
    public static enum Stage {
        INITIAL,
        RUNNING;
    }
}
