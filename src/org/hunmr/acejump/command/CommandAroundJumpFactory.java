package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class CommandAroundJumpFactory {

    public static boolean isCommandKey(char key) {
        return key == 'p' || key == 's' || key == 'c' || key == 'x';
    }

    public static CommandAroundJump createCommand(char key, Editor editor) {
        switch (key) {
            case 's' :
                return new SelectionCommand(editor);
            case 'x' :
                return new CutAfterJumpCommand(editor);
            case 'c' :
                return new CopyAfterJumpCommand(editor);
            case 'p' :
                return new PasteAfterJumpCommand(editor);
            default:
                ;
        }

        return null;
    }
}
