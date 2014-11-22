package org.hunmr.acejump.command;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.impl.EditorCopyPasteHelperImpl;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.EditorUtils;

public class ObtainAndPasteRangeAfterJumpCommand extends CommandAroundJump {

    private final Class<? extends Selector> _selectorClass;

    public ObtainAndPasteRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }

    @Override
    public void afterJump(final int jumpTargetOffset) {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                EditorUtils.copyRange(_selectorClass, _editor);
                _editor.getCaretModel().moveToOffset(getOffsetBeforeJump());

                EditorUtils.selectRangeOf(_selectorClass, _editor);
                TextRange[] tr = EditorCopyPasteHelperImpl.getInstance().pasteFromClipboard(_editor);
                EditorUtils.selectTextRange(_editor, tr);
            }
        };

        ApplicationManager.getApplication().runWriteAction(AppUtil.getRunnableWrapper(runnable, _editor));
    }
}
