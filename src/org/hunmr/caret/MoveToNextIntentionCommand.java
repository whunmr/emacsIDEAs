package org.hunmr.caret;

import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.hunmr.common.CommandContext;

import static com.intellij.codeInsight.intention.impl.IntentionHintComponent.showIntentionHint;

public class MoveToNextIntentionCommand {
    public static final int NOT_FOUND = -1;
    private final Editor _editor;
    private final PsiFile _psiFile;
    private final CommandContext _cmdCtx;
    private final Project _project;

    public MoveToNextIntentionCommand(Editor editor, PsiFile psiFile, CommandContext cmdCtx) {
        _editor = editor;
        _psiFile = psiFile;
        _cmdCtx = cmdCtx;
        _project = _editor.getProject();
    }

    public void  moveCaretToNextIntention() {
        final int originalCaretOffset = _editor.getCaretModel().getOffset();

        int offset = findIntentionOffset();

        if (offset != NOT_FOUND) {
            _editor.getCaretModel().moveToOffset(offset);
            _editor.getScrollingModel().scrollToCaret(ScrollType.CENTER);
        } else {
            _editor.getCaretModel().moveToOffset(originalCaretOffset);
            HintManager.getInstance().showInformationHint(_editor, "No more quick fix intentions.");
        }
    }

    private int findIntentionOffset() {
        if (_cmdCtx.getPrefixCount() == 0) {
            return forwardFindIntentionOffset();
        } else {
            return backwardFindIntentionOffset();
        }
    }

    private int forwardFindIntentionOffset() {
        int startOffset = findElementEndOffset() + 1;
        int endOffset = _editor.getDocument().getTextLength();

        for (int offset = startOffset; offset < endOffset; ) {
            if (hasIntentionAtCaret(offset)) {
                return offset;
            }
            offset += getElementLengthAt(offset);
        }

        return NOT_FOUND;
    }

    private int backwardFindIntentionOffset() {
        int reverseStart = findPreviousElementStartOffset() - 1;

        for (int offset = reverseStart; offset > 0; ) {
            if (hasIntentionAtCaret(offset)) {
                return offset;
            }

            offset -= getElementLengthAt(offset);
        }

        return NOT_FOUND;
    }

    private int getElementLengthAt(int offset) {
        PsiElement elem = _psiFile.findElementAt(offset);

        if (elem != null) {
            return elem.getTextLength();
        } else {
            return 1;
        }
    }

    private int findElementEndOffset() {
        PsiElement elem = _psiFile.findElementAt(_editor.getCaretModel().getOffset());

        if (elem != null) {
            return elem.getTextRange().getEndOffset();
        } else {
            return _editor.getCaretModel().getOffset();
        }
    }

    private int findPreviousElementStartOffset() {
        PsiElement elem = _psiFile.findElementAt(_editor.getCaretModel().getOffset() - 1);

        if (elem != null) {
            return elem.getTextRange().getStartOffset();
        } else {
            return _editor.getCaretModel().getOffset();
        }
    }

    private boolean hasIntentionAtCaret(int offset) {
        _editor.getCaretModel().moveToOffset(offset);

        ShowIntentionsPass.IntentionsInfo intentions = new ShowIntentionsPass.IntentionsInfo();
        ShowIntentionsPass.getActionsToShow(_editor, _psiFile, intentions, -1);

        boolean hasIntention = !intentions.errorFixesToShow.isEmpty() || !intentions.inspectionFixesToShow.isEmpty();
        boolean shouldShowIntentionPopup = _cmdCtx.getLastCmdKey() == 'I';

        if (hasIntention && shouldShowIntentionPopup) {
            showIntentionHint(_project, _psiFile, _editor, intentions, true);
        }

        return hasIntention;
    }
}
