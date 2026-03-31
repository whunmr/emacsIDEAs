package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentInsertionPlanner;
import org.hunmr.argument.ArgumentSelectorSupport;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;
import org.hunmr.util.EditorUtils;

public class CopyRangeAfterJumpCommand extends CommandAroundJump  {
    private final Class<? extends Selector> _selectorClass;

    public CopyRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }


    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
            return;
        }

        final boolean argumentSelector = ArgumentSelectorSupport.isArgumentSelector(_selectorClass);
        final String targetText = argumentSelector
                ? ArgumentSelectorSupport.getSelectedText(_selectorClass, _te.getDocument().getText(), _toff)
                : "";
        if (argumentSelector && targetText.isEmpty()) {
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
                    ArgumentInsertionPlan plan = ArgumentInsertionPlanner.plan(document.getText(), _soff, targetText);
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
                    return;
                }

                EditorUtils.copyRange(_selectorClass, _te);
                pasteClipboardToOffset();
            }

            private void pasteClipboardToOffset() {
                focusSourceCaret();

                TextRange[] tr = ClipboardEditorUtil.pasteFromClipboard(_se);

                if (shouldSelectAfterJump() && tr.length > 0)
                    EditorUtils.selectTextRange(_se, tr);
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
