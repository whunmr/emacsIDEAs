package org.hunmr.argument;

public final class TextSpan {
    private final int _startOffset;
    private final int _endOffset;

    public TextSpan(int startOffset, int endOffset) {
        _startOffset = Math.max(0, startOffset);
        _endOffset = Math.max(_startOffset, endOffset);
    }

    public int getStartOffset() {
        return _startOffset;
    }

    public int getEndOffset() {
        return _endOffset;
    }

    public int length() {
        return _endOffset - _startOffset;
    }

    public boolean contains(int offset) {
        return offset >= _startOffset && offset <= _endOffset;
    }
}
