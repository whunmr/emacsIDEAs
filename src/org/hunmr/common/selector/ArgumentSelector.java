package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentParser;
import org.hunmr.argument.ParsedArguments;
import org.hunmr.common.CommandContext;

public class ArgumentSelector extends Selector {
    public ArgumentSelector(Editor editor) {
        super(editor);
    }

    @Override
    public TextRange getRange(CommandContext cmdCtx) {
        ParsedArguments parsedArguments = ArgumentParser.parse(_docText);
        ArgumentCandidate candidate = parsedArguments.findArgumentAtOrNear(_editor.getCaretModel().getOffset());
        if (candidate == null || candidate.getRange() == null) {
            return null;
        }

        return new TextRange(candidate.getRange().getStartOffset(), candidate.getRange().getEndOffset());
    }
}
