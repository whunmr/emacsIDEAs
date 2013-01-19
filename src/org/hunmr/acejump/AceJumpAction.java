package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;
import org.hunmr.acejump.runnable.JumpRunnable;
import org.hunmr.acejump.runnable.ShowMarkersRunnable;
import org.hunmr.util.EditorUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

public class AceJumpAction extends AnAction {
    private volatile boolean _isStillRunning = false;
    private MarkerCollection _markers;
    private AceJumpAction _action;
    private Editor _editor;
    private JComponent _contentComponent;
    private KeyListener[] _keyListeners;
    private Document _document;
    private MarkersPanel _markersPanel;
    private char _targetChar;
    private KeyListener _showMarkersKeyListener;
    private KeyListener _jumpToMarkerKeyListener;

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getData(PlatformDataKeys.EDITOR) == null) {
            return;
        }

        switchEditorIfNeed(e);

        if (_isStillRunning) {
            return;
        }

        initMemberVariableForConvenientAccess(e);
        disableAllExistingKeyListeners();
        _contentComponent.addKeyListener(_showMarkersKeyListener);
    }

    private void switchEditorIfNeed(AnActionEvent e) {
        Editor newEditor = e.getData(PlatformDataKeys.EDITOR);
        if (_editor != null && _editor != newEditor) {
            cleanupSetupsInAndBackToNormalEditingMode();
        }
        _editor = newEditor;
    }

    private boolean handleShowMarkersKey(char key) {
        if (isJumpTargetChar(key)) {
            runReadAction(new ShowMarkersRunnable(getOffsetsOfCurrentKey(key), _action));

            if (_markers.hasNoPlaceToJump()) {
                cleanupSetupsInAndBackToNormalEditingMode();
                return false;
            }

            if (_markers.hasOnlyOnePlaceToJump()) {
                jumpToOffset(_markers.getFirstOffset());
                return false;
            }

            _contentComponent.addKeyListener(_jumpToMarkerKeyListener);
            return true;
        }

        return false;
    }

    private boolean isJumpTargetChar(char key) {
        return EditorUtils.isPrintableChar(key) || key == '\n';
    }

    private boolean handleJumpToMarkerKey(char key) {
        if (isJumpTargetChar(key) && _markers.containsKey(key)) {
            if (_markers.keyMappingToMultipleMarkers(key)) {
                ArrayList<Integer> offsets = _markers.get(key).getOffsets();
                _markers.clear();
                runReadAction(new ShowMarkersRunnable(offsets, _action));
                return false;
            }

            jumpToOffset(_markers.get(key).getOffset());
            return true;
        }

        return false;
    }

    private KeyListener createJumpToMarupKeyListener() {
        return new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                boolean jumpFinished = handleJumpToMarkerKey(keyEvent.getKeyChar());
                if (jumpFinished) {
                    _contentComponent.removeKeyListener(_jumpToMarkerKeyListener);
                }
            }

            public void keyPressed(KeyEvent keyEvent) {
                if (KeyEvent.VK_ESCAPE == keyEvent.getKeyChar()) {
                    cleanupSetupsInAndBackToNormalEditingMode();
                }
            }
            public void keyReleased(KeyEvent keyEvent){}
        };
    }

    private KeyListener createShowMarkupKeyListener() {
        return new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                _targetChar = keyEvent.getKeyChar();
                boolean showMarkupFinished = handleShowMarkersKey(keyEvent.getKeyChar());
                if (showMarkupFinished) {
                    _contentComponent.removeKeyListener(_showMarkersKeyListener);
                }
            }

            public void keyPressed(KeyEvent keyEvent) {
                if (KeyEvent.VK_ESCAPE == keyEvent.getKeyChar()) {
                    cleanupSetupsInAndBackToNormalEditingMode();
                }
            }

            public void keyReleased(KeyEvent keyEvent) {
            }
        };
    }

    private ArrayList<Integer> getOffsetsOfCurrentKey(char key) {
        if (_markers.get(key) != null) {
            return _markers.get(key).getOffsets();
        }

        TextRange visibleTextRange = EditorUtils.getVisibleTextRange(_editor);

        ArrayList offsets = getOffsetsOfCharIgnoreCase(key, visibleTextRange, _document);
        if (key == KeyEvent.VK_SPACE) {
            offsets.addAll(getOffsetsOfCharIgnoreCase('\t', visibleTextRange, _document));
        } else if (key == '/') {
            offsets.addAll(getOffsetsOfCharIgnoreCase('\n', visibleTextRange, _document));
        }

        return offsets;
    }

    public ArrayList<Integer> getOffsetsOfCharIgnoreCase(char charToFind, TextRange markerRange, Document document) {
        ArrayList<Integer> offsets = new ArrayList<Integer>();
        String visibleText = document.getText(markerRange);
        char lowCase = Character.toLowerCase(charToFind);
        char upperCase = Character.toUpperCase(charToFind);

        offsets.addAll(getOffsetsOfChar(markerRange.getStartOffset(), lowCase, visibleText));
        if (upperCase != lowCase) {
            offsets.addAll(getOffsetsOfChar(markerRange.getStartOffset(), upperCase, visibleText));
        }

        return offsets;
    }

    private ArrayList<Integer> getOffsetsOfChar(int startOffset, char c, String visibleText) {
        ArrayList<Integer> offsets = new ArrayList<Integer>();

        int index = visibleText.indexOf(c);
        while (index >= 0) {
            if (!isSpaceInMiddleOfSpaces(c, visibleText, index)) {
                offsets.add(startOffset + index);
            }

            index = visibleText.indexOf(c, index + 1);
        }

        return offsets;
    }

    private boolean isSpaceInMiddleOfSpaces(char c, String visibleText, int index) {
        boolean charIsWhiteSpace = c == ' ' || c == '\t';
        if (charIsWhiteSpace) {
            boolean inMiddleOfWhiteSpaces = (index != 0)
                                            && (index != visibleText.length() - 1)
                                            && (visibleText.charAt(index - 1) == c)
                                            && (visibleText.charAt(index + 1) == c);
            if (inMiddleOfWhiteSpaces) {
                return true;
            }
        }
        return false;
    }

    private void runReadAction(ShowMarkersRunnable action) {
        ApplicationManager.getApplication().runReadAction(action);
    }

    private void jumpToOffset(int offset) {
        ApplicationManager.getApplication().runReadAction(new JumpRunnable(offset, this));
    }

    public void cleanupSetupsInAndBackToNormalEditingMode() {
        restoreOldKeyListeners();

        if (_showMarkersKeyListener != null) {
            _contentComponent.removeKeyListener(_showMarkersKeyListener);
            _showMarkersKeyListener = null;
        }

        if (_jumpToMarkerKeyListener != null) {
            _contentComponent.removeKeyListener(_jumpToMarkerKeyListener);
            _showMarkersKeyListener = null;
        }

        if (_markersPanel != null) {
            _contentComponent.remove(_markersPanel);
        }
        _contentComponent.repaint();
        _isStillRunning = false;
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

    private void initMemberVariableForConvenientAccess(AnActionEvent e) {
        _isStillRunning = true;
        _document = _editor.getDocument();
        _action = this;
        _contentComponent = _editor.getContentComponent();
        _markers = new MarkerCollection();

        _showMarkersKeyListener = createShowMarkupKeyListener();
        _jumpToMarkerKeyListener = createJumpToMarupKeyListener();
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

    public void setIsStillRunning(boolean isRunning) {
        this._isStillRunning = isRunning;
    }

    public Editor getEditor() {
        return _editor;
    }

    public MarkerCollection getMarkerCollection() {
        return _markers;
    }
}
