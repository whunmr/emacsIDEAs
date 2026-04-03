package org.hunmr.location;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CollectedUsageFormatter {
    private static final Pattern LABEL_PATTERN = Pattern.compile("(?m)^- \\(([a-z]+)\\)");

    private CollectedUsageFormatter() {
    }

    public static String nextLabel(String clipboardText) {
        Matcher matcher = LABEL_PATTERN.matcher(clipboardText == null ? "" : clipboardText);
        int maxIndex = -1;
        while (matcher.find()) {
            maxIndex = Math.max(maxIndex, decodeLabel(matcher.group(1)));
        }

        return encodeLabel(maxIndex + 1);
    }

    public static String formatEntry(String label,
                                     String lineContent,
                                     CollectedLocationContext context,
                                     String absolutePath,
                                     int lineNumber) {
        String safeLineContent = sanitizeLineContent(lineContent);
        String location = formatLocation(absolutePath, lineNumber);
        String container = formatContainer(context);
        if (!container.isEmpty()) {
            return "- (" + normalizeLabel(label) + ") `" + safeLineContent + "` located in { " + container + " }  " + location;
        }

        return "- (" + normalizeLabel(label) + ") `" + safeLineContent + "`  " + location;
    }

    private static String formatContainer(CollectedLocationContext context) {
        CollectedLocationContext safeContext = context == null ? CollectedLocationContext.EMPTY : context;
        if (safeContext.hasContainer()) {
            return safeContext.getContainerKind() + " `" + safeContext.getContainerName() + "`";
        }
        if (safeContext.hasSymbol()) {
            return safeContext.getSymbolKind() + " `" + safeContext.getSymbolName() + "`";
        }
        return "";
    }

    private static String formatLocation(String absolutePath, int lineNumber) {
        String safePath = absolutePath == null ? "" : absolutePath;
        if (lineNumber <= 0) {
            return "(at " + safePath + ")";
        }
        return "(at " + safePath + ":" + lineNumber + ")";
    }

    private static String sanitizeLineContent(String lineContent) {
        String safeLineContent = lineContent == null ? "" : lineContent.trim();
        if (safeLineContent.isEmpty()) {
            return "<empty>";
        }
        return safeLineContent;
    }

    private static String normalizeLabel(String label) {
        String safeLabel = label == null ? "" : label.trim();
        if (safeLabel.isEmpty()) {
            return "aa";
        }
        return safeLabel;
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
