package org.hunmr.acejump.marker;

import com.intellij.openapi.util.TextRange;

import java.util.ArrayList;
import java.util.Collections;

public class Marker {
    private char _markerChar;
    private ArrayList<Integer> _offsets = new ArrayList<Integer>();

    public Marker(char markerChar, int offset) {
        _markerChar = markerChar;
        addOffsetToMarker(offset);
    }

    public void addOffsetToMarker(int offset) {
        _offsets.add(offset);
    }

    public ArrayList<Integer> getOffsets() {
        return _offsets;
    }

    public char getMarkerChar() {
        return _markerChar;
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
