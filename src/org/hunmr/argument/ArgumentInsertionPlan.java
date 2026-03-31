package org.hunmr.argument;

public final class ArgumentInsertionPlan {
    private final int _replaceStartOffset;
    private final int _replaceEndOffset;
    private final String _insertedText;
    private final int _selectionStartOffset;
    private final int _selectionEndOffset;
    private final int _caretOffset;

    public ArgumentInsertionPlan(int replaceStartOffset,
                                 int replaceEndOffset,
                                 String insertedText,
                                 int selectionStartOffset,
                                 int selectionEndOffset,
                                 int caretOffset) {
        _replaceStartOffset = replaceStartOffset;
        _replaceEndOffset = replaceEndOffset;
        _insertedText = insertedText == null ? "" : insertedText;
        _selectionStartOffset = selectionStartOffset;
        _selectionEndOffset = selectionEndOffset;
        _caretOffset = caretOffset;
    }

    public int getReplaceStartOffset() {
        return _replaceStartOffset;
    }

    public int getReplaceEndOffset() {
        return _replaceEndOffset;
    }

    public String getInsertedText() {
        return _insertedText;
    }

    public int getSelectionStartOffset() {
        return _selectionStartOffset;
    }

    public int getSelectionEndOffset() {
        return _selectionEndOffset;
    }

    public int getCaretOffset() {
        return _caretOffset;
    }
}
