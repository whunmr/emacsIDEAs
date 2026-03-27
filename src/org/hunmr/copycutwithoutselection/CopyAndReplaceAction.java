package org.hunmr.copycutwithoutselection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.common.ChainActionEvent;
import org.hunmr.common.CommandContext;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.common.selector.Selection;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;

public class CopyAndReplaceAction extends EmacsIdeasAction {
    @Override
    public void actionPerformed(final AnActionEvent e) {
        if (super.initAction(e)) {
            final Editor editor = _editor;
            final ChainActionEvent pendingJumpAction = new ChainActionEvent(e, createPendingJumpAction(e, editor), editor);
            CopyCutWithoutSelectAction.getInstance().actionPerformed(pendingJumpAction);
            cleanupSetupsInAndBackToNormalEditingMode();
        }
    }

    private Runnable createPendingJumpAction(final AnActionEvent e, final Editor editor) {
        return new Runnable() {
            @Override
            public void run() {
                AceJumpAction.getInstance().actionPerformed(createPendingSelectAndPasteAction(e, editor));
            }
        };
    }

    private ChainActionEvent createPendingSelectAndPasteAction(AnActionEvent e, final Editor editor) {
        Runnable selectAndPaste = new Runnable() {
            @Override
            public void run() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        CommandContext cmdCtx = CopyCutWithoutSelectAction.getInstance().getCmdContext();
                        TextRange tr = Selection.getTextRangeBy(editor, cmdCtx);
                        if (tr != null) {
                            editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
                            ClipboardEditorUtil.pasteFromClipboard(editor);
                        }
                    }
                };

                AppUtil.runWriteAction(runnable, editor);
            }
        };

        return new ChainActionEvent(e, selectAndPaste, editor);
    }
}
