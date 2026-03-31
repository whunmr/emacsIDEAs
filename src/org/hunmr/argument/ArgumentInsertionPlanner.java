package org.hunmr.argument;

public final class ArgumentInsertionPlanner {
    private ArgumentInsertionPlanner() {
    }

    public static ArgumentInsertionPlan plan(String text, int caretOffset, String argumentText) {
        String safeText = text == null ? "" : text;
        String safeArgumentText = argumentText == null ? "" : argumentText;
        int safeOffset = Math.max(0, Math.min(caretOffset, safeText.length()));

        ParsedArguments parsed = ArgumentParser.parse(safeText);
        ArgumentList list = parsed.findInnermostList(safeOffset);
        if (list == null || shouldFallbackToPlainInsert(parsed, safeText, safeOffset)) {
            return plainInsert(safeOffset, safeArgumentText);
        }

        int prevIndex = findPreviousNonWhitespace(safeText, safeOffset - 1, list.getOpenOffset());
        int nextIndex = findNextNonWhitespace(safeText, safeOffset, list.getCloseOffset());

        boolean hasBefore = prevIndex > list.getOpenOffset();
        boolean hasAfter = nextIndex >= 0 && nextIndex < list.getCloseOffset();
        char prevChar = hasBefore ? safeText.charAt(prevIndex) : '(';
        char nextChar = hasAfter ? safeText.charAt(nextIndex) : ')';

        int replaceStart = safeOffset;
        int replaceEnd = safeOffset;
        String prefix = "";
        String suffix = "";

        if (!hasBefore && !hasAfter) {
            return createPlan(replaceStart, replaceEnd, prefix, safeArgumentText, suffix);
        }

        if (!hasBefore) {
            replaceEnd = consumeWhitespaceForward(safeText, safeOffset, nextIndex);
            suffix = needsTrailingComma(nextChar) ? ", " : "";
            return createPlan(replaceStart, replaceEnd, prefix, safeArgumentText, suffix);
        }

        if (!hasAfter) {
            replaceStart = consumeWhitespaceBackward(safeText, safeOffset, prevIndex);
            prefix = needsLeadingComma(prevChar) ? ", " : "";
            return createPlan(replaceStart, replaceEnd, prefix, safeArgumentText, suffix);
        }

        if (prevChar == ',' && nextChar != ',') {
            replaceEnd = consumeWhitespaceForward(safeText, safeOffset, nextIndex);
            suffix = needsTrailingComma(nextChar) ? ", " : "";
            return createPlan(replaceStart, replaceEnd, prefix, safeArgumentText, suffix);
        }

        if (nextChar == ',' && prevChar != ',') {
            replaceStart = consumeWhitespaceBackward(safeText, safeOffset, prevIndex);
            prefix = needsLeadingComma(prevChar) ? ", " : "";
            return createPlan(replaceStart, replaceEnd, prefix, safeArgumentText, suffix);
        }

        if (prevChar == ',' && nextChar == ',') {
            replaceStart = consumeWhitespaceBackward(safeText, safeOffset, prevIndex);
            replaceEnd = consumeWhitespaceForward(safeText, safeOffset, nextIndex);
            return createPlan(replaceStart, replaceEnd, prefix, safeArgumentText, suffix);
        }

        return plainInsert(safeOffset, safeArgumentText);
    }

    private static boolean shouldFallbackToPlainInsert(ParsedArguments parsed, String text, int caretOffset) {
        ArgumentCandidate candidate = parsed.findArgumentAtOrNear(caretOffset);
        if (candidate == null || candidate.getRange() == null) {
            return false;
        }

        int start = candidate.getRange().getStartOffset();
        int end = candidate.getRange().getEndOffset();
        if (caretOffset <= start || caretOffset >= end || caretOffset <= 0 || caretOffset >= text.length()) {
            return false;
        }

        char left = text.charAt(caretOffset - 1);
        char right = text.charAt(caretOffset);
        return isTokenChar(left) && isTokenChar(right);
    }

    private static boolean isTokenChar(char c) {
        return !Character.isWhitespace(c) && ",()[]{}".indexOf(c) < 0;
    }

    private static int findPreviousNonWhitespace(String text, int index, int lowerBoundExclusive) {
        for (int i = index; i > lowerBoundExclusive; i--) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int findNextNonWhitespace(String text, int index, int upperBoundExclusive) {
        for (int i = Math.max(0, index); i < upperBoundExclusive; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int consumeWhitespaceForward(String text, int start, int nextNonWhitespaceIndex) {
        if (nextNonWhitespaceIndex < 0) {
            return start;
        }

        for (int i = start; i < nextNonWhitespaceIndex; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return start;
            }
        }

        return nextNonWhitespaceIndex;
    }

    private static int consumeWhitespaceBackward(String text, int end, int previousNonWhitespaceIndex) {
        if (previousNonWhitespaceIndex < 0) {
            return end;
        }

        for (int i = previousNonWhitespaceIndex + 1; i < end; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return end;
            }
        }

        return previousNonWhitespaceIndex + 1;
    }

    private static boolean needsLeadingComma(char prevChar) {
        return prevChar != '(' && prevChar != ',';
    }

    private static boolean needsTrailingComma(char nextChar) {
        return nextChar != ')' && nextChar != ',';
    }

    private static ArgumentInsertionPlan plainInsert(int offset, String text) {
        return createPlan(offset, offset, "", text, "");
    }

    private static ArgumentInsertionPlan createPlan(int replaceStart, int replaceEnd, String prefix, String argumentText, String suffix) {
        String insertedText = prefix + argumentText + suffix;
        int selectionStart = replaceStart + prefix.length();
        int selectionEnd = selectionStart + argumentText.length();
        int caretOffset = replaceStart + insertedText.length();
        return new ArgumentInsertionPlan(replaceStart, replaceEnd, insertedText, selectionStart, selectionEnd, caretOffset);
    }
}
