package org.hunmr.copycutwithoutselection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.util.TextRange;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.hunmr.common.CommandContext;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.common.selector.Selection;
import org.hunmr.common.selector.SelectorFactory;
import org.hunmr.util.AppUtil;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.util.concurrent.TimeUnit;

public class CopyCutWithoutSelectAction extends EmacsIdeasAction {
    private static CopyCutWithoutSelectAction _instance;
    private KeyListener _handleCopyKeyListener;
    protected SelectionModel _selection;
    protected CommandContext _cmdCtx;

    public CopyCutWithoutSelectAction() {
        this._instance = this;
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
        performCopyCutWithoutSelection(e);
    }

    public void performCopyCutWithoutSelection(AnActionEvent e) {
        if (super.initAction(e)) {
            attachKeyListener(_handleCopyKeyListener);
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
        return new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent keyEvent) {
                keyEvent.consume();
                boolean copyFinished  = handleKey(keyEvent.getKeyChar());
                if (copyFinished) {
                    Runnable pendingAction = getPendingAction();
                    cleanupSetupsInAndBackToNormalEditingMode();
                    if (pendingAction != null) {
                        pendingAction.run();
                    }
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

    protected boolean handleKey(char key) {
        if (_cmdCtx == null || _editor == null || _selection == null) {
            return false;
        }

        if (_cmdCtx.consume(key)) {
            return false;
        }

        if (SelectorFactory.isSelectorKey(key)) {
            final TextRange tr = Selection.getTextRangeBy(_editor, _cmdCtx);
            if (tr != null) {
                _selection.setSelection(tr.getStartOffset(), tr.getEndOffset());
                doActionOnSelectedRange(tr);
            }

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
        final Editor editor = _editor;
        if (editor == null || editor.isDisposed()) {
            return;
        }

        Runnable cutRunnable = new Runnable() {
            @Override
            public void run() {
                if (editor.isDisposed()) {
                    return;
                }

                editor.getSelectionModel().copySelectionToClipboard();
                EditorModificationUtil.deleteSelectedText(editor);
            }
        };

        AppUtil.runWriteAction(cutRunnable, editor);
    }

    private void copySelection(TextRange tr) {
        final Editor editor = _editor;
        final SelectionModel selection = _selection;
        if (editor == null || editor.isDisposed() || selection == null) {
            return;
        }

        selection.copySelectionToClipboard();
        selection.removeSelection();

        final RangeHighlighter rh = addHighlighterOnCopiedRange(editor, tr);
        scheduleHighlighterCleanup(editor, rh);
    }

    private void scheduleHighlighterCleanup(final Editor editor, final RangeHighlighter rh) {
        if (rh == null) {
            return;
        }

        AppExecutorUtil.getAppScheduledExecutorService().schedule(new Runnable() {
            @Override
            public void run() {
                ApplicationManager.getApplication().invokeLater(createClearHighlighterRunnable(editor, rh));
            }
        }, 200, TimeUnit.MILLISECONDS);
    }

    private Runnable createClearHighlighterRunnable(final Editor editor, final RangeHighlighter rh) {
        return new Runnable() {
            @Override
            public void run() {
                if (editor == null || editor.isDisposed()) {
                    return;
                }

                editor.getMarkupModel().removeHighlighter(rh);
            }
        };
    }

    private RangeHighlighter addHighlighterOnCopiedRange(Editor editor, TextRange tr) {
        if (editor == null || editor.isDisposed()) {
            return null;
        }

        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setBackgroundColor(SelectorFactory.HIGHLIGHT_COLOR);
        return editor.getMarkupModel().addRangeHighlighter(tr.getStartOffset(), tr.getEndOffset(),
                HighlighterLayer.LAST + 1, textAttributes, HighlighterTargetArea.EXACT_RANGE);
    }

    public void cleanupSetupsInAndBackToNormalEditingMode() {
        if (_handleCopyKeyListener != null) {
            detachKeyListener(_handleCopyKeyListener);
            _handleCopyKeyListener = null;
        }

        super.cleanupSetupsInAndBackToNormalEditingMode();
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

    public static CopyCutWithoutSelectAction getInstance() {
        if (_instance == null) {
            _instance = new CopyCutWithoutSelectAction();
        }
        return _instance;
    }

    public CommandContext getCmdContext() {
        return _instance != null ? _instance._cmdCtx : null;
    }
}
