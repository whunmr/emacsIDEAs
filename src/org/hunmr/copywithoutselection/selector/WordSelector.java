package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class WordSelector extends Selector {

    public WordSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange(CommandContext cmdCtx) {
        _editor.getSelectionModel().selectWordAtCaret(false);
        int wordStart = _editor.getSelectionModel().getSelectionStart();
        int wordEnd = _editor.getSelectionModel().getSelectionEnd();

        return wordEnd > wordStart ? new TextRange(wordStart, wordEnd) : null;
    }
}
