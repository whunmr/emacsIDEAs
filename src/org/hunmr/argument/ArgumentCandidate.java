package org.hunmr.argument;

public final class ArgumentCandidate {
    private final TextSpan _range;
    private final int _anchorOffset;
    private final int _listOpenOffset;
    private final int _listCloseOffset;
    private final int _indexInList;

    public ArgumentCandidate(TextSpan range, int anchorOffset, int listOpenOffset, int listCloseOffset, int indexInList) {
        _range = range;
        _anchorOffset = anchorOffset;
        _listOpenOffset = listOpenOffset;
        _listCloseOffset = listCloseOffset;
        _indexInList = indexInList;
    }

    public TextSpan getRange() {
        return _range;
    }

    public int getAnchorOffset() {
        return _anchorOffset;
    }

    public int getListOpenOffset() {
        return _listOpenOffset;
    }

    public int getListCloseOffset() {
        return _listCloseOffset;
    }

    public int getIndexInList() {
        return _indexInList;
    }

    public boolean contains(int offset) {
        return _range != null && offset >= _range.getStartOffset() && offset <= _range.getEndOffset();
    }

    public int distanceTo(int offset) {
        if (_range == null) {
            return Integer.MAX_VALUE;
        }

        if (contains(offset)) {
            return 0;
        }

        if (offset < _range.getStartOffset()) {
            return _range.getStartOffset() - offset;
        }

        return offset - _range.getEndOffset();
    }

    public String getText(String text) {
        if (_range == null || text == null) {
            return "";
        }

        int start = Math.min(_range.getStartOffset(), text.length());
        int end = Math.min(_range.getEndOffset(), text.length());
        return start < end ? text.substring(start, end) : "";
    }
}
