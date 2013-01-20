package org.hunmr.acejump.command;

import com.intellij.openapi.editor.Editor;

public class CommandAroundJumpFactory {

    public static boolean isCommandKey(char key) {
        return key == '.' || key == ';' || key == ',' || key == ' ';
    }

    public static CommandAroundJump createCommand(char key, Editor editor) {
        switch (key) {
            case ';' :
                return new SelectionCommand(editor);
            case ' ' :
                return new CutAfterJumpCommand(editor);
            case ',' :
                return new CopyAfterJumpCommand(editor);
            case '.' :
                return new PasteAfterJumpCommand(editor);
            default:
                ;
        }

        return null;
    }
}
