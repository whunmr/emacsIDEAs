package org.hunmr.copywithoutselection.selector;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;

public class BlockSelector extends Selector {
    public BlockSelector(Editor editor) {
        super(editor);
    }

    public TextRange getRange() {
        //  {'[<("



        return null;
    }
}
