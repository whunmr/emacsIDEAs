package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

public class WordSelector extends Selector {

    public WordSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange() {
        _editor.getSelectionModel().selectWordAtCaret(false);
        int wordStart = _editor.getSelectionModel().getSelectionStart();
        int wordEnd = _editor.getSelectionModel().getSelectionEnd();

        return wordEnd > wordStart ? new TextRange(wordStart, wordEnd) : null;
    }
}
