package org.hunmr.acejump;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.acejump.command.CommandAroundJump;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.argument.ArgumentCandidate;
import org.hunmr.argument.ArgumentListRangePlanner;
import org.hunmr.argument.ArgumentParser;
import org.hunmr.argument.ParsedArguments;
import org.hunmr.argument.TextSpan;
import org.hunmr.common.SimpleEditorAction;
import org.hunmr.common.selector.Selector;
import org.hunmr.util.EditorUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;

public abstract class AbstractAceJumpArgumentSliceRangeAction extends SimpleEditorAction {
    @Override
    public final void actionPerformed(AnActionEvent e) {
        Editor editor = getEditor(e);
        if (editor == null || editor.isDisposed()) {
            return;
        }

        AceJumpAction aceJumpAction = AceJumpAction.getInstance();
        aceJumpAction.switchEditorIfNeed(e);

        ArrayList<JOffset> offsets = collectVisibleSliceOffsets(aceJumpAction.getEditors());
        if (offsets.isEmpty()) {
            HintManager.getInstance().showInformationHint(editor, "No visible argument slices");
            return;
        }

        CommandAroundJump command = createCommand(editor, getSelectorClass());
        if (command == null) {
            return;
        }

        if (!beforeStartJump(editor, getSelectorClass())) {
            return;
        }

        aceJumpAction.addCommandAroundJump(command);
        aceJumpAction.startJump(e, offsets);
    }

    protected abstract boolean isBeforeSlice();

    protected abstract Class<? extends Selector> getSelectorClass();

    protected abstract CommandAroundJump createCommand(Editor editor, Class<? extends Selector> selectorClass);

    protected boolean beforeStartJump(Editor editor, Class<? extends Selector> selectorClass) {
        return true;
    }

    private ArrayList<JOffset> collectVisibleSliceOffsets(ArrayList<Editor> editors) {
        ArrayList<JOffset> offsets = new ArrayList<JOffset>();
        if (editors == null) {
            return offsets;
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
            LinkedHashSet<Integer> visibleAnchors = new LinkedHashSet<Integer>();
            for (ArgumentCandidate candidate : parsedArguments.getArguments()) {
                if (candidate == null) {
                    continue;
                }

                int anchorOffset = candidate.getAnchorOffset();
                if (anchorOffset < visibleRange.getStartOffset() || anchorOffset > visibleRange.getEndOffset()) {
                    continue;
                }

                TextSpan span = isBeforeSlice()
                        ? ArgumentListRangePlanner.planBefore(parsedArguments.getText(), anchorOffset)
                        : ArgumentListRangePlanner.planAfter(parsedArguments.getText(), anchorOffset);
                if (span == null || span.length() == 0) {
                    continue;
                }

                visibleAnchors.add(anchorOffset);
            }

            for (Integer anchorOffset : visibleAnchors) {
                offsets.add(new JOffset(editor, anchorOffset));
            }
        }

        return offsets;
    }
}
