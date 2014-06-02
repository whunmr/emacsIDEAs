package org.hunmr.common.selector;

import com.intellij.openapi.editor.Editor;

import java.awt.*;

public class SelectorFactory {
    public static final String HELP_MSG = "C-c w : Copy word\n" +
            "C-c s : Copy String\n" +
            "C-c l : Copy Line\n" +
            "C-c q : Copy Quoted\n" +
            "C-c a : Copy Quoted\n" +
            "C-c p : Copy Paragraph\n";
    public static final Color HIGHLIGHT_COLOR = new Color(122, 214, 162);

    public static boolean isSelectorKey(char c) {
        return "wslpbqaeud".indexOf(Character.toLowerCase(c)) != -1;
    }

    public static Selector createSelectorBy(char key, Editor editor) {
        Selector selector = null;

        switch (Character.toLowerCase(key)) {
            case 'w':
                selector = new WordSelector(editor);
                break;
            case 's':
                selector = new StringSelector(editor);
                break;
            case 'l':
                selector = new LineSelector(editor);
                break;
            case 'b':
                selector = new BlockSelector(editor);
                break;
            case 'q':
                selector = new QuoteSelector(editor);
                break;
            case 'a':
                selector = new ToLineStartSelector(editor);
                break;
            case 'e':
                selector = new ToLineEndSelector(editor);
                break;
            case 'p':
                selector = new ParagraphSelector(editor);
                break;
            case 'u':
                selector = new ToParagraphStartSelector(editor);
                break;
            case 'd':
                selector = new ToParagraphEndSelector(editor);
                break;
            default:
                break;
        }
        return selector;
    }
}
