package org.hunmr.argument;

final class ArgumentListFormatting {
    private ArgumentListFormatting() {
    }

    static boolean isMultilineList(String text, ArgumentList list) {
        if (text == null || list == null) {
            return false;
        }

        int start = Math.max(0, list.getOpenOffset() + 1);
        int end = Math.max(start, Math.min(list.getCloseOffset(), text.length()));
        return text.substring(start, end).indexOf('\n') >= 0 || text.substring(start, end).indexOf('\r') >= 0;
    }

    static String detectLineSeparator(String text) {
        if (text == null) {
            return "\n";
        }

        int index = text.indexOf('\n');
        if (index > 0 && text.charAt(index - 1) == '\r') {
            return "\r\n";
        }

        return "\n";
    }

    static boolean hasTrailingComma(String text, ArgumentList list) {
        int previousIndex = findPreviousNonWhitespace(text, list.getCloseOffset() - 1, list.getOpenOffset());
        return previousIndex >= 0 && text.charAt(previousIndex) == ',';
    }

    static ArgumentCandidate findCandidateStartingAt(ArgumentList list, int offset) {
        for (ArgumentCandidate candidate : list.getArguments()) {
            if (candidate != null && candidate.getRange() != null && candidate.getRange().getStartOffset() == offset) {
                return candidate;
            }
        }
        return null;
    }

    static ArgumentCandidate findCandidateEndingAt(ArgumentList list, int offset) {
        for (ArgumentCandidate candidate : list.getArguments()) {
            if (candidate != null && candidate.getRange() != null && candidate.getRange().getEndOffset() == offset) {
                return candidate;
            }
        }
        return null;
    }

    static ArgumentCandidate findNextCandidate(ArgumentList list, ArgumentCandidate current) {
        if (list == null || current == null) {
            return null;
        }

        int nextIndex = current.getIndexInList() + 1;
        if (nextIndex < 0 || nextIndex >= list.getArguments().size()) {
            return null;
        }

        return list.getArguments().get(nextIndex);
    }

    static ArgumentCandidate findCandidateStartingAtOrAfter(ArgumentList list, int offset) {
        if (list == null) {
            return null;
        }

        for (ArgumentCandidate candidate : list.getArguments()) {
            if (candidate != null
                    && candidate.getRange() != null
                    && candidate.getRange().getStartOffset() >= offset) {
                return candidate;
            }
        }

        return null;
    }

    static ArgumentCandidate findCandidateEndingAtOrBefore(ArgumentList list, int offset) {
        if (list == null) {
            return null;
        }

        ArgumentCandidate best = null;
        for (ArgumentCandidate candidate : list.getArguments()) {
            if (candidate == null || candidate.getRange() == null) {
                continue;
            }

            if (candidate.getRange().getEndOffset() > offset) {
                break;
            }

            best = candidate;
        }

        return best;
    }

    static String getLineIndent(String text, int offset) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int lineStart = findLineStart(text, safeOffset);
        int cursor = lineStart;
        while (cursor < safeOffset && cursor < text.length()) {
            char c = text.charAt(cursor);
            if (c != ' ' && c != '\t') {
                break;
            }
            cursor++;
        }

        return text.substring(lineStart, cursor);
    }

    static int findLineStart(String text, int offset) {
        int cursor = Math.max(0, Math.min(offset, text.length()));
        while (cursor > 0) {
            char c = text.charAt(cursor - 1);
            if (c == '\n' || c == '\r') {
                break;
            }
            cursor--;
        }
        return cursor;
    }

    static String detectIndentUnit(String text, ArgumentList list) {
        if (list != null && !list.getArguments().isEmpty()) {
            String closeIndent = getLineIndent(text, list.getCloseOffset());
            String candidateIndent = getLineIndent(text, list.getArguments().get(0).getRange().getStartOffset());
            if (candidateIndent.startsWith(closeIndent) && candidateIndent.length() > closeIndent.length()) {
                return candidateIndent.substring(closeIndent.length());
            }
            return candidateIndent;
        }

        return "    ";
    }

    static String reindentArgumentText(String argumentText, String indent, String lineSeparator) {
        if (argumentText == null || argumentText.isEmpty()) {
            return "";
        }

        String normalized = argumentText.replace("\r\n", "\n").replace('\r', '\n');
        if (normalized.indexOf('\n') < 0) {
            return argumentText;
        }

        String[] lines = normalized.split("\n", -1);
        StringBuilder builder = new StringBuilder();
        builder.append(lines[0]);

        int minIndent = Integer.MAX_VALUE;
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                continue;
            }
            int currentIndent = countLeadingWhitespace(line);
            minIndent = Math.min(minIndent, currentIndent);
        }

        if (minIndent == Integer.MAX_VALUE) {
            minIndent = 0;
        }

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            builder.append(lineSeparator).append(indent);
            if (line.isEmpty()) {
                continue;
            }

            int trimStart = Math.min(minIndent, countLeadingWhitespace(line));
            builder.append(line.substring(trimStart));
        }

        return builder.toString();
    }

    static int countLeadingWhitespace(String text) {
        int index = 0;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (c != ' ' && c != '\t') {
                break;
            }
            index++;
        }
        return index;
    }

    static int findPreviousNonWhitespace(String text, int index, int lowerBoundExclusive) {
        for (int i = index; i > lowerBoundExclusive; i--) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
