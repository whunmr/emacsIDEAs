package org.hunmr.acejump;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentParser;
import org.hunmr.argument.ParsedArguments;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.util.EditorUtils;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public abstract class AbstractAceJumpArgumentAction extends SimpleEditorAction {
    @Override
    public final void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        if (editor == null || editor.isDisposed()) {
            return;
        }

        AceJumpAction aceJumpAction = AceJumpAction.getInstance();
        aceJumpAction.switchEditorIfNeed(e);

        IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets = collectVisibleArguments(aceJumpAction.getEditors());
        ArrayList<JOffset> offsets = collectOffsets(targets);
        if (offsets.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "No visible arguments");
            return;
        }

        CommandAroundJump command = createCommand(editor, targets);
        if (command == null) {
            return;
        }

        aceJumpAction.addCommandAroundJump(command);
        aceJumpAction.startJump(e, offsets);
    }

    protected abstract CommandAroundJump createCommand(Editor editor, IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets);

    private IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> collectVisibleArguments(ArrayList<Editor> editors) {
        IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets = new IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>>();
        if (editors == null) {
            return targets;
        }

        for (Editor editor : editors) {
            if (editor == null || editor.isDisposed()) {
                continue;
            }

            TextRange visibleRange = EditorUtils.getVisibleTextRange(editor);
            if (visibleRange == null || visibleRange.isEmpty()) {
                continue;
            }

            ParsedArguments parsedArguments = ArgumentParser.parse(editor.getDocument().getText());
            LinkedHashMap<Integer, ArgumentCandidate> byAnchor = new LinkedHashMap<Integer, ArgumentCandidate>();
            for (ArgumentCandidate candidate : parsedArguments.getArguments()) {
                if (candidate == null) {
                    continue;
                }

                int anchorOffset = candidate.getAnchorOffset();
                if (anchorOffset < visibleRange.getStartOffset() || anchorOffset > visibleRange.getEndOffset()) {
                    continue;
                }

                if (!byAnchor.containsKey(anchorOffset)) {
                    byAnchor.put(anchorOffset, candidate);
                }
            }

            if (!byAnchor.isEmpty()) {
                targets.put(editor, byAnchor);
            }
        }

        return targets;
    }

    private ArrayList<JOffset> collectOffsets(IdentityHashMap<Editor, Map<Integer, ArgumentCandidate>> targets) {
        ArrayList<JOffset> offsets = new ArrayList<JOffset>();
        for (Map.Entry<Editor, Map<Integer, ArgumentCandidate>> entry : targets.entrySet()) {
            Editor editor = entry.getKey();
            if (editor == null || editor.isDisposed()) {
                continue;
            }

            for (Integer anchorOffset : entry.getValue().keySet()) {
                offsets.add(new JOffset(editor, anchorOffset));
            }
        }

        return offsets;
    }
}
