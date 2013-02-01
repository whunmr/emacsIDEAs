package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class CommandAroundJumpFactory {

    public static final char SELECT_AFTER_JUMP = 's';
    public static final char COPY_AFTER_JUMP = 'c';
    public static final char CUT_AFTER_JUMP = 'x';
    public static final char CUT_AFTER_JUMP_WITH_SPACE_KEY = ' ';

    public static boolean isCommandKey(char key) {
        key = Character.toLowerCase(key);
        return key == CUT_AFTER_JUMP_WITH_SPACE_KEY
                || key == SELECT_AFTER_JUMP
                || key == CUT_AFTER_JUMP
                || key == COPY_AFTER_JUMP;
    }

    public static CommandAroundJump createCommand(char key, Editor editor) {
        switch (Character.toLowerCase(key)) {
            case SELECT_AFTER_JUMP :
                return new SelectionCommand(editor);
            case CUT_AFTER_JUMP_WITH_SPACE_KEY:
            case CUT_AFTER_JUMP:
                return new CutAfterJumpCommand(editor);
            case COPY_AFTER_JUMP:
                return new CopyAfterJumpCommand(editor);
            default:
                return null;
        }
    }
}
