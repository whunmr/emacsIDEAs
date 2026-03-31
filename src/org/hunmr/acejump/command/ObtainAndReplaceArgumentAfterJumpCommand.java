package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentEditPlanner;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentParser;
import org.hunmr.argument.ParsedArguments;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;

import java.util.IdentityHashMap;
import java.util.Map;

public class ObtainAndReplaceArgumentAfterJumpCommand extends ArgumentJumpCommand {
    private ArgumentCandidate _sourceCandidate;

    public ObtainAndReplaceArgumentAfterJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        super(editor, targets);
    }

    @Override
    public void beforeJump(JOffset jumpTargetOffset) {
        if (!hasUsableSourceEditor()) {
            return;
        }

        super.beforeJump(jumpTargetOffset);
        ParsedArguments parsedArguments = ArgumentParser.parse(_se.getDocument().getText());
        _sourceCandidate = parsedArguments.findArgumentAtOrNear(_soff);
    }

    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor() || _sourceCandidate == null) {
            return;
        }

        final ArgumentCandidate targetCandidate = getTargetCandidate();
        if (targetCandidate == null) {
            return;
        }

        final String targetText = targetCandidate.getText(_te.getDocument().getText());
        if (targetText.isEmpty()) {
            return;
        }

        if (_se == _te && ArgumentEditPlanner.sameArgument(_sourceCandidate, targetCandidate)) {
            return;
        }

        AppUtil.runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (!hasUsableSourceEditor()) {
                    return;
                }

                ClipboardEditorUtil.copyToClipboard(targetText);
                Document document = _se.getDocument();
                ArgumentInsertionPlan plan = ArgumentEditPlanner.planReplace(_sourceCandidate, targetText);
                if (plan == null) {
                    return;
                }

                document.replaceString(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());
                _se.getCaretModel().moveToOffset(Math.min(plan.getCaretOffset(), document.getTextLength()));

                if (shouldSelectAfterJump()) {
                    _se.getSelectionModel().setSelection(plan.getSelectionStartOffset(), plan.getSelectionEndOffset());
                } else {
                    _se.getSelectionModel().removeSelection();
                }
            }
        }, _se);
    }
}
