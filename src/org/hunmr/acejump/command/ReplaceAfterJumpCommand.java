package org.hunmr.acejump.command;

import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.argument.ArgumentEditPlanner;
import org.hunmr.argument.ArgumentInsertionPlan;
import org.hunmr.argument.ArgumentInsertionPlanner;
import org.hunmr.argument.ArgumentSelectorSupport;
import org.hunmr.argument.TextSpan;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.ClipboardEditorUtil;
import org.hunmr.util.EditorUtils;

public class ReplaceAfterJumpCommand extends CommandAroundJump {
    private boolean _addNewLineBeforePaste;
    protected final Class<? extends Selector> _selectorClass;

    private int _caretOffsetFromSelectRangeStartBeforeJump;
    private boolean _hasSourceRange;
    private String _sourceText;
    private int _sourceRangeStartBeforeJump;
    private int _sourceRangeEndBeforeJump;

    public ReplaceAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _addNewLineBeforePaste = false;
        _selectorClass = selectorClass;
        _hasSourceRange = false;
        _sourceText = "";
        _sourceRangeStartBeforeJump = -1;
        _sourceRangeEndBeforeJump = -1;
    }

    @Override
    public void beforeJump(final JOffset jumpTargetOffset) {
        if (!hasUsableSourceEditor()) {
            return;
        }

        super.beforeJump(jumpTargetOffset);
        _sourceText = "";
        _sourceRangeStartBeforeJump = -1;
        _sourceRangeEndBeforeJump = -1;
        EditorUtils.selectRangeOf(_selectorClass, _se);
        if (!_se.getSelectionModel().hasSelection()) {
            _hasSourceRange = false;
            return;
        }

        _hasSourceRange = true;
        _sourceText = _se.getSelectionModel().getSelectedText();
        _sourceRangeStartBeforeJump = _se.getSelectionModel().getSelectionStart();
        _sourceRangeEndBeforeJump = _se.getSelectionModel().getSelectionEnd();
        _se.getSelectionModel().copySelectionToClipboard();
        _caretOffsetFromSelectRangeStartBeforeJump = _soff - _se.getSelectionModel().getSelectionStart();
        focusSourceCaret();
    }

    @Override
    public void afterJump() {
        if (!hasUsableSourceEditor() || !hasUsableTargetEditor() || !_hasSourceRange) {
            return;
        }

        final boolean argumentSelector = ArgumentSelectorSupport.isArgumentSelector(_selectorClass);
        final String targetDocumentText = _te.getDocument().getText();
        final TextSpan targetSpan = argumentSelector
                ? ArgumentSelectorSupport.getTextSpan(_selectorClass, targetDocumentText, _toff)
                : null;
        if (argumentSelector) {
            if (_sourceText == null || _sourceText.isEmpty() || targetSpan == null) {
                return;
            }

            if (_se == _te
                    && targetSpan.getStartOffset() == _sourceRangeStartBeforeJump
                    && targetSpan.getEndOffset() == _sourceRangeEndBeforeJump) {
                return;
            }
        }

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (!hasUsableTargetEditor()) {
                    return;
                }

                if (argumentSelector) {
                    ClipboardEditorUtil.copyToClipboard(_sourceText);
                    Document document = _te.getDocument();
                    ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(document.getText(), targetSpan);
                    if (deletePlan == null) {
                        return;
                    }

                    String textAfterDelete = ArgumentEditPlanner.apply(document.getText(), deletePlan);
                    int insertOffset = ArgumentEditPlanner.adjustOffsetAfterPlan(targetSpan.getStartOffset(), deletePlan);
                    ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(textAfterDelete, insertOffset, _sourceText);
                    if (insertPlan == null) {
                        return;
                    }

                    document.replaceString(deletePlan.getReplaceStartOffset(), deletePlan.getReplaceEndOffset(), deletePlan.getInsertedText());
                    document.replaceString(insertPlan.getReplaceStartOffset(), insertPlan.getReplaceEndOffset(), insertPlan.getInsertedText());
                    _te.getCaretModel().moveToOffset(Math.min(insertPlan.getCaretOffset(), document.getTextLength()));

                    if (shouldSelectAfterJump()) {
                        _te.getSelectionModel().setSelection(insertPlan.getSelectionStartOffset(), insertPlan.getSelectionEndOffset());
                    } else {
                        _te.getSelectionModel().removeSelection();
                    }
                    return;
                }

                TextRange tr = EditorUtils.getRangeOf(_selectorClass, _te);
                if (tr != null)
                {
                    _te.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
                    EditorUtils.deleteRange(tr, _te);
                }

                CaretModel targetCaret = _te.getCaretModel();

                if (_addNewLineBeforePaste) {
                    _te.getDocument().insertString(targetCaret.getOffset(), "\n");
                    targetCaret.moveToOffset(targetCaret.getOffset() + 1);
                }

                TextRange[] textRanges = ClipboardEditorUtil.pasteFromClipboard(_te);

                if (shouldSelectAfterJump() && textRanges.length > 0) {
                    int caret = textRanges[0].getStartOffset() + _caretOffsetFromSelectRangeStartBeforeJump;
                    targetCaret.moveToOffset(caret);
                    EditorUtils.selectRangeOf(_selectorClass, _te);
                }
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}
