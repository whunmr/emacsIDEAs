package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.wm.ToolWindowManager;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.CommandAroundJumpFactory;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;
import org.hunmr.acejump.runnable.JumpRunnable;
import org.hunmr.acejump.runnable.ShowMarkersRunnable;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.util.EditorUtils;
import org.hunmr.util.Str;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Stack;

public class AceJumpAction extends EmacsIdeasAction {
    private MarkerCollection _markers;
    private MarkersPanel _markersPanel;
    private KeyListener _showMarkersKeyListener;
    private KeyListener _jumpToMarkerKeyListener;
    private Stack<CommandAroundJump> _commandsAroundJump;

    @Override
    public void actionPerformed(AnActionEvent e) {
        Project p = e.getData(PlatformDataKeys.PROJECT);
        if (!ToolWindowManager.getInstance(p).isEditorComponentActive()) {
            ToolWindowManager.getInstance(p).activateEditorComponent();
            return;
        }

        if (super.initAction(e)) {
            _contentComponent.addKeyListener(_showMarkersKeyListener);
        }
    }

    private boolean handleShowMarkersKey(char key) {
        if (EditorUtils.isPrintableChar(key)) {
            runReadAction(new ShowMarkersRunnable(getOffsetsOfCurrentKey(key), (AceJumpAction) _action));

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
            _commandsAroundJump.push(CommandAroundJumpFactory.createCommand(key, getEditor()));
            return false;
        }

        if (!_markers.containsKey(key)){
            key = Str.getCounterCase(key);
        }

        if (EditorUtils.isPrintableChar(key) && _markers.containsKey(key)) {
            if (_markers.keyMappingToMultipleMarkers(key)) {
                ArrayList<Integer> offsets = _markers.get(key).getOffsets();
                _markers.clear();
                runReadAction(new ShowMarkersRunnable(offsets, (AceJumpAction) _action));
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

        TextRange visibleTextRange = EditorUtils.getVisibleTextRange(getEditor());

        ArrayList offsets = getOffsetsOfCharIgnoreCase(String.valueOf(key), visibleTextRange, _document);

        if (key == KeyEvent.VK_SPACE) {
            offsets.addAll(getOffsetsOfCharIgnoreCase("\t\n", visibleTextRange, _document));
        } else if (key == ',') {
            offsets.addAll(getOffsetsOfCharIgnoreCase(";.{}()_=", visibleTextRange, _document));
        }

        return offsets;
    }

    public ArrayList<Integer> getOffsetsOfCharIgnoreCase(String charSet, TextRange markerRange, Document document) {
        ArrayList<Integer> offsets = new ArrayList<Integer>();
        String visibleText = document.getText(markerRange);

        for (char charToFind : charSet.toCharArray()) {
            char lowCase = Character.toLowerCase(charToFind);
            char upperCase = Character.toUpperCase(charToFind);
            offsets.addAll(getOffsetsOfChar(markerRange.getStartOffset(), lowCase, visibleText));
            if (upperCase != lowCase) {
                offsets.addAll(getOffsetsOfChar(markerRange.getStartOffset(), upperCase, visibleText));
            }
        }

        return offsets;
    }

    private ArrayList<Integer> getOffsetsOfChar(int startOffset, char c, String visibleText) {
        int caretOffset = getEditor().getCaretModel().getOffset();

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
            if (isAfterSpace) {
                return true;
            }
        }
        return false;
    }

    private boolean isSpace(char c) {
        return c == ' ' || c == '\t';
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

        super.cleanupSetupsInAndBackToNormalEditingMode();
    }

    protected void initMemberVariableForConvenientAccess(AnActionEvent e) {
        super.initMemberVariableForConvenientAccess(e);

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

    public MarkerCollection getMarkerCollection() {
        return _markers;
    }
}
