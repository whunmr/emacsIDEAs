package org.hunmr.acejump.marker;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import org.hunmr.options.PluginConfig;

public class JOffset {
    public Editor editor;
    public int offset;
    final PluginConfig _config = ServiceManager.getService(PluginConfig.class);

    public JOffset(Editor editor, int offset) {
        this.editor = editor;
        if (_config._markBehindChar) {
            this.offset = offset + 1;
        } else {
            this.offset = offset;
        }
    }

    public void restoreCaret() {
        editor.getContentComponent().requestFocus();
        if (_config._jumpBehindChar) {
            editor.getCaretModel().moveToOffset(offset + 1);
        } else {
            editor.getCaretModel().moveToOffset(offset);
        }
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
