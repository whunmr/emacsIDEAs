package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentInsertionPlanner;
import org.hunmr.util.AppUtil;

import java.util.IdentityHashMap;
import java.util.Map;

public class CopyArgumentAfterJumpCommand extends CommandAroundJump {
    private final IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> _targets;

    public CopyArgumentAfterJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        super(editor);
        _targets = targets;
    }

    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor() || _targets == null) {
            return;
        }

        Map<Integer, ArgumentCandidate> candidatesByAnchor = _targets.get(_te);
        if (candidatesByAnchor == null) {
            return;
        }

        final ArgumentCandidate candidate = candidatesByAnchor.get(_toff);
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
