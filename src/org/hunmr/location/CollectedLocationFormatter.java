package org.hunmr.location;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CollectedLocationFormatter {
    private static final Pattern LABEL_PATTERN = Pattern.compile("(?m)^-\\s*<([a-z]+)>=");

    private CollectedLocationFormatter() {
    }

    public static String nextLabel(String clipboardText) {
        Matcher matcher = LABEL_PATTERN.matcher(clipboardText == null ? "" : clipboardText);
        int maxIndex = -1;
        while (matcher.find()) {
            maxIndex = Math.max(maxIndex, decodeLabel(matcher.group(1)));
        }

        return encodeLabel(maxIndex + 1);
    }

    public static String appendEntry(String clipboardText, String entry) {
        String safeClipboardText = clipboardText == null ? "" : clipboardText;
        String safeEntry = entry == null ? "" : entry;
        if (safeEntry.isEmpty()) {
            return safeClipboardText;
        }

        StringBuilder builder = new StringBuilder(safeClipboardText);
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        builder.append(safeEntry);
        if (builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        return builder.toString();
    }

    public static String formatEntry(String label,
                                     String selectedText,
                                     String absolutePath,
                                     int startLine,
                                     int endLine,
                                     boolean fullFileSelection) {
        String safeLabel = formatLabel(label);
        String location = formatLocation(absolutePath, startLine, endLine, fullFileSelection);
        String safeSelectedText = stripTrailingLineBreaks(selectedText);
        if (safeSelectedText.isEmpty()) {
            return safeLabel + " " + location;
        }

        if (isSingleLine(safeSelectedText)) {
            return safeLabel + " `" + safeSelectedText + "`  " + location;
        }

        String preview = firstTwoLines(safeSelectedText);
        return safeLabel + " ```\n" + preview + "\n```  " + location;
    }

    public static String toHintHtml(String entry) {
        String safeEntry = escapeHtml(entry == null ? "" : entry);
        return "<html><pre>" + safeEntry + "</pre></html>";
    }

    private static String formatLocation(String absolutePath, int startLine, int endLine, boolean fullFileSelection) {
        String safePath = absolutePath == null ? "" : absolutePath;
        String location;
        if (fullFileSelection || startLine <= 0) {
            location = safePath;
        } else if (endLine <= startLine) {
            location = safePath + ":" + startLine;
        } else {
            location = safePath + ":" + startLine + "-" + endLine;
        }

        return "(at " + location + ")";
    }

    private static String stripTrailingLineBreaks(String text) {
        String safeText = text == null ? "" : text;
        int end = safeText.length();
        while (end > 0) {
            char current = safeText.charAt(end - 1);
            if (current != '\n' && current != '\r') {
                break;
            }
            end--;
        }
        return safeText.substring(0, end);
    }

    private static boolean isSingleLine(String text) {
        return text.indexOf('\n') < 0 && text.indexOf('\r') < 0;
    }

    private static String firstTwoLines(String text) {
        String normalized = (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n');
        String[] lines = normalized.split("\n", -1);
        if (lines.length == 0) {
            return "";
        }

        if (lines.length == 1) {
            return lines[0];
        }

        return lines[0] + "\n" + lines[1];
    }

    private static String encodeLabel(int index) {
        int safeIndex = Math.max(0, index);
        StringBuilder builder = new StringBuilder();
        int current = safeIndex;
        do {
            builder.insert(0, (char) ('a' + (current % 26)));
            current = current / 26 - 1;
        } while (current >= 0);
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

    private static String formatLabel(String label) {
        String safeLabel = label == null ? "a" : label.trim();
        if (safeLabel.startsWith("-")) {
            Matcher matcher = LABEL_PATTERN.matcher(safeLabel);
            if (matcher.find()) {
                safeLabel = matcher.group(1);
            } else {
                safeLabel = safeLabel.replaceFirst("^-\\s*<", "").replaceFirst(">=$", "");
            }
        }

        if (safeLabel.isEmpty()) {
            safeLabel = "a";
        }
        return "- <" + safeLabel + ">=";
    }

    private static String escapeHtml(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char current = text.charAt(i);
            switch (current) {
                case '&':
                    builder.append("&amp;");
                    break;
                case '<':
                    builder.append("&lt;");
                    break;
                case '>':
                    builder.append("&gt;");
                    break;
                case '"':
                    builder.append("&quot;");
                    break;
                default:
                    builder.append(current);
                    break;
            }
        }
        return builder.toString();
    }
}
