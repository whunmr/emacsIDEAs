package org.hunmr.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.TextRange;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

public class EditorUtils {
    public static TextRange getVisibleTextRange(Editor editor) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();

        LogicalPosition startLogicalPosition = editor.xyToLogicalPosition(visibleArea.getLocation());

        Double endVisiualX = visibleArea.getX() + visibleArea.getWidth();
        Double endVisiualY = visibleArea.getY() + visibleArea.getHeight();
        LogicalPosition endLogicalPosition = editor.xyToLogicalPosition(new Point(endVisiualX.intValue(), endVisiualY.intValue() - editor.getLineHeight()));

        return new TextRange(editor.logicalPositionToOffset(startLogicalPosition), editor.logicalPositionToOffset(endLogicalPosition));
    }

    public static ArrayList<Integer> getOffsetsOfCharIgnoreCase(char charToSet, TextRange markerRange, Document document) {
        ArrayList<Integer> offsets = new ArrayList<Integer>();

        int startOffset = markerRange.getStartOffset();
        String visibleText = document.getText(markerRange);

        getOffsetsOfChar(startOffset, Character.toLowerCase(charToSet), visibleText, offsets);
        getOffsetsOfChar(startOffset, Character.toUpperCase(charToSet), visibleText, offsets);

        Collections.reverse(offsets);
        return offsets;
    }

    private static void getOffsetsOfChar(int startOffset, char charToSet, String visibleText, ArrayList<Integer> offsets) {
        int index = visibleText.indexOf(charToSet);
        while (index >= 0) {
            int offset = startOffset + index;
            offsets.add(offset);
            index = visibleText.indexOf(charToSet, index + 1);
        }
    }
}
