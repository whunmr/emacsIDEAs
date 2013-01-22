package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.CommandAroundJumpFactory;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;
import org.hunmr.acejump.runnable.JumpRunnable;
import org.hunmr.acejump.runnable.ShowMarkersRunnable;
import org.hunmr.util.EditorUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Stack;

public class AceJumpAction extends AnAction {
    private volatile boolean _isStillRunning = false;
    private MarkerCollection _markers;
    private AceJumpAction _action;
    private Editor _editor;
    private JComponent _contentComponent;
    private KeyListener[] _keyListeners;
    private Document _document;
    private MarkersPanel _markersPanel;
    private KeyListener _showMarkersKeyListener;
    private KeyListener _jumpToMarkerKeyListener;
    private Stack<CommandAroundJump> _commandsAroundJump;

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (e.getData(PlatformDataKeys.EDITOR) == null) {
            return;
        }

        switchEditorIfNeed(e);

        if (_isStillRunning) {
            cleanupSetupsInAndBackToNormalEditingMode();
        }

        initMemberVariableForConvenientAccess();
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
        if (EditorUtils.isPrintableChar(key)) {
            runReadAction(new ShowMarkersRunnable(getOffsetsOfCurrentKey(key), _action));

            if (_markers.hasNoPlaceToJump()) {
                cleanupSetupsInAndBackToNormalEditingMode();
                return false;
            }

            //if (_markers.hasOnlyOnePlaceToJump()) {
            //    jumpToOffset(_markers.getFirstOffset());
            //    return false;
            //}

            _contentComponent.addKeyListener(_jumpToMarkerKeyListener);
            return true;
        }

        return false;
    }

    private boolean handleJumpToMarkerKey(char key) {
        if (CommandAroundJumpFactory.isCommandKey(key)) {
            _commandsAroundJump.push(CommandAroundJumpFactory.createCommand(key, _editor));
            return false;
        }

        if (EditorUtils.isPrintableChar(key) && _markers.containsKey(key)) {
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

    private KeyListener createShowMarkersKeyListener() {
        return new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                boolean showMarkersFinished = handleShowMarkersKey(keyEvent.getKeyChar());
                if (showMarkersFinished) {
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

    private ArrayList<Integer> getOffsetsOfCurrentKey(char key) {
        if (_markers.get(key) != null) {
            return _markers.get(key).getOffsets();
        }

        TextRange visibleTextRange = EditorUtils.getVisibleTextRange(_editor);

        ArrayList offsets = getOffsetsOfCharIgnoreCase(key, visibleTextRange, _document);
        if (key == KeyEvent.VK_SPACE) {
            offsets.addAll(getOffsetsOfCharIgnoreCase('\t', visibleTextRange, _document));
            offsets.addAll(getOffsetsOfCharIgnoreCase('\n', visibleTextRange, _document));
        } else if (key == ',') {
            offsets.addAll(getOffsetsOfCharIgnoreCase('.', visibleTextRange, _document));
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
        int caretOffset = _editor.getCaretModel().getOffset();

        ArrayList<Integer> offsets = new ArrayList<Integer>();

        int index = visibleText.indexOf(c);
        while (index >= 0) {
            int offset = startOffset + index;
            if (!isSpaceAndShouldIgnore(c, visibleText, index) && offset != caretOffset) {
                offsets.add(offset);
            }

            index = visibleText.indexOf(c, index + 1);
        }

        return offsets;
    }

    private boolean isSpaceAndShouldIgnore(char c, String visibleText, int index) {
        if (isSpace(c)) {
            boolean isAfterSpace = (index != 0) && (isSpace(visibleText.charAt(index - 1)));
            boolean noSpaceAround = (index != 0)
                                    && (index != visibleText.length() - 1)
                                    && (!isSpace(visibleText.charAt(index - 1)))
                                    && (!isSpace(visibleText.charAt(index + 1)));

            if (isAfterSpace || noSpaceAround) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpace(char c) {
        return c == ' ' || c == '\t';
    }

    private void runReadAction(ShowMarkersRunnable action) {
        ApplicationManager.getApplication().runReadAction(action);
    }

    private void jumpToOffset(final int jumpOffset) {
        for (CommandAroundJump cmd : _commandsAroundJump) {
            cmd.beforeJump(jumpOffset);
        }

        ApplicationManager.getApplication().runReadAction(new JumpRunnable(jumpOffset, this));

        for (CommandAroundJump cmd : _commandsAroundJump) {
            cmd.afterJump(jumpOffset);
        }
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

    private void initMemberVariableForConvenientAccess() {
        _isStillRunning = true;
        _document = _editor.getDocument();
        _action = this;
        _contentComponent = _editor.getContentComponent();
        _markers = new MarkerCollection();

        _showMarkersKeyListener = createShowMarkersKeyListener();
        _jumpToMarkerKeyListener = createJumpToMarupKeyListener();
        _commandsAroundJump = new Stack<CommandAroundJump>();
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
