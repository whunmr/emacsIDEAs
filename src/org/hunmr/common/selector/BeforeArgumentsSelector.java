package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.argument.ArgumentListRangePlanner;
import org.hunmr.argument.TextSpan;
import org.hunmr.common.CommandContext;

public class BeforeArgumentsSelector extends Selector {
    public BeforeArgumentsSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange(CommandContext cmdCtx) {
        TextSpan span = ArgumentListRangePlanner.planBefore(_docText, _editor.getCaretModel().getOffset());
        if (span == null || span.length() == 0) {
            return null;
        }

        return new TextRange(span.getStartOffset(), span.getEndOffset());
    }
}
