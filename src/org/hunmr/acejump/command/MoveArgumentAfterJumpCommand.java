package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentEditPlanner;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentInsertionPlanner;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;

import java.util.IdentityHashMap;
import java.util.Map;

public class MoveArgumentAfterJumpCommand extends ArgumentJumpCommand {
    public MoveArgumentAfterJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        super(editor, targets);
    }

    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
            return;
        }

        final ArgumentCandidate targetCandidate = getTargetCandidate();
        if (targetCandidate == null || targetCandidate.contains(_soff) && _se == _te) {
            return;
        }

        final String targetText = targetCandidate.getText(_te.getDocument().getText());
        if (targetText.isEmpty()) {
            return;
        }

        AppUtil.runWriteAction(new Runnable() {
            @Override
            public void run() {
                if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
                    return;
                }

                ClipboardEditorUtil.copyToClipboard(targetText);

                if (_se == _te) {
                    moveInsideSameEditor(targetCandidate, targetText);
                } else {
                    moveAcrossEditors(targetCandidate, targetText);
                }
            }

            private void moveInsideSameEditor(ArgumentCandidate targetCandidate, String targetText) {
                Document document = _se.getDocument();
                String originalText = document.getText();
                ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(originalText, targetCandidate);
                if (deletePlan == null) {
                    return;
                }

                String textAfterDelete = ArgumentEditPlanner.apply(originalText, deletePlan);
                int adjustedSourceOffset = ArgumentEditPlanner.adjustOffsetAfterPlan(_soff, deletePlan);
                ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(textAfterDelete, adjustedSourceOffset, targetText);

                document.replaceString(deletePlan.getReplaceStartOffset(), deletePlan.getReplaceEndOffset(), deletePlan.getInsertedText());

                int insertStart = insertPlan.getReplaceStartOffset();
                int insertEnd = insertPlan.getReplaceEndOffset();
                document.replaceString(insertStart, insertEnd, insertPlan.getInsertedText());
                _se.getCaretModel().moveToOffset(Math.min(insertPlan.getCaretOffset(), document.getTextLength()));

                if (shouldSelectAfterJump()) {
                    _se.getSelectionModel().setSelection(insertPlan.getSelectionStartOffset(), insertPlan.getSelectionEndOffset());
                } else {
                    _se.getSelectionModel().removeSelection();
                }
            }

            private void moveAcrossEditors(ArgumentCandidate targetCandidate, String targetText) {
                Document targetDocument = _te.getDocument();
                ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(targetDocument.getText(), targetCandidate);
                if (deletePlan == null) {
                    return;
                }

                targetDocument.replaceString(deletePlan.getReplaceStartOffset(), deletePlan.getReplaceEndOffset(), deletePlan.getInsertedText());

                focusSourceCaret();
                Document sourceDocument = _se.getDocument();
                ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(sourceDocument.getText(), _soff, targetText);
                sourceDocument.replaceString(insertPlan.getReplaceStartOffset(), insertPlan.getReplaceEndOffset(), insertPlan.getInsertedText());
                _se.getCaretModel().moveToOffset(Math.min(insertPlan.getCaretOffset(), sourceDocument.getTextLength()));

                if (shouldSelectAfterJump()) {
                    _se.getSelectionModel().setSelection(insertPlan.getSelectionStartOffset(), insertPlan.getSelectionEndOffset());
                } else {
                    _se.getSelectionModel().removeSelection();
                }
            }
        }, _se);
    }
}
