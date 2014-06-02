package org.hunmr.copycutwithoutselection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.common.ChainActionEvent;
import org.hunmr.common.CommandContext;
import org.hunmr.common.EmacsIdeasAction;
import org.hunmr.common.selector.Selection;
import org.hunmr.util.AppUtil;

public class CopyAndReplaceAction extends EmacsIdeasAction {
    @Override
    public void actionPerformed(final AnActionEvent e) {
        if (super.initAction(e)) {
            final ChainActionEvent pendingJumpAction = new ChainActionEvent(e, createPendingJumpAction(e), _editor, _project);
            CopyCutWithoutSelectAction.getInstance().actionPerformed(pendingJumpAction);
            cleanupSetupsInAndBackToNormalEditingMode();
        }
    }

    private Runnable createPendingJumpAction(final AnActionEvent e) {
        return new Runnable() {
            @Override
            public void run() {
                AceJumpAction.getInstance().actionPerformed(createPendingSelectAndPasteAction(e));
            }
        };
    }

    private ChainActionEvent createPendingSelectAndPasteAction(AnActionEvent e) {
        Runnable selectAndPaste = new Runnable() {
            @Override
            public void run() {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        CommandContext cmdCtx = CopyCutWithoutSelectAction.getInstance().getCmdContext();
                        TextRange tr = Selection.getTextRangeBy(_editor, cmdCtx);
                        if (tr != null) {
                            _editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
                            EditorModificationUtil.pasteFromClipboard(_editor);
                        }
                    }
                };

                ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
            }
        };

        return new ChainActionEvent(e, selectAndPaste, _editor, _project);
    }
}
