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
        if (list == null) {
            return plainInsert(safeOffset, safeArgumentText);
        }

        safeOffset = normalizeOffsetInsideCurrentArgument(parsed, list, safeOffset);

        ArgumentInsertionPlan multilinePlan = planMultilineInsert(parsed, safeText, list, safeOffset, safeArgumentText);
        if (multilinePlan != null) {
            return multilinePlan;
        }

        int prevIndex = ArgumentListFormatting.findPreviousNonWhitespace(safeText, safeOffset - 1, list.getOpenOffset());
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

    private static ArgumentInsertionPlan planMultilineInsert(ParsedArguments parsed,
                                                             String text,
                                                             ArgumentList list,
                                                             int offset,
                                                             String argumentText) {
        if (!ArgumentListFormatting.isMultilineList(text, list)) {
            return null;
        }

        String lineSeparator = ArgumentListFormatting.detectLineSeparator(text);
        boolean trailingComma = ArgumentListFormatting.hasTrailingComma(text, list);

        ArgumentCandidate beforeCandidate = ArgumentListFormatting.findCandidateStartingAt(list, offset);
        if (beforeCandidate != null) {
            String indent = ArgumentListFormatting.getLineIndent(text, beforeCandidate.getRange().getStartOffset());
            String formattedArgument = ArgumentListFormatting.reindentArgumentText(argumentText, indent, lineSeparator);
            return createPlan(offset, offset, "", formattedArgument, "," + lineSeparator + indent);
        }

        ArgumentCandidate afterCandidate = ArgumentListFormatting.findCandidateEndingAt(list, offset);
        if (afterCandidate != null) {
            ArgumentCandidate nextCandidate = ArgumentListFormatting.findNextCandidate(list, afterCandidate);
            String indent = nextCandidate != null
                    ? ArgumentListFormatting.getLineIndent(text, nextCandidate.getRange().getStartOffset())
                    : ArgumentListFormatting.getLineIndent(text, afterCandidate.getRange().getStartOffset());
            String formattedArgument = ArgumentListFormatting.reindentArgumentText(argumentText, indent, lineSeparator);

            if (nextCandidate != null) {
                String nextIndent = ArgumentListFormatting.getLineIndent(text, nextCandidate.getRange().getStartOffset());
                return createPlan(offset, nextCandidate.getRange().getStartOffset(),
                        "," + lineSeparator + indent,
                        formattedArgument,
                        "," + lineSeparator + nextIndent);
            }

            String closeIndent = ArgumentListFormatting.getLineIndent(text, list.getCloseOffset());
            return createPlan(offset, list.getCloseOffset(),
                    "," + lineSeparator + indent,
                    formattedArgument,
                    (trailingComma ? "," : "") + lineSeparator + closeIndent);
        }

        ArgumentCandidate nextCandidate = ArgumentListFormatting.findCandidateStartingAtOrAfter(list, offset);
        if (nextCandidate != null) {
            String indent = ArgumentListFormatting.getLineIndent(text, nextCandidate.getRange().getStartOffset());
            String formattedArgument = ArgumentListFormatting.reindentArgumentText(argumentText, indent, lineSeparator);
            return createPlan(offset, nextCandidate.getRange().getStartOffset(),
                    "",
                    formattedArgument,
                    "," + lineSeparator + indent);
        }

        ArgumentCandidate previousCandidate = ArgumentListFormatting.findCandidateEndingAtOrBefore(list, offset);
        if (previousCandidate != null) {
            String indent = ArgumentListFormatting.getLineIndent(text, previousCandidate.getRange().getStartOffset());
            String formattedArgument = ArgumentListFormatting.reindentArgumentText(argumentText, indent, lineSeparator);
            String closeIndent = ArgumentListFormatting.getLineIndent(text, list.getCloseOffset());
            return createPlan(previousCandidate.getRange().getEndOffset(), list.getCloseOffset(),
                    "," + lineSeparator + indent,
                    formattedArgument,
                    (trailingComma ? "," : "") + lineSeparator + closeIndent);
        }

        if (list.getArguments().isEmpty()) {
            String closeIndent = ArgumentListFormatting.getLineIndent(text, list.getCloseOffset());
            String argumentIndent = closeIndent + ArgumentListFormatting.detectIndentUnit(text, list);
            String formattedArgument = ArgumentListFormatting.reindentArgumentText(argumentText, argumentIndent, lineSeparator);
            return createPlan(offset, offset,
                    lineSeparator + argumentIndent,
                    formattedArgument,
                    (trailingComma ? "," : "") + lineSeparator + closeIndent);
        }

        return null;
    }

    private static int normalizeOffsetInsideCurrentArgument(ParsedArguments parsed, ArgumentList list, int caretOffset) {
        ArgumentCandidate candidate = parsed.findArgumentAtOrNear(caretOffset);
        if (candidate == null || candidate.getRange() == null) {
            return caretOffset;
        }

        int start = candidate.getRange().getStartOffset();
        int end = candidate.getRange().getEndOffset();
        if (candidate.getListOpenOffset() != list.getOpenOffset() || candidate.getListCloseOffset() != list.getCloseOffset()) {
            return caretOffset;
        }

        if (caretOffset >= start && caretOffset <= candidate.getAnchorOffset()) {
            return start;
        }

        if (caretOffset > candidate.getAnchorOffset() && caretOffset < end) {
            return end;
        }

        return caretOffset;
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
