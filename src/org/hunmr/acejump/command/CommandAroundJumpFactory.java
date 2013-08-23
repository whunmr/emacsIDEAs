package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class CommandAroundJumpFactory {

    public static final char SELECT_AFTER_JUMP = 's';
    public static final char COPY_AFTER_JUMP = 'c';
    public static final char CUT_AFTER_JUMP = 'x';
    public static final char CUT_AFTER_JUMP_WITH_SPACE_KEY = ' ';
    public static final char PASTE_AFTER_JUMP = 'p';
    public static final char PASTE_WITH_NEWLINE_AFTER_JUMP = 'P';
    public static final char REPLACE_WORD_AFTER_JUMP = 'w';
    public static final char REPLACE_LINE_AFTER_JUMP = 'l';
    public static final char REPLACE_PARAGRAPH_AFTER_JUMP = '&';
    public static final char REPLACE_TO_LINE_END_AFTER_JUMP = 'e';
    public static final char REPLACE_BLOCK_AFTER_JUMP = 'b';

    public static boolean isCommandKey(char key) {
        key = Character.toLowerCase(key);
        return key == CUT_AFTER_JUMP_WITH_SPACE_KEY
                || key == SELECT_AFTER_JUMP
                || key == CUT_AFTER_JUMP
                || key == COPY_AFTER_JUMP
                || key == PASTE_AFTER_JUMP;
    }

    public static CommandAroundJump createCommand(char key, Editor editor) {
        switch (key) {
            case PASTE_AFTER_JUMP:
                return new PasteAfterJumpCommand(editor, false);
            case PASTE_WITH_NEWLINE_AFTER_JUMP:
                return new PasteAfterJumpCommand(editor, true);
            default:
                return getCommandsIgnoreCase(key, editor);
        }

    }

    private static CommandAroundJump getCommandsIgnoreCase(char key, Editor editor) {
        switch (Character.toLowerCase(key)) {
            case SELECT_AFTER_JUMP :
                return new SelectionCommand(editor);
            case CUT_AFTER_JUMP_WITH_SPACE_KEY:
            case CUT_AFTER_JUMP:
                return new CutAfterJumpCommand(editor);
            case COPY_AFTER_JUMP:
                return new CopyAfterJumpCommand(editor);
            case REPLACE_WORD_AFTER_JUMP:
                return new ReplaceWordAfterJumpCommand(editor);
            case REPLACE_LINE_AFTER_JUMP:
                return new ReplaceLineAfterJumpCommand(editor);
            case REPLACE_PARAGRAPH_AFTER_JUMP:
                return new ReplaceParagraphAfterJumpCommand(editor);
            case REPLACE_TO_LINE_END_AFTER_JUMP:
                return new ReplaceToLineEndAfterJumpCommand(editor);
            case REPLACE_BLOCK_AFTER_JUMP:
                return new ReplaceBlockAfterJumpCommand(editor);
            default:
                return null;
        }
    }
}
