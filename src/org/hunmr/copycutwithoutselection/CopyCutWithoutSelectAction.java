package org.hunmr.copycutwithoutselection;

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
import org.hunmr.caret.MoveToNextIntentionCommand;
import org.hunmr.common.CommandContext;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.copycutwithoutselection.selector.Selector;
import org.hunmr.copycutwithoutselection.selector.SelectorFactory;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ThreadUtil;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class CopyCutWithoutSelectAction extends EmacsIdeasAction {
    private KeyListener _handleCopyKeyListener;
    private SelectionModel _selection;
    private CommandContext _cmdCtx;

    public void actionPerformed(AnActionEvent e) {
        if (super.initAction(e)) {
            _contentComponent.addKeyListener(_handleCopyKeyListener);
        }
    }

    @Override
    protected void initMemberVariableForConvenientAccess(AnActionEvent e) {
        super.initMemberVariableForConvenientAccess(e);
        _handleCopyKeyListener = createHandleCopyWithoutSelectionKeyListener();
        _selection = _editor.getSelectionModel();
        _cmdCtx = new CommandContext();
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
        if (_cmdCtx.consume(key)) {
            return false;
        }

        if (SelectorFactory.isSelectorKey(key)) {
            final TextRange tr = getTextRangeBy(key);
            if (tr != null) {
                _selection.setSelection(tr.getStartOffset(), tr.getEndOffset());
                doActionOnSelectedRange(tr);
            }

            return true;
        }

        if (Character.toLowerCase(key) == 'i') {
            new MoveToNextIntentionCommand(_editor, _psiFile, _cmdCtx).moveCaretToNextIntention();
            return true;
        }

        return true;
    }

    private void doActionOnSelectedRange(TextRange tr) {
        if (_cmdCtx.getPrefixCount() == 0) {
            copySelection(tr);
        } else if (_cmdCtx.getPrefixCount() == 1) {
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

    private TextRange getTextRangeBy(char key) {
        Selector selector = SelectorFactory.createSelectorBy(key, _editor);

        if (selector == null) {
            HintManager.getInstance().showInformationHint(_editor, SelectorFactory.HELP_MSG);
            return null;
        }

        TextRange tr = selector.getRange(_cmdCtx);
        if (tr == null) {
            HintManager.getInstance().showInformationHint(_editor, "404");
        }

        return tr;
    }

    private RangeHighlighter addHighlighterOnCopiedRange(TextRange tr) {
        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setBackgroundColor(SelectorFactory.HIGHLIGHT_COLOR);
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
