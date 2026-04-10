package org.hunmr.util;

import com.intellij.codeInsight.actions.ReformatCodeAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;
import org.hunmr.common.selector.Selector;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class EditorUtils {
    public static TextRange getVisibleTextRange(Editor editor) {
        if (!isUsableEditor(editor)) {
            return TextRange.EMPTY_RANGE;
        }

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
        ArrayList<Integer> offsets = new ArrayList<Integer>();
        if (!isUsableEditor(editor)) {
            return offsets;
        }

        Document document = editor.getDocument();

        TextRange visibleTextRange = getVisibleTextRange(editor);
        int startLine = document.getLineNumber(visibleTextRange.getStartOffset());
        int endLine = document.getLineNumber(visibleTextRange.getEndOffset());

        for (int i = startLine; i < endLine; i++) {
            offsets.add(document.getLineStartOffset(i));
        }

        return offsets;
    }

    private static Selector getSelector(Class<? extends Selector> selectorClass, Editor editor) {
        if (selectorClass == null || !isUsableEditor(editor)) {
            return null;
        }

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
        return selector != null ? selector.getRange(new CommandContext()) : null;
    }

    public static void selectRangeOf(Class<? extends Selector> selectorClass, Editor editor) {
        if (!isUsableEditor(editor)) {
            return;
        }

        TextRange tr = getRangeOf(selectorClass, editor);
        if (tr != null) {
            editor.getSelectionModel().setSelection(tr.getStartOffset(), tr.getEndOffset());
        }
    }

    public static void copyRange(Class<? extends Selector> selectorClass, Editor editor) {
        if (!isUsableEditor(editor)) {
            return;
        }

        selectRangeOf(selectorClass, editor);
        editor.getSelectionModel().copySelectionToClipboard();
        editor.getSelectionModel().removeSelection();
    }

    public static void deleteRange(Class<? extends Selector> selectorClass, Editor editor) {
        if (!isUsableEditor(editor)) {
            return;
        }

        selectRangeOf(selectorClass, editor);
        EditorModificationUtil.deleteSelectedText(editor);
    }

    public static void reformatCode(AnActionEvent e) {
        if (e == null) {
            return;
        }

        ReformatCodeAction reformat = new ReformatCodeAction();
        ActionUtil.invokeAction(reformat, e.getDataContext(), e.getPlace(), e.getInputEvent(), null);
    }

    public static void selectTextRange(Editor editor, TextRange[] tr) {
        if (isUsableEditor(editor) && tr != null && tr.length > 0) {
            editor.getSelectionModel().setSelection(tr[0].getStartOffset(), tr[0].getEndOffset());
        }
    }

    public static void selectTextRange(Editor editor, int startOffset, int endOffset) {
        if (isUsableEditor(editor)) {
            editor.getSelectionModel().setSelection(startOffset, endOffset);
        }
    }

    public static void deleteRange(TextRange tr, Editor editor) {
        if (tr == null || !isUsableEditor(editor)) {
            return;
        }

        selectTextRange(editor, new TextRange[] {tr} );
        EditorModificationUtil.deleteSelectedText(editor);
    }

    private static boolean isUsableEditor(Editor editor) {
        return editor != null && !editor.isDisposed();
    }
}
