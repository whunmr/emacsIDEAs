package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.argument.ArgumentEditPlanner;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentInsertionPlanner;
import org.hunmr.argument.ArgumentSelectorSupport;
import org.hunmr.argument.TextSpan;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;
import org.hunmr.util.EditorUtils;

public class ObtainAndPasteRangeAfterJumpCommand extends CommandAroundJump {

    private final Class<? extends Selector> _selectorClass;

    public ObtainAndPasteRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }

    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
            return;
        }

        final boolean argumentSelector = ArgumentSelectorSupport.isArgumentSelector(_selectorClass);
        final String sourceDocumentText = _se.getDocument().getText();
        final String targetDocumentText = _te.getDocument().getText();
        final TextSpan sourceSpan = argumentSelector
                ? ArgumentSelectorSupport.getTextSpan(_selectorClass, sourceDocumentText, _soff)
                : null;
        final TextSpan targetSpan = argumentSelector
                ? ArgumentSelectorSupport.getTextSpan(_selectorClass, targetDocumentText, _toff)
                : null;
        final String targetText = argumentSelector && targetSpan != null
                ? targetDocumentText.substring(targetSpan.getStartOffset(), targetSpan.getEndOffset())
                : "";
        if (argumentSelector && (sourceSpan == null || targetSpan == null || targetText.isEmpty())) {
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
                    return;
                }

                if (argumentSelector) {
                    ClipboardEditorUtil.copyToClipboard(targetText);
                    focusSourceCaret();

                    Document document = _se.getDocument();
                    ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(document.getText(), sourceSpan);
                    if (deletePlan == null) {
                        return;
                    }

                    String textAfterDelete = ArgumentEditPlanner.apply(document.getText(), deletePlan);
                    int insertOffset = ArgumentEditPlanner.adjustOffsetAfterPlan(sourceSpan.getStartOffset(), deletePlan);
                    ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(textAfterDelete, insertOffset, targetText);
                    if (insertPlan == null) {
                        return;
                    }

                    document.replaceString(deletePlan.getReplaceStartOffset(), deletePlan.getReplaceEndOffset(), deletePlan.getInsertedText());
                    document.replaceString(insertPlan.getReplaceStartOffset(), insertPlan.getReplaceEndOffset(), insertPlan.getInsertedText());
                    _se.getCaretModel().moveToOffset(Math.min(insertPlan.getCaretOffset(), document.getTextLength()));

                    if (shouldSelectAfterJump()) {
                        _se.getSelectionModel().setSelection(insertPlan.getSelectionStartOffset(), insertPlan.getSelectionEndOffset());
                    } else {
                        _se.getSelectionModel().removeSelection();
                    }
                    return;
                }

                EditorUtils.copyRange(_selectorClass, _te);
                focusSourceCaret();

                EditorUtils.selectRangeOf(_selectorClass, _se);
                EditorUtils.deleteRange(_selectorClass, _se);
                TextRange[] tr = ClipboardEditorUtil.pasteFromClipboard(_se);

                if (shouldSelectAfterJump() && tr.length > 0) {
                    EditorUtils.selectTextRange(_se, tr);
                }
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
