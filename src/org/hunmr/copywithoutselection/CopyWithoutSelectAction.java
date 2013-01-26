package org.hunmr.copywithoutselection;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.copywithoutselection.selector.*;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ThreadUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CopyWithoutSelectAction extends EmacsIdeasAction {
    public static final String HELP_MSG = "C-c w : Copy word\n" +
            "C-c s : Copy String\n" +
            "C-c l : Copy Line\n" +
            "C-c q : Copy Quoted\n" +
            "C-c a : Copy Quoted\n" +
            "C-c p : Copy Paragraph\n";
    public static final Color HIGHLIGHT_COLOR = new Color(122, 214, 162);
    private KeyListener _handleCopyKeyListener;
    private SelectionModel _selection;
    private int _spaceCmdCount;

    public void actionPerformed(AnActionEvent e) {
        if (super.initAction(e)) {
            _contentComponent.addKeyListener(_handleCopyKeyListener);
        }
    }

    @Override
    protected void initMemberVariableForConvenientAccess() {
        super.initMemberVariableForConvenientAccess();
        _handleCopyKeyListener = createHandleCopyWithoutSelectionKeyListener();
        _selection = _editor.getSelectionModel();
        _spaceCmdCount = 0;
    }

    private KeyListener createHandleCopyWithoutSelectionKeyListener() {
        return new KeyListener() {
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                boolean copyFinished  = handleKey(keyEvent.getKeyChar());
                if (copyFinished) {
                    cleanupSetupsInAndBackToNormalEditingMode();
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

    private boolean handleKey(char key) {
        if (key == ' ') {
            _spaceCmdCount++;
            return false;
        }

        final TextRange tr = createTextSelectorBy(key);
        if (tr != null) {
            _selection.setSelection(tr.getStartOffset(), tr.getEndOffset());
            doActionOnSelectedRange(tr);
        }

        return true;
    }

    private void doActionOnSelectedRange(TextRange tr) {
        if (_spaceCmdCount == 0) {
            copySelection(tr);
        } else if (_spaceCmdCount == 1) {
            cutSelection();
        }
    }

    private void cutSelection() {
        Runnable cutRunnable = new Runnable() {
            @Override
            public void run() {
                _editor.getSelectionModel().copySelectionToClipboard();
                EditorModificationUtil.deleteSelectedText(_editor);
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(cutRunnable, _editor));
    }

    private void copySelection(TextRange tr) {
        _selection.copySelectionToClipboard();
        _selection.removeSelection();

        final RangeHighlighter rh = addHighlighterOnCopiedRange(tr);
        ApplicationManager.getApplication().invokeLater(createClearHighlighterRunnable(rh));
    }

    private Runnable createClearHighlighterRunnable(final RangeHighlighter rh) {
        return new Runnable() {
            @Override
            public void run() {
                ThreadUtil.sleep(200);
                _editor.getMarkupModel().removeHighlighter(rh);
            }
        };
    }

    private TextRange createTextSelectorBy(char key) {
        TextRange tr = null;
        switch (key) {
            case 'w' :
            case 'W' :
                tr = new WordSelector(_editor).getRange();
                break;
            case 's' :
            case 'S' :
                tr = new StringSelector(_editor).getRange();
                break;
            case 'l' :
            case 'L' :
                tr = new LineSelector(_editor).getRange();
                break;
            case 'p' :
            case 'P' :
                tr = new ParagraphSelector(_editor).getRange();
                break;
            case 'b' :
            case 'B' :
                tr = new BlockSelector(_editor).getRange();
                break;
            case 'a' :
            case 'A' :
            case 'q' :
            case 'Q' :
                tr = new QuoteSelector(_editor).getRange();
                break;
            default:
                HintManager.getInstance().showInformationHint(_editor, HELP_MSG);
                return null;
        }

        if (tr == null) {
            HintManager.getInstance().showInformationHint(_editor, "404");
        }

        return tr;
    }

    private RangeHighlighter addHighlighterOnCopiedRange(TextRange tr) {
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setBackgroundColor(HIGHLIGHT_COLOR);
        return _editor.getMarkupModel().addRangeHighlighter(tr.getStartOffset(), tr.getEndOffset(),
                HighlighterLayer.LAST + 1, textAttributes, HighlighterTargetArea.EXACT_RANGE);
    }

    public void cleanupSetupsInAndBackToNormalEditingMode() {
        if (_handleCopyKeyListener != null) {
            _contentComponent.removeKeyListener(_handleCopyKeyListener);
            _handleCopyKeyListener = null;
        }

        super.cleanupSetupsInAndBackToNormalEditingMode();
    }

}
