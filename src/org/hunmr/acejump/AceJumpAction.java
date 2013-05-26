package org.hunmr.acejump;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowManager;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.CommandAroundJumpFactory;
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

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

public class AceJumpAction extends EmacsIdeasAction {
    private MarkerCollection _markers;
    private MarkersPanel _markersPanel;
    private KeyListener _showMarkersKeyListener;
    private KeyListener _jumpToMarkerKeyListener;
    private Stack<CommandAroundJump> _commandsAroundJump;
    private HashSet<Character> _commandsAroundJumpCodes = new HashSet<Character>();
    private static volatile AceJumpAction _instance;
    private boolean _isCalledFromOtherAction;
    private OffsetsFinder _offsetsFinder = new WordOffsetsFinder();

    public AceJumpAction() {
        _instance = this;
    }

    public void performAction(AnActionEvent e) {
        _offsetsFinder = new CharOffsetsFinder();
        _isCalledFromOtherAction = true;
        this.actionPerformed(e);
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        _isCalledFromOtherAction = false;

        Project p = getProjectFrom(e);

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
            runReadAction(new ShowMarkersRunnable(getOffsetsOfCharInVisibleArea(key), (AceJumpAction) _action));

            if (_markers.hasNoPlaceToJump()) {
                cleanupSetupsInAndBackToNormalEditingMode();
                return false;
            }

            if (_isCalledFromOtherAction && _markers.hasOnlyOnePlaceToJump()) {
                jumpToOffset(_markers.getFirstOffset());
                return false;
            }

            _contentComponent.addKeyListener(_jumpToMarkerKeyListener);
            return true;
        }

        return false;
    }

    private boolean handleJumpToMarkerKey(char key) {
        if (!_isCalledFromOtherAction && CommandAroundJumpFactory.isCommandKey(key)) {
            addCommandsAroundJump(CommandAroundJumpFactory.createCommand(key, _editor));
            return false;
        }

        if (!_markers.containsKey(key)) {
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

    private void addCommandsAroundJump(CommandAroundJump command) {
        _commandsAroundJump.push(command);
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

    private KeyListener createJumpToMarkupKeyListener() {
        return new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                boolean jumpFinished = handleJumpToMarkerKey(keyEvent.getKeyChar());
                if (jumpFinished) {
                    _contentComponent.removeKeyListener(_jumpToMarkerKeyListener);
                    handlePendingActionOnSuccess();
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

    private List<Integer> getOffsetsOfCharInVisibleArea(char key) {
        if (_markers.get(key) != null) {
            return _markers.get(key).getOffsets();
        }

        return _offsetsFinder.getOffsets(key, _editor, _document);
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

        _commandsAroundJumpCodes = new HashSet<Character>();
        _offsetsFinder = new WordOffsetsFinder();
        super.cleanupSetupsInAndBackToNormalEditingMode();
    }

    protected void initMemberVariableForConvenientAccess(AnActionEvent e) {
        super.initMemberVariableForConvenientAccess(e);

        _markers = new MarkerCollection();
        _showMarkersKeyListener = createShowMarkersKeyListener();
        _jumpToMarkerKeyListener = createJumpToMarkupKeyListener();
        _commandsAroundJump = new Stack<CommandAroundJump>();

        for (Character commandsCode : _commandsAroundJumpCodes) {
            addCommandsAroundJump(CommandAroundJumpFactory.createCommand(commandsCode, _editor));
        }
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

    public static AceJumpAction getInstance() {
        return _instance;
    }

    public void addCommandsAroundJumpKey(char commandsAroundJumpCode) {
        _commandsAroundJumpCodes.add(commandsAroundJumpCode);
    }

    public boolean isCalledFromOtherAction() {
        return _isCalledFromOtherAction;
    }
}
