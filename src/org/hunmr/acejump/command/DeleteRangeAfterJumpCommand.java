package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.AppUtil;
import org.hunmr.util.EditorUtils;

public class DeleteRangeAfterJumpCommand extends CommandAroundJump  {
    private final Class<? extends Selector> _selectorClass;

    public DeleteRangeAfterJumpCommand(Editor editor, Class<? extends Selector> selectorClass) {
        super(editor);
        _selectorClass = selectorClass;
    }


    @Override
    public void afterJump() {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                //TODO: add option to specify whether copy or not before delete
                EditorUtils.copyRange(_selectorClass, _te);
                EditorUtils.deleteRange(_selectorClass, _te);
            }
        };

        AppUtil.runWriteAction(runnable, _se);
    }
}