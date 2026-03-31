package org.hunmr.argument;

import java.util.List;

public final class ArgumentEditPlanner {
    private ArgumentEditPlanner() {
    }

    public static ArgumentInsertionPlan planDelete(String text, ArgumentCandidate candidate) {
        if (candidate == null || candidate.getRange() == null) {
            return null;
        }

        ParsedArguments parsed = ArgumentParser.parse(text);
        ArgumentList list = findMatchingList(parsed, candidate);
        if (list == null) {
            return createPlan(candidate.getRange().getStartOffset(), candidate.getRange().getEndOffset(), "");
        }

        List<ArgumentCandidate> arguments = list.getArguments();
        int index = candidate.getIndexInList();
        if (index < 0 || index >= arguments.size()) {
            return createPlan(candidate.getRange().getStartOffset(), candidate.getRange().getEndOffset(), "");
        }

        if (arguments.size() == 1) {
            return createPlan(candidate.getRange().getStartOffset(), candidate.getRange().getEndOffset(), "");
        }

        if (index < arguments.size() - 1) {
            ArgumentCandidate next = arguments.get(index + 1);
            return createPlan(candidate.getRange().getStartOffset(), next.getRange().getStartOffset(), "");
        }

        ArgumentCandidate previous = arguments.get(index - 1);
        return createPlan(previous.getRange().getEndOffset(), candidate.getRange().getEndOffset(), "");
    }

    public static ArgumentInsertionPlan planReplace(ArgumentCandidate candidate, String replacementText) {
        if (candidate == null || candidate.getRange() == null) {
            return null;
        }

        TextSpan range = candidate.getRange();
        String safeReplacementText = replacementText == null ? "" : replacementText;
        int selectionStart = range.getStartOffset();
        int selectionEnd = selectionStart + safeReplacementText.length();
        int caretOffset = selectionEnd;
        return new ArgumentInsertionPlan(range.getStartOffset(), range.getEndOffset(), safeReplacementText, selectionStart, selectionEnd, caretOffset);
    }

    public static String apply(String text, ArgumentInsertionPlan plan) {
        if (plan == null) {
            return text == null ? "" : text;
        }

        String safeText = text == null ? "" : text;
        StringBuilder builder = new StringBuilder(safeText);
        builder.replace(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());
        return builder.toString();
    }

    public static int adjustOffsetAfterPlan(int offset, ArgumentInsertionPlan plan) {
        if (plan == null) {
            return offset;
        }

        if (offset <= plan.getReplaceStartOffset()) {
            return offset;
        }

        if (offset >= plan.getReplaceEndOffset()) {
            return offset + plan.getInsertedText().length() - (plan.getReplaceEndOffset() - plan.getReplaceStartOffset());
        }

        return plan.getReplaceStartOffset() + plan.getInsertedText().length();
    }

    public static boolean sameArgument(ArgumentCandidate left, ArgumentCandidate right) {
        if (left == null || right == null || left.getRange() == null || right.getRange() == null) {
            return false;
        }

        return left.getRange().getStartOffset() == right.getRange().getStartOffset()
                && left.getRange().getEndOffset() == right.getRange().getEndOffset()
                && left.getListOpenOffset() == right.getListOpenOffset()
                && left.getListCloseOffset() == right.getListCloseOffset();
    }

    private static ArgumentList findMatchingList(ParsedArguments parsed, ArgumentCandidate candidate) {
        if (parsed == null || candidate == null) {
            return null;
        }

        for (ArgumentList list : parsed.getLists()) {
            if (list == null) {
                continue;
            }

            if (list.getOpenOffset() == candidate.getListOpenOffset()
                    && list.getCloseOffset() == candidate.getListCloseOffset()) {
                return list;
            }
        }

        return null;
    }

    private static ArgumentInsertionPlan createPlan(int replaceStart, int replaceEnd, String insertedText) {
        return new ArgumentInsertionPlan(replaceStart, replaceEnd, insertedText, replaceStart, replaceStart, replaceStart + insertedText.length());
    }
}
