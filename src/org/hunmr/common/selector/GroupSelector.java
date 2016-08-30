package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import org.hunmr.common.CommandContext;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GroupSelector extends BlockSelector {

    public GroupSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange(CommandContext cmdCtx) {
        TextRange r = super.getRange(cmdCtx);

        if (r != null) {
            int startLine = _document.getLineNumber(r.getStartOffset() - 1);
            int start = _document.getLineStartOffset(startLine);
            int end   = _document.getLineEndOffset(_document.getLineNumber(r.getEndOffset() + 1));

            if (end + 1 < _docText.length()) {
                end++; //extend to include the ending newline char
            }

            if (isBeginningOfBody(_document.getText(new TextRange(start, end)))) {
                //To include function-name line if { starts at newline.
                // void function-name
                // {
                //   ...
                // }
                start = _document.getLineStartOffset(startLine - 1);
            }

            return new TextRange(start, end);
        }

        return r;
    }

    private boolean isBeginningOfBody(String text) {
        Pattern p = Pattern.compile("^\\s*\\{.*");
        Matcher m = p.matcher(text);
        return m.find();
    }
}
