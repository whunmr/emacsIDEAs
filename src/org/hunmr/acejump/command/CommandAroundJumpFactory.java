package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class CommandAroundJumpFactory {
    public static final char SELECT_AFTER_JUMP = 's';
    public static final char COPY_AFTER_JUMP = 'c';
    public static final char CUT_AFTER_JUMP = 'x';
    public static final char CUT_AFTER_JUMP_WITH_SPACE_KEY = ' ';

    public static final char REPLACE_WORD_AFTER_JUMP = 'w';
    public static final char REPLACE_LINE_AFTER_JUMP = 'l';
    public static final char REPLACE_PARAGRAPH_AFTER_JUMP = '&';
    public static final char REPLACE_TO_LINE_END_AFTER_JUMP = 'e';
    public static final char REPLACE_BLOCK_AFTER_JUMP = 'b';
    //TODO a, u and d

//    //todo remove
//    C-L 't' c 'm' : Copy jump area
//    C-L 't' x 'm' : Cut jump area
//    C-L 't' p 'm' : Paste clipborad content to target place
//    C-L 't' P 'm' : Insert '\n' and Paste clipborad content to target place
//    C-L 't' s 'm' : Select jump area

    public static final char OBTAIN_REMOTE_WORD = '0';
    public static final char OBTAIN_REMOTE_LINE = '1';
    public static final char OBTAIN_REMOTE_PARAGRAPH = '2';
    public static final char OBTAIN_REMOTE_TO_LINE_END = '3';
    public static final char OBTAIN_REMOTE_BLOCK = '3';

    public static CommandAroundJump createCommand(char key, Editor editor) {
        switch (key) {
            default:
                return getCommandsIgnoreCase(key, editor);
        }
    }

    private static CommandAroundJump getCommandsIgnoreCase(char key, Editor editor) {
        switch (Character.toLowerCase(key)) {
            case SELECT_AFTER_JUMP :
                return new SelectAfterJumpCommand(editor);
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
