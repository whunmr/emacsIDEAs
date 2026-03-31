package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentEditPlanner;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentSelectorSupport;
import org.hunmr.argument.TextSpan;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;
import org.hunmr.util.EditorUtils;

public class DeleteRangeAfterJumpCommand extends CommandAroundJump  {
    private final Class<? extends Selector> _selectorClass;

    public DeleteRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }


    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
            return;
        }

        final boolean argumentSelector = ArgumentSelectorSupport.isArgumentSelector(_selectorClass);
        final String targetDocumentText = _te.getDocument().getText();
        final TextSpan targetSpan = argumentSelector
                ? ArgumentSelectorSupport.getTextSpan(_selectorClass, targetDocumentText, _toff)
                : null;
        final String targetText = argumentSelector && targetSpan != null
                ? targetDocumentText.substring(targetSpan.getStartOffset(), targetSpan.getEndOffset())
                : "";
        if (argumentSelector && (targetSpan == null || targetText.isEmpty())) {
            return;
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!hasUsableTargetEditor()) {
                    return;
                }

                if (argumentSelector) {
                    ClipboardEditorUtil.copyToClipboard(targetText);

                    Document document = _te.getDocument();
                    ArgumentInsertionPlan plan = ArgumentEditPlanner.planDelete(document.getText(), targetSpan);
                    if (plan == null) {
                        return;
                    }

                    document.replaceString(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());
                    _te.getSelectionModel().removeSelection();
                    _te.getCaretModel().moveToOffset(Math.min(plan.getCaretOffset(), document.getTextLength()));
                    return;
                }

                //TODO: add option to specify whether copy or not before delete
                EditorUtils.copyRange(_selectorClass, _te);
                EditorUtils.deleteRange(_selectorClass, _te);
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
