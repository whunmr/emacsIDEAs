package org.hunmr.acejump;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.command.MoveArgumentAfterJumpCommand;
import org.hunmr.argument.ArgumentCandidate;

import java.util.IdentityHashMap;
import java.util.Map;

public class AceJumpMoveArgumentAction extends AbstractAceJumpArgumentAction {
    @Override
    protected CommandAroundJump createCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        return new MoveArgumentAfterJumpCommand(editor, targets);
    }
}
