package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.SelectAfterJumpCommand;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;
import org.hunmr.acejump.offsets.CharOffsetsFinder;
import org.hunmr.acejump.offsets.OffsetsFinder;
import org.hunmr.acejump.offsets.WordOffsetsFinder;
import org.hunmr.acejump.runnable.JumpRunnable;
import org.hunmr.acejump.runnable.ShowMarkersRunnable;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.util.EditorUtils;
import org.hunmr.util.Str;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class AceJumpAction extends EmacsIdeasAction {
    private MarkerCollection _markers;
    private ArrayList<MarkersPanel> _markersPanels;
    private KeyListener _showMarkersKeyListener;
    private KeyListener _jumpToMarkerKeyListener;
    private Stack<CommandAroundJump> _commandsAroundJump = new Stack<CommandAroundJump>();
    private static volatile AceJumpAction _instance;
    private boolean _isCalledFromOtherAction; //TODO
    private OffsetsFinder _offsetsFinder = new WordOffsetsFinder();

    public AceJumpAction() {
        _instance = this;
    }

    public void performAction(AnActionEvent e) {
        _offsetsFinder = new CharOffsetsFinder();
        _isCalledFromOtherAction = true;
        beginJump(e);
    }

    public void startJump(AnActionEvent e, List<JOffset> offsets) {
        _isCalledFromOtherAction = true;
        if (offsets == null || offsets.isEmpty()) {
            return;
        }

        if (super.initAction(e)) {
            showMarkers(offsets);
        }
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        if (isCalledFromOtherAction()) {
            _offsetsFinder = new CharOffsetsFinder();
        }

        beginJump(e);
    }

    private void beginJump(AnActionEvent e) {
        if (super.initAction(e)) {
            attachKeyListener(_showMarkersKeyListener);
        }
    }

    private boolean handleShowMarkersKey(char key) {
        if (_markers == null || _action == null) {
            return false;
        }

        if (EditorUtils.isPrintableChar(key)) {
            showMarkers(getOffsetsOfCharInVisibleArea(key));
            return true;
        }

        return false;
    }

    private boolean handleJumpToMarkerKey(char key) {
        if (_markers == null) {
            return false;
        }

        if (!_markers.containsMarkerWithKey(key)) {
            key = Str.getCounterCase(key);
        }

        if (EditorUtils.isPrintableChar(key) && _markers.containsMarkerWithKey(key)) {
            ArrayList<JOffset> offsetsOfKey = _markers.getOffsetsOfKey(key);

            if (offsetsOfKey.size() > 1) {
                _markers.clear();
                runReadAction(new ShowMarkersRunnable(offsetsOfKey, (AceJumpAction) _action));
                return false;
            }

            jumpToOffset(offsetsOfKey.get(0));
            return true;
        }

        return false;
    }

    private KeyListener createShowMarkersKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                boolean showMarkersFinished = handleShowMarkersKey(keyEvent.getKeyChar());
                if (showMarkersFinished) {
                    detachKeyListener(keyEvent, this);
                }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (KeyEvent.VK_ESCAPE == keyEvent.getKeyChar()) {
                    cleanupSetupsInAndBackToNormalEditingMode();
                }
            }
        };
    }

    private KeyListener createJumpToMarkupKeyListener() {
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();

                if (KeyEvent.VK_SPACE == keyEvent.getKeyChar() || KeyEvent.VK_SEMICOLON == keyEvent.getKeyChar()) {
                    cleanupSetupsInAndBackToNormalEditingMode();
                    return;
                }

                if (keyEvent.isShiftDown() && isUsableEditor(_editor)) {
                    addCommandAroundJump(new SelectAfterJumpCommand(_editor));
                }

                boolean jumpFinished = handleJumpToMarkerKey(keyEvent.getKeyChar());
                if (jumpFinished) {
                    detachKeyListener(keyEvent, this);
                    handlePendingActionOnSuccess();
                }
            }

            @Override
            public void keyPressed(KeyEvent keyEvent) {
                if (KeyEvent.VK_ESCAPE == keyEvent.getKeyChar()) {
                    cleanupSetupsInAndBackToNormalEditingMode();
                }
            }
        };
    }

    private void attachKeyListener(KeyListener keyListener) {
        JComponent contentComponent = _contentComponent;
        if (contentComponent != null && keyListener != null) {
            contentComponent.addKeyListener(keyListener);
        }
    }

    private void detachKeyListener(KeyListener keyListener) {
        JComponent contentComponent = _contentComponent;
        if (contentComponent != null && keyListener != null) {
            contentComponent.removeKeyListener(keyListener);
        }
    }

    private void detachKeyListener(KeyEvent keyEvent, KeyListener keyListener) {
        Component component = keyEvent.getComponent();
        if (component instanceof JComponent && keyListener != null) {
            ((JComponent) component).removeKeyListener(keyListener);
            return;
        }

        detachKeyListener(keyListener);
    }

    private List<JOffset> getOffsetsOfCharInVisibleArea(char key) {
        if (_markers.get(key) != null) {
            return _markers.get(key).getOffsets();
        }

        return findOffsetsInEditors(key);
    }

    private List<JOffset> findOffsetsInEditors(char key) {
        List<JOffset> joffsets = new ArrayList<JOffset>();
        if (_editors == null || _offsetsFinder == null || !isUsableEditor(_editor)) {
            return joffsets;
        }

        for (Editor editor : _editors) {
            if (!isUsableEditor(editor)) {
                continue;
            }

            List<Integer> offsets = _offsetsFinder.getOffsets(key, editor, _editor);
            for (Integer offset : offsets) {
                joffsets.add(new JOffset(editor, offset));
            }
        }

        return joffsets;
    }

    private void jumpToOffset(final JOffset jumpOffset) {
        if (jumpOffset == null || !isUsableEditor(jumpOffset.editor)) {
            cleanupSetupsInAndBackToNormalEditingMode();
            return;
        }

        for (CommandAroundJump cmd : _commandsAroundJump) {
            if (cmd != null) {
                cmd.beforeJump(jumpOffset);
            }
        }

        ApplicationManager.getApplication().runReadAction(new JumpRunnable(jumpOffset, this));

        for (CommandAroundJump cmd : _commandsAroundJump) {
            if (cmd != null) {
                cmd.preAfterJump(jumpOffset);
                cmd.afterJump();
            }
        }

        cleanupSetupsInAndBackToNormalEditingMode();
    }

    public void cleanupSetupsInAndBackToNormalEditingMode() {
        if (_showMarkersKeyListener != null) {
            detachKeyListener(_showMarkersKeyListener);
            _showMarkersKeyListener = null;
        }

        if (_jumpToMarkerKeyListener != null) {
            detachKeyListener(_jumpToMarkerKeyListener);
            _jumpToMarkerKeyListener = null;
        }

        if (_markersPanels != null) {
            for (MarkersPanel markersPanel : _markersPanels) {
                if (markersPanel == null) {
                    continue;
                }

                Container parent = markersPanel.getParent();
                if (parent != null) {
                    parent.remove(markersPanel);
                    parent.repaint();
                }
            }
        }

        if (_editors != null) {
            for (Editor editor : _editors) {
                if (isUsableEditor(editor)) {
                    editor.getComponent().repaint();
                }
            }
        }

        _markers = null;
        _markersPanels = null;
        _commandsAroundJump = new Stack<CommandAroundJump>();
        _offsetsFinder = new WordOffsetsFinder();
        _isCalledFromOtherAction = false;
        super.cleanupSetupsInAndBackToNormalEditingMode();
    }

    protected void initMemberVariableForConvenientAccess(AnActionEvent e) {
        super.initMemberVariableForConvenientAccess(e);

        _markers = new MarkerCollection();
        _showMarkersKeyListener = createShowMarkersKeyListener();
        _jumpToMarkerKeyListener = createJumpToMarkupKeyListener();
    }

    public void showNewMarkersPanel(ArrayList<MarkersPanel> markersPanels) {
        if (_markersPanels != null) {
            for (MarkersPanel markersPanel : _markersPanels) {
                if (markersPanel == null) {
                    continue;
                }

                Container parent = markersPanel.getParent();
                if (parent != null) {
                    parent.remove(markersPanel);
                    parent.repaint();
                }
            }
        }

        _markersPanels = markersPanels;

        if (markersPanels == null) {
            return;
        }

        for (MarkersPanel markersPanel : markersPanels) {
            if (markersPanel != null && isUsableEditor(markersPanel._editor)) {
                markersPanel._editor.getContentComponent().add(markersPanel);
                markersPanel._editor.getContentComponent().repaint();
            }
        }
    }

    public MarkerCollection getMarkerCollection() {
        return _markers;
    }

    public static AceJumpAction getInstance() {
        if (_instance == null) {
            _instance = new AceJumpAction();
        }
        return _instance;
    }

    public void addCommandAroundJump(CommandAroundJump commandAroundJump) {
        if (commandAroundJump != null) {
            _commandsAroundJump.push(commandAroundJump);
        }
    }

    public boolean isCalledFromOtherAction() {
        return _isCalledFromOtherAction;
    }

    private void showMarkers(List<JOffset> offsets) {
        if (_markers == null || _action == null || offsets == null || offsets.isEmpty()) {
            cleanupSetupsInAndBackToNormalEditingMode();
            return;
        }

        runReadAction(new ShowMarkersRunnable(offsets, (AceJumpAction) _action));

        if (_markers.hasNoPlaceToJump()) {
            cleanupSetupsInAndBackToNormalEditingMode();
            return;
        }

        if (_isCalledFromOtherAction && _markers.hasOnlyOnePlaceToJump()) {
            JOffset firstOffset = _markers.getFirstOffset();
            if (firstOffset == null) {
                cleanupSetupsInAndBackToNormalEditingMode();
                return;
            }

            jumpToOffset(firstOffset);
            return;
        }

        attachKeyListener(_jumpToMarkerKeyListener);
    }
}
