package org.hunmr.argument;

import java.util.List;

public final class ArgumentEditPlanner {
    private ArgumentEditPlanner() {
    }

    public static ArgumentInsertionPlan planDelete(String text, ArgumentCandidate candidate) {
        if (candidate == null || candidate.getRange() == null) {
            return null;
        }

        String safeText = text == null ? "" : text;
        ParsedArguments parsed = ArgumentParser.parse(safeText);
        ArgumentList list = findMatchingList(parsed, candidate);
        if (list == null) {
            return createPlan(candidate.getRange().getStartOffset(), candidate.getRange().getEndOffset(), "");
        }

        List<ArgumentCandidate> arguments = list.getArguments();
        int index = candidate.getIndexInList();
        if (index < 0 || index >= arguments.size()) {
            return createPlan(candidate.getRange().getStartOffset(), candidate.getRange().getEndOffset(), "");
        }

        if (ArgumentListFormatting.isMultilineList(safeText, list)) {
            ArgumentInsertionPlan multilinePlan = planMultilineDelete(safeText, list, index, index);
            if (multilinePlan != null) {
                return multilinePlan;
            }
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

    public static ArgumentInsertionPlan planDelete(String text, TextSpan span) {
        if (span == null || span.length() == 0) {
            return null;
        }

        String safeText = text == null ? "" : text;
        ParsedArguments parsed = ArgumentParser.parse(safeText);
        ArgumentSlice slice = findMatchingSlice(parsed, span);
        if (slice == null) {
            return createPlan(span.getStartOffset(), span.getEndOffset(), "");
        }

        if (slice.getFirstIndex() == slice.getLastIndex()) {
            return planDelete(safeText, slice.getFirstCandidate());
        }

        if (ArgumentListFormatting.isMultilineList(safeText, slice.getList())) {
            ArgumentInsertionPlan multilinePlan = planMultilineDelete(
                    safeText,
                    slice.getList(),
                    slice.getFirstIndex(),
                    slice.getLastIndex()
            );
            if (multilinePlan != null) {
                return multilinePlan;
            }
        }

        return planSingleLineDelete(slice);
    }

    public static ArgumentInsertionPlan planReplace(String text, ArgumentCandidate candidate, String replacementText) {
        if (candidate == null || candidate.getRange() == null) {
            return null;
        }

        String safeText = text == null ? "" : text;
        TextSpan range = candidate.getRange();
        String safeReplacementText = replacementText == null ? "" : replacementText;
        ParsedArguments parsed = ArgumentParser.parse(safeText);
        ArgumentList list = findMatchingList(parsed, candidate);
        if (list != null && ArgumentListFormatting.isMultilineList(safeText, list)) {
            String lineSeparator = ArgumentListFormatting.detectLineSeparator(safeText);
            String indent = ArgumentListFormatting.getLineIndent(safeText, range.getStartOffset());
            safeReplacementText = ArgumentListFormatting.reindentArgumentText(safeReplacementText, indent, lineSeparator);
        }

        int selectionStart = range.getStartOffset();
        int selectionEnd = selectionStart + safeReplacementText.length();
        int caretOffset = selectionEnd;
        return new ArgumentInsertionPlan(range.getStartOffset(), range.getEndOffset(), safeReplacementText, selectionStart, selectionEnd, caretOffset);
    }

    public static ArgumentInsertionPlan planReplace(ArgumentCandidate candidate, String replacementText) {
        return planReplace("", candidate, replacementText);
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

    private static ArgumentInsertionPlan planSingleLineDelete(ArgumentSlice slice) {
        ArgumentList list = slice.getList();
        List<ArgumentCandidate> arguments = list.getArguments();

        if (slice.getFirstIndex() == 0 && slice.getLastIndex() == arguments.size() - 1) {
            return createPlan(
                    slice.getFirstCandidate().getRange().getStartOffset(),
                    slice.getLastCandidate().getRange().getEndOffset(),
                    ""
            );
        }

        if (slice.getLastIndex() < arguments.size() - 1) {
            ArgumentCandidate next = arguments.get(slice.getLastIndex() + 1);
            return createPlan(
                    slice.getFirstCandidate().getRange().getStartOffset(),
                    next.getRange().getStartOffset(),
                    ""
            );
        }

        ArgumentCandidate previous = arguments.get(slice.getFirstIndex() - 1);
        return createPlan(
                previous.getRange().getEndOffset(),
                slice.getLastCandidate().getRange().getEndOffset(),
                ""
        );
    }

    private static ArgumentInsertionPlan planMultilineDelete(String text,
                                                             ArgumentList list,
                                                             int firstIndex,
                                                             int lastIndex) {
        List<ArgumentCandidate> arguments = list.getArguments();
        int closeLineStart = ArgumentListFormatting.findLineStart(text, list.getCloseOffset());
        int closeBoundary = closeLineStart < list.getCloseOffset() ? closeLineStart : list.getCloseOffset();
        ArgumentCandidate firstCandidate = arguments.get(firstIndex);
        ArgumentCandidate lastCandidate = arguments.get(lastIndex);

        if (firstIndex == 0 && lastIndex == arguments.size() - 1) {
            int replaceStart = ArgumentListFormatting.findLineStart(text, firstCandidate.getRange().getStartOffset());
            if (closeBoundary <= replaceStart) {
                return null;
            }
            return createPlan(replaceStart, closeBoundary, "");
        }

        if (lastIndex < arguments.size() - 1) {
            int replaceStart = ArgumentListFormatting.findLineStart(text, firstCandidate.getRange().getStartOffset());
            int replaceEnd = ArgumentListFormatting.findLineStart(text, arguments.get(lastIndex + 1).getRange().getStartOffset());
            if (replaceEnd <= replaceStart) {
                return null;
            }
            return createPlan(replaceStart, replaceEnd, "");
        }

        if (firstIndex > 0) {
            ArgumentCandidate previous = arguments.get(firstIndex - 1);
            boolean trailingComma = ArgumentListFormatting.hasTrailingComma(text, list);
            int replaceStart = trailingComma
                    ? ArgumentListFormatting.findLineStart(text, firstCandidate.getRange().getStartOffset())
                    : previous.getRange().getEndOffset();
            if (closeBoundary <= replaceStart) {
                return null;
            }
            if (!trailingComma && containsLineBreak(text, replaceStart, list.getCloseOffset())) {
                String insertedText = ArgumentListFormatting.detectLineSeparator(text)
                        + ArgumentListFormatting.getLineIndent(text, list.getCloseOffset());
                return createPlan(replaceStart, closeBoundary, insertedText);
            }
            return createPlan(replaceStart, closeBoundary, "");
        }

        return null;
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

    private static ArgumentSlice findMatchingSlice(ParsedArguments parsed, TextSpan span) {
        if (parsed == null || span == null) {
            return null;
        }

        ArgumentSlice best = null;
        for (ArgumentList list : parsed.getLists()) {
            if (list == null || list.getArguments().isEmpty()) {
                continue;
            }

            List<ArgumentCandidate> arguments = list.getArguments();
            int firstIndex = -1;
            int lastIndex = -1;
            for (int i = 0; i < arguments.size(); i++) {
                ArgumentCandidate candidate = arguments.get(i);
                if (candidate == null || candidate.getRange() == null) {
                    continue;
                }

                if (candidate.getRange().getStartOffset() == span.getStartOffset()) {
                    firstIndex = i;
                }

                if (candidate.getRange().getEndOffset() == span.getEndOffset()) {
                    lastIndex = i;
                }
            }

            if (firstIndex < 0 || lastIndex < firstIndex) {
                continue;
            }

            ArgumentCandidate firstCandidate = arguments.get(firstIndex);
            ArgumentCandidate lastCandidate = arguments.get(lastIndex);
            if (firstCandidate == null || lastCandidate == null) {
                continue;
            }

            if (firstCandidate.getRange().getStartOffset() != span.getStartOffset()
                    || lastCandidate.getRange().getEndOffset() != span.getEndOffset()) {
                continue;
            }

            ArgumentSlice candidateSlice = new ArgumentSlice(list, firstIndex, lastIndex);
            if (best == null || widthOf(list) < widthOf(best.getList())) {
                best = candidateSlice;
            }
        }

        return best;
    }

    private static ArgumentInsertionPlan createPlan(int replaceStart, int replaceEnd, String insertedText) {
        return new ArgumentInsertionPlan(replaceStart, replaceEnd, insertedText, replaceStart, replaceStart, replaceStart + insertedText.length());
    }

    private static int widthOf(ArgumentList list) {
        return list.getCloseOffset() - list.getOpenOffset();
    }

    private static boolean containsLineBreak(String text, int start, int end) {
        if (text == null) {
            return false;
        }

        int safeStart = Math.max(0, Math.min(start, text.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, text.length()));
        for (int i = safeStart; i < safeEnd; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                return true;
            }
        }
        return false;
    }

    private static final class ArgumentSlice {
        private final ArgumentList _list;
        private final int _firstIndex;
        private final int _lastIndex;

        private ArgumentSlice(ArgumentList list, int firstIndex, int lastIndex) {
            _list = list;
            _firstIndex = firstIndex;
            _lastIndex = lastIndex;
        }

        private ArgumentList getList() {
            return _list;
        }

        private int getFirstIndex() {
            return _firstIndex;
        }

        private int getLastIndex() {
            return _lastIndex;
        }

        private ArgumentCandidate getFirstCandidate() {
            return _list.getArguments().get(_firstIndex);
        }

        private ArgumentCandidate getLastCandidate() {
            return _list.getArguments().get(_lastIndex);
        }
    }
}
