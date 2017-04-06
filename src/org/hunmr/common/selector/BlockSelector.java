package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class BlockSelector extends Selector {

    public static final int NOT_FOUND = -1;
    public static final String OPEN_BRACKETS = "{[(<";
    public static final String CLOSE_BRACKETS = "}])>";

    public BlockSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange(CommandContext cmdCtx) {
        if (caretIsAtEdge()) {
            return null;
        }

        final int caretOffset = _editor.getCaretModel().getOffset();

        int start = getOffsetOfOpenBracket(caretOffset);
        if (start == NOT_FOUND) {
            return null;
        }

        int end = getOffsetOfCloseBracket(caretOffset);
        if (end == NOT_FOUND) {
            return null;
        }

        if (cmdCtx.getLastCmdKey() == 'B') {
            start--;
            end++;
        }

        return end > start ? new TextRange(start, end) : null;
    }

    private int getOffsetOfOpenBracket(int caretOffset) {
        int i = 0;
        for (int offset = caretOffset; offset > 0; offset--) {
            i = i + getCharScore(_docText.charAt(offset - 1));

            if (i > 0) {
                return offset;
            }
        }

        return NOT_FOUND;
    }

    private int getOffsetOfCloseBracket(int caretOffset) {
        int i = 0;
        int docEndOffset = _docText.length();

        for (int offset = caretOffset; offset < docEndOffset; offset++) {
            i = i + getCharScore(_docText.charAt(offset));

            if (i < 0) {
                return offset;
            }
        }

        return NOT_FOUND;
    }

    private int getCharScore(char c) {
        boolean isOpenBracketChar = OPEN_BRACKETS.indexOf(c) != -1;
        if (isOpenBracketChar) {
            return 1;
        }

        boolean isCloseBracketChar = CLOSE_BRACKETS.indexOf(c) != -1;
        if (isCloseBracketChar) {
            return -1;
        }

        return 0;
    }

}
