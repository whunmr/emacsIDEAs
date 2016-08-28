package org.hunmr.acejump.marker;

import com.intellij.openapi.editor.Editor;

public class JOffset {
    public Editor editor;
    public int offset;

    public JOffset(Editor editor, int offset) {
        this.editor = editor;
        this.offset = offset;
    }

    public void restoreCaret() {
        editor.getContentComponent().requestFocus();
        editor.getCaretModel().moveToOffset(offset);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JOffset jOffset = (JOffset) o;

        if (offset != jOffset.offset) return false;
        return !(editor != null ? !editor.equals(jOffset.editor) : jOffset.editor != null);

    }

    @Override
    public int hashCode() {
        int result = editor != null ? editor.hashCode() : 0;
        result = 31 * result + offset;
        return result;
    }
}
