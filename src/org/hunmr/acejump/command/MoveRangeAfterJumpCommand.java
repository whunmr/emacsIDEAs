package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
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

public class MoveRangeAfterJumpCommand extends CommandAroundJump  {
    private final Class<? extends Selector> _selectorClass;
    private int _length;

    public MoveRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
        _length = 0;
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
        if (argumentSelector) {
            if (targetSpan == null || targetText.isEmpty()) {
                return;
            }

            if (inSameEditor() && targetSpan.contains(_soff)) {
                _se.getCaretModel().moveToOffset(_soff);
                return;
            }
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!hasUsableSourceEditor() || !hasUsableTargetEditor()) {
                    return;
                }

                if (argumentSelector) {
                    ClipboardEditorUtil.copyToClipboard(targetText);

                    if (_se == _te) {
                        moveArgumentSliceInsideSameEditor(targetSpan, targetText);
                    } else {
                        moveArgumentSliceAcrossEditors(targetSpan, targetText);
                    }
                    return;
                }

                TextRange sourceRange = EditorUtils.getRangeOf(_selectorClass, _te);
                if (sourceRange == null) {
                    return;
                }

                if (inSameEditor()) {
                    boolean noNeedToMove = sourceRange.contains(_soff);
                    if (noNeedToMove) {
                        _se.getCaretModel().moveToOffset(_soff);
                        return;
                    }
                }

                int textSourceStartOffset = sourceRange.getStartOffset();

                EditorUtils.copyRange(_selectorClass, _te);

                if ( !inSameEditor() || textSourceStartOffset > _soff) {
                    deleteTextSource(_te);

                    pasteClipboardToOffset();
                } else {
                    pasteClipboardToOffset();

                    focusTargetCaret();
                    deleteTextSource(_te);
                    focusSourceCaret();

                    int cur_offset = _se.getCaretModel().getOffset();

                    if (_config._needSelectTextAfterJump) {
                        EditorUtils.selectTextRange(_se, cur_offset - _length, cur_offset);
                    }
                }
            }

            private void deleteTextSource(Editor editor) {
                if (!isUsableEditor(editor)) {
                    return;
                }

                EditorUtils.selectRangeOf(_selectorClass, editor);
                EditorModificationUtil.deleteSelectedText(editor);
            }

            private void pasteClipboardToOffset() {
                focusSourceCaret();

                TextRange[] tr = ClipboardEditorUtil.pasteFromClipboard(_se);
                if (shouldSelectAfterJump() && tr.length > 0) {
                    EditorUtils.selectTextRange(_se, tr);
                }
                _length = tr.length == 0 ? 0 : tr[0].getEndOffset() - tr[0].getStartOffset();
            }

            private void moveArgumentSliceInsideSameEditor(TextSpan targetSpan, String targetText) {
                Document document = _se.getDocument();
                String originalText = document.getText();
                ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(originalText, targetSpan);
                if (deletePlan == null) {
                    return;
                }

                String textAfterDelete = ArgumentEditPlanner.apply(originalText, deletePlan);
                int adjustedSourceOffset = ArgumentEditPlanner.adjustOffsetAfterPlan(_soff, deletePlan);
                ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(textAfterDelete, adjustedSourceOffset, targetText);
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
            }

            private void moveArgumentSliceAcrossEditors(TextSpan targetSpan, String targetText) {
                Document targetDocument = _te.getDocument();
                ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(targetDocument.getText(), targetSpan);
                if (deletePlan == null) {
                    return;
                }

                targetDocument.replaceString(deletePlan.getReplaceStartOffset(), deletePlan.getReplaceEndOffset(), deletePlan.getInsertedText());

                focusSourceCaret();
                Document sourceDocument = _se.getDocument();
                ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(sourceDocument.getText(), _soff, targetText);
                if (insertPlan == null) {
                    return;
                }

                sourceDocument.replaceString(insertPlan.getReplaceStartOffset(), insertPlan.getReplaceEndOffset(), insertPlan.getInsertedText());
                _se.getCaretModel().moveToOffset(Math.min(insertPlan.getCaretOffset(), sourceDocument.getTextLength()));

                if (shouldSelectAfterJump()) {
                    _se.getSelectionModel().setSelection(insertPlan.getSelectionStartOffset(), insertPlan.getSelectionEndOffset());
                } else {
                    _se.getSelectionModel().removeSelection();
                }
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
