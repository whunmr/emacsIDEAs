package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;
import org.hunmr.argument.ArgumentCandidate;

import java.util.IdentityHashMap;
import java.util.Map;

public abstract class ArgumentJumpCommand extends CommandAroundJump {
    protected final IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> _targets;

    protected ArgumentJumpCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        super(editor);
        _targets = targets;
    }

    protected ArgumentCandidate getTargetCandidate() {
        if (_targets == null || !hasUsableTargetEditor()) {
            return null;
        }

        Map<Integer, ArgumentCandidate> candidatesByAnchor = _targets.get(_te);
        if (candidatesByAnchor == null) {
            return null;
        }

        return candidatesByAnchor.get(_toff);
    }
}
