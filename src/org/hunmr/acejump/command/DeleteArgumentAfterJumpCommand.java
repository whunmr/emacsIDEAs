package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentEditPlanner;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;

import java.util.IdentityHashMap;
import java.util.Map;

public class DeleteArgumentAfterJumpCommand extends ArgumentJumpCommand {
    public DeleteArgumentAfterJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        super(editor, targets);
    }

    @Override
    public void afterJump() {
        if (!hasUsableTargetEditor()) {
            return;
        }

        final ArgumentCandidate candidate = getTargetCandidate();
        if (candidate == null) {
            return;
        }

        final String deletedText = candidate.getText(_te.getDocument().getText());
        if (deletedText.isEmpty()) {
            return;
        }

        AppUtil.runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (!hasUsableTargetEditor()) {
                    return;
                }

                ClipboardEditorUtil.copyToClipboard(deletedText);
                Document document = _te.getDocument();
                ArgumentInsertionPlan plan = ArgumentEditPlanner.planDelete(document.getText(), candidate);
                if (plan == null) {
                    return;
                }

                document.replaceString(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());
                _te.getSelectionModel().removeSelection();
                _te.getCaretModel().moveToOffset(Math.min(plan.getCaretOffset(), document.getTextLength()));
            }
        }, _te);
    }
}
