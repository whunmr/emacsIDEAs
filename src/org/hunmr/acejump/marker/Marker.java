package org.hunmr.acejump.marker;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.Collections;

public class Marker {
    private char _markupChar;
    private ArrayList<Integer> _offsets = new ArrayList<Integer>();

    public Marker(char markupChar, int offset) {
        _markupChar = markupChar;
        addOffsetToMarker(offset);
    }

    public void addOffsetToMarker(int offset) {
        _offsets.add(offset);
    }

    public ArrayList<Integer> getOffsets() {
        return _offsets;
    }

    public char getMarkupChar() {
        return _markupChar;
    }

    public int getOffset() {
        return _offsets.get(0);
    }

    public boolean isMappingToMultipleOffset() {
        return _offsets.size() > 1;
    }

    public TextRange getTextRange() {
        Collections.sort(_offsets);

        Integer startOffset = _offsets.get(0);
        Integer endOffset = _offsets.get(_offsets.size() - 1);

        return new TextRange(startOffset, endOffset);
    }
}
