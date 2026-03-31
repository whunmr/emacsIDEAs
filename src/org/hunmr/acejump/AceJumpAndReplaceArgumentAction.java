package org.hunmr.acejump;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.ReplaceArgumentAfterJumpCommand;
import org.hunmr.argument.ArgumentCandidate;

import java.util.IdentityHashMap;
import java.util.Map;

public class AceJumpAndReplaceArgumentAction extends AbstractAceJumpArgumentAction {
    @Override
    protected CommandAroundJump createCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        return new ReplaceArgumentAfterJumpCommand(editor, targets);
    }
}
