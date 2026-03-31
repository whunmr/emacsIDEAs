package org.hunmr.argument;

public final class ArgumentListRangePlanner {
    private ArgumentListRangePlanner() {
    }

    public static TextSpan planBefore(String text, int caretOffset) {
        return plan(text, caretOffset, true);
    }

    public static TextSpan planAfter(String text, int caretOffset) {
        return plan(text, caretOffset, false);
    }

    private static TextSpan plan(String text, int caretOffset, boolean before) {
        String safeText = text == null ? "" : text;
        int safeOffset = Math.max(0, Math.min(caretOffset, safeText.length()));

        ParsedArguments parsedArguments = ArgumentParser.parse(safeText);
        ArgumentList list = parsedArguments.findInnermostList(safeOffset);
        if (list == null || list.getArguments().isEmpty()) {
            return null;
        }

        ArgumentCandidate first = null;
        ArgumentCandidate last = null;
        for (ArgumentCandidate candidate : list.getArguments()) {
            if (!shouldInclude(candidate, safeOffset, before)) {
                continue;
            }

            if (first == null) {
                first = candidate;
            }
            last = candidate;
        }

        if (first == null || last == null || first.getRange() == null || last.getRange() == null) {
            return null;
        }

        return new TextSpan(first.getRange().getStartOffset(), last.getRange().getEndOffset());
    }

    private static boolean shouldInclude(ArgumentCandidate candidate, int caretOffset, boolean before) {
        if (candidate == null || candidate.getRange() == null) {
            return false;
        }

        if (before) {
            return candidate.getAnchorOffset() < caretOffset;
        }

        return candidate.getRange().getEndOffset() > caretOffset;
    }
}
