package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class CommandAroundJumpFactory {

    public static boolean isCommandKey(char key) {
        return key == ' ' || key == 's' || key == 'x' || key == 'c' || key == 'p' || key == 'P';
    }

    public static CommandAroundJump createCommand(char key, Editor editor) {
        switch (key) {
            case 's' :
                return new SelectionCommand(editor);
            case ' ' :
            case 'x' :
                return new CutAfterJumpCommand(editor);
            case 'c' :
                return new CopyAfterJumpCommand(editor);
            case 'p' :
                return new PasteAfterJumpCommand(editor, false);
            case 'P' :
                return new PasteAfterJumpCommand(editor, true);
            default:
                ;
        }

        return null;
    }
}
