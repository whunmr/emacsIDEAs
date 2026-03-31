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

public class ReplaceArgumentAfterJumpCommand extends ArgumentJumpCommand {
    private ArgumentCandidate _sourceCandidate;
    private String _sourceArgumentText;

    public ReplaceArgumentAfterJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
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
        _sourceArgumentText = _sourceCandidate == null ? "" : _sourceCandidate.getText(_se.getDocument().getText());
    }

    @Override
    public void afterJump() {
        if (!hasUsableTargetEditor() || _sourceCandidate == null || _sourceArgumentText == null || _sourceArgumentText.isEmpty()) {
            return;
        }

        final ArgumentCandidate targetCandidate = getTargetCandidate();
        if (targetCandidate == null) {
            return;
        }

        if (_se == _te && ArgumentEditPlanner.sameArgument(_sourceCandidate, targetCandidate)) {
            return;
        }

        AppUtil.runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (!hasUsableTargetEditor()) {
                    return;
                }

                ClipboardEditorUtil.copyToClipboard(_sourceArgumentText);
                Document document = _te.getDocument();
                ArgumentInsertionPlan plan = ArgumentEditPlanner.planReplace(document.getText(), targetCandidate, _sourceArgumentText);
                if (plan == null) {
                    return;
                }

                document.replaceString(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());
                _te.getCaretModel().moveToOffset(Math.min(plan.getCaretOffset(), document.getTextLength()));

                if (shouldSelectAfterJump()) {
                    _te.getSelectionModel().setSelection(plan.getSelectionStartOffset(), plan.getSelectionEndOffset());
                } else {
                    _te.getSelectionModel().removeSelection();
                }
            }
        }, _te);
    }
}
