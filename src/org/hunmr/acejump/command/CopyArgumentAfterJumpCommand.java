package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentInsertionPlanner;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;

import java.util.IdentityHashMap;
import java.util.Map;

public class CopyArgumentAfterJumpCommand extends ArgumentJumpCommand {
    public CopyArgumentAfterJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        super(editor, targets);
    }

    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
            return;
        }

        final ArgumentCandidate candidate = getTargetCandidate();
        if (candidate == null) {
            return;
        }

        final String targetText = candidate.getText(_te.getDocument().getText());
        if (targetText.isEmpty()) {
            return;
        }

        AppUtil.runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (!hasUsableSourceEditor()) {
                    return;
                }

                ClipboardEditorUtil.copyToClipboard(targetText);
                focusSourceCaret();
                Document document = _se.getDocument();
                ArgumentInsertionPlan plan = ArgumentInsertionPlanner.plan(document.getText(), _soff, targetText);

                document.replaceString(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());
                _se.getCaretModel().moveToOffset(plan.getCaretOffset());

                if (shouldSelectAfterJump()) {
                    _se.getSelectionModel().setSelection(plan.getSelectionStartOffset(), plan.getSelectionEndOffset());
                } else {
                    _se.getSelectionModel().removeSelection();
                }
            }
        }, _se);
    }
}
