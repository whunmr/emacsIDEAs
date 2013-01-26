package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

public class QuoteSelector extends Selector{
    public QuoteSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange() {
        if (caretIsAtEdge()) {
            return null;
        }

        final String docText = normalizeDocumentText();

        final int quoteCharIndex = backwardFindNearestQuoteCharOffset(docText);
        if (quoteCharIndex == -1) {
            return null;
        }

        final char quoteChar = _docText.charAt(quoteCharIndex);
        TextRange tr = findQuotedTextRange(docText, quoteChar);

        return tr != null ? tr : findQuotedTextRange(docText, nextQuoteChar(quoteChar));
    }

    private char nextQuoteChar(char quoteChar) {
        return quoteChar == '\'' ? '"' : '\'';
    }

    private TextRange findQuotedTextRange(String docText, char quoteChar) {
        final int caretOffset = _editor.getCaretModel().getOffset();
        final int startQuoteOffset = docText.lastIndexOf(quoteChar, caretOffset-1);
        if (startQuoteOffset == -1) {
            return null;
        }

        int start = startQuoteOffset + 1;
        int end = docText.indexOf(quoteChar, caretOffset);

        return end > start ? new TextRange(start, end) : null;
    }

    private int backwardFindNearestQuoteCharOffset(String docText) {
        int offset = _editor.getCaretModel().getOffset();
        return Math.max(docText.lastIndexOf("'", offset-1), docText.lastIndexOf("\"", offset-1));
    }

    private boolean caretIsAtEdge() {
        int offset = _editor.getCaretModel().getOffset();
        return offset == 0 || offset == _docText.length();
    }

    private String normalizeDocumentText() {
        return _docText.replace("\\\\", "aa").replace("\\\"", "aa").replace("\\\'", "aa");
    }


}
