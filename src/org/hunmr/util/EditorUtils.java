package org.hunmr.util;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.common.selector.*;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class EditorUtils {
    public static TextRange getVisibleTextRange(Editor editor) {
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();

        LogicalPosition startLogicalPosition = editor.xyToLogicalPosition(visibleArea.getLocation());

        Double endVisualX = visibleArea.getX() + visibleArea.getWidth();
        Double endVisualY = visibleArea.getY() + visibleArea.getHeight();
        LogicalPosition endLogicalPosition = editor.xyToLogicalPosition(new Point(endVisualX.intValue(), endVisualY.intValue()));

        return new TextRange(editor.logicalPositionToOffset(startLogicalPosition), editor.logicalPositionToOffset(endLogicalPosition));
    }


    public static boolean isPrintableChar( char c ) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of( c );
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.CHAR_UNDEFINED &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }
        
    public static ArrayList<Integer> getVisibleLineStartOffsets(Editor editor) {
        Document document = editor.getDocument();
        ArrayList<Integer> offsets = new ArrayList<Integer>();

        TextRange visibleTextRange = getVisibleTextRange(editor);
        int startLine = document.getLineNumber(visibleTextRange.getStartOffset());
        int endLine = document.getLineNumber(visibleTextRange.getEndOffset());

        for (int i = startLine; i < endLine; i++) {
            offsets.add(document.getLineStartOffset(i));
        }

        return offsets;
    }

    private static Selector getSelector(Class<? extends Selector> selectorClass, Editor editor) {
        try {
            return selectorClass.getDeclaredConstructor(Editor.class).newInstance(editor);
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static TextRange getRangeOf(Class<? extends Selector> selectorClass, Editor editor) {
        Selector selector = getSelector(selectorClass, editor);
        return selector.getRange(new CommandContext());
    }

    public static void selectRangeOf(Class<? extends Selector> selectorClass, Editor editor) {
        TextRange tr = getRangeOf(selectorClass, editor);
        if (tr != null) {
            editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
        }
    }

    public static void copyRange(Class<? extends Selector> selectorClass, Editor editor) {
        selectRangeOf(selectorClass, editor);
        editor.getSelectionModel().copySelectionToClipboard();
        editor.getSelectionModel().removeSelection();
    }
}
