package org.hunmr.argument;

import org.hunmr.common.selector.AfterArgumentsSelector;
import org.hunmr.common.selector.ArgumentSelector;
import org.hunmr.common.selector.BeforeArgumentsSelector;
import org.hunmr.common.selector.Selector;

public final class ArgumentSelectorSupport {
    private ArgumentSelectorSupport() {
    }

    public static boolean isArgumentSelector(Class<? extends Selector> selectorClass) {
        return selectorClass == ArgumentSelector.class
                || selectorClass == BeforeArgumentsSelector.class
                || selectorClass == AfterArgumentsSelector.class;
    }

    public static TextSpan getTextSpan(Class<? extends Selector> selectorClass, String text, int caretOffset) {
        if (!isArgumentSelector(selectorClass)) {
            return null;
        }

        if (selectorClass == ArgumentSelector.class) {
            ParsedArguments parsedArguments = ArgumentParser.parse(text);
            ArgumentCandidate candidate = parsedArguments.findArgumentAtOrNear(caretOffset);
            return candidate == null ? null : candidate.getRange();
        }

        if (selectorClass == BeforeArgumentsSelector.class) {
            return ArgumentListRangePlanner.planBefore(text, caretOffset);
        }

        return ArgumentListRangePlanner.planAfter(text, caretOffset);
    }

    public static String getSelectedText(Class<? extends Selector> selectorClass, String text, int caretOffset) {
        TextSpan span = getTextSpan(selectorClass, text, caretOffset);
        if (span == null || text == null) {
            return "";
        }

        int startOffset = Math.max(0, Math.min(span.getStartOffset(), text.length()));
        int endOffset = Math.max(startOffset, Math.min(span.getEndOffset(), text.length()));
        return text.substring(startOffset, endOffset);
    }
}
