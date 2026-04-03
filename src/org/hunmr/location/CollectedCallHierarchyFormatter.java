package org.hunmr.location;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CollectedCallHierarchyFormatter {
    private static final Pattern LABEL_PATTERN = Pattern.compile("(?m)^- \\(([a-z]+)\\)=");

    private CollectedCallHierarchyFormatter() {
    }

    public static String nextLabel(String text) {
        Matcher matcher = LABEL_PATTERN.matcher(text == null ? "" : text);
        int maxIndex = -1;
        while (matcher.find()) {
            maxIndex = Math.max(maxIndex, decodeLabel(matcher.group(1)));
        }
        return encodeLabel(maxIndex + 1);
    }

    public static String formatBlock(String targetDescription, String callers, String callees) {
        StringBuilder builder = new StringBuilder();
        builder.append("Call hierarchy");
        if (targetDescription != null && !targetDescription.isEmpty()) {
            builder.append(" for ").append(targetDescription);
        }
        builder.append(":\n");
        if (callers != null && !callers.isEmpty()) {
            builder.append("[incoming callers]\n").append(callers);
        }
        if (callees != null && !callees.isEmpty()) {
            builder.append("[outgoing callees]\n").append(callees);
        }
        return builder.toString();
    }

    public static String formatEntry(String label,
                                     String relation,
                                     CollectedLocationContext context,
                                     String absolutePath,
                                     int lineNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append("- (").append(normalizeLabel(label)).append(")= ");
        if (relation != null && !relation.isEmpty()) {
            builder.append('[').append(relation).append("] ");
        }
        builder.append(formatContext(context));
        builder.append("  ").append(formatLocation(absolutePath, lineNumber));
        return builder.toString();
    }

    private static String formatContext(CollectedLocationContext context) {
        CollectedLocationContext safeContext = context == null ? CollectedLocationContext.EMPTY : context;
        if (safeContext.hasSymbol()) {
            StringBuilder builder = new StringBuilder();
            builder.append(safeContext.getSymbolKind()).append(" `").append(safeContext.getSymbolName()).append('`');
            if (safeContext.hasContainer()) {
                builder.append(" in ")
                        .append(safeContext.getContainerKind())
                        .append(" `")
                        .append(safeContext.getContainerName())
                        .append('`');
            }
            return builder.toString();
        }
        return "symbol `<unknown>`";
    }

    private static String formatLocation(String absolutePath, int lineNumber) {
        String safePath = absolutePath == null ? "" : absolutePath;
        if (lineNumber <= 0) {
            return "(at " + safePath + ")";
        }
        return "(at " + safePath + ":" + lineNumber + ")";
    }

    private static String normalizeLabel(String label) {
        String safeLabel = label == null ? "" : label.trim();
        return safeLabel.isEmpty() ? "aa" : safeLabel;
    }

    private static String encodeLabel(int index) {
        int safeIndex = Math.max(0, index);
        StringBuilder builder = new StringBuilder();
        int current = safeIndex;
        do {
            builder.insert(0, (char) ('a' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);

        while (builder.length() < 2) {
            builder.insert(0, 'a');
        }
        return builder.toString();
    }

    private static int decodeLabel(String label) {
        if (label == null || label.isEmpty()) {
            return -1;
        }

        int value = 0;
        for (int i = 0; i < label.length(); i++) {
            char current = label.charAt(i);
            if (current < 'a' || current > 'z') {
                return -1;
            }
            value = value * 26 + (current - 'a' + 1);
        }
        return value - 1;
    }
}
