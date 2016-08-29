package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.EditorUtils;

public class CopyRangeAfterJumpCommand extends CommandAroundJump  {
    private final Class<? extends Selector> _selectorClass;

    public CopyRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }


    @Override
    public void afterJump() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                EditorUtils.copyRange(_selectorClass, _te);
                pasteClipboardToOffset();
            }

            private void pasteClipboardToOffset() {
                focusSourceCaret();

                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_se);

                if (_config._needSelectTextAfterJump)
                    EditorUtils.selectTextRange(_se, tr);
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}