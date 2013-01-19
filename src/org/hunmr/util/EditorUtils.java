package org.hunmr.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.TextRange;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class EditorUtils {
    public static TextRange getVisibleTextRange(Editor editor) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();

        LogicalPosition startLogicalPosition = editor.xyToLogicalPosition(visibleArea.getLocation());

        Double endVisiualX = visibleArea.getX() + visibleArea.getWidth();
        Double endVisiualY = visibleArea.getY() + visibleArea.getHeight();
        LogicalPosition endLogicalPosition = editor.xyToLogicalPosition(new Point(endVisiualX.intValue(), endVisiualY.intValue()));

        return new TextRange(editor.logicalPositionToOffset(startLogicalPosition), editor.logicalPositionToOffset(endLogicalPosition));
    }

    public static ArrayList<Integer> getOffsetsOfCharIgnoreCase(char charToFind, TextRange markerRange, Document document) {
        ArrayList<Integer> offsets = new ArrayList<Integer>();

        String visibleText = document.getText(markerRange);

        char lowCase = Character.toLowerCase(charToFind);
        char upperCase = Character.toUpperCase(charToFind);

        getOffsetsOfChar(markerRange.getStartOffset(), lowCase, visibleText, offsets);
        if (upperCase != lowCase) {
            getOffsetsOfChar(markerRange.getStartOffset(), upperCase, visibleText, offsets);
        }

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

    public static boolean isPrintableChar( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }
}
