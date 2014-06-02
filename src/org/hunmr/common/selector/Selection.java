package org.hunmr.common.selector;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

public class Selection {
    public static TextRange getTextRangeBy(Editor editor, CommandContext cmdCtx) {
        char key = cmdCtx.getLastCmdKey();
        Selector selector = SelectorFactory.createSelectorBy(key, editor);

        if (selector == null) {
            HintManager.getInstance().showInformationHint(editor, SelectorFactory.HELP_MSG);
            return null;
        }

        TextRange tr = selector.getRange(cmdCtx);
        if (tr == null) {
            HintManager.getInstance().showInformationHint(editor, "404");
        }

        return tr;
    }
}
