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
                                     String usageCategory,
                                     String lineContent,
                                     CollectedLocationContext context,
                                     String absolutePath,
                                     int lineNumber) {
        String safeLineContent = sanitizeLineContent(lineContent);
        String location = formatLocation(absolutePath, lineNumber);
        String container = formatContainer(context);
        String categoryPrefix = formatCategory(usageCategory);
        if (!container.isEmpty()) {
            return "- (" + normalizeLabel(label) + ")= " + categoryPrefix + "`" + safeLineContent + "` located in { " + container + " }  " + location;
        }

        return "- (" + normalizeLabel(label) + ")= " + categoryPrefix + "`" + safeLineContent + "`  " + location;
    }

    public static String formatBlock(String title, java.util.Map<String, java.util.List<String>> groupedEntries) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ").append(title).append(":\n");
        for (java.util.Map.Entry<String, java.util.List<String>> entry : groupedEntries.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isEmpty()) {
                continue;
            }
            builder.append("- [").append(entry.getKey()).append("]\n");
            for (String line : entry.getValue()) {
                builder.append("  ").append(line).append('\n');
            }
        }
        return builder.toString();
    }

    public static String formatSectionBlock(String block) {
        String trimmedBlock = trimTrailingLineBreaks(block);
        if (trimmedBlock.isEmpty()) {
            return "";
        }

        String[] lines = trimmedBlock.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i > 0) {
                builder.append('\n').append("  ");
            }
            builder.append(lines[i]);
        }
        return builder.toString();
    }

    static String describeUsagePresentation(String targetsNodeText,
                                           String searchString,
                                           String tabText,
                                           String tabName) {
        String kind = extractUsagePresentationKind(targetsNodeText);
        String name = firstNonEmptyUsageName(searchString, tabText, tabName);
        if (!kind.isEmpty() && !name.isEmpty()) {
            return kind + " " + name;
        }
        if (!name.isEmpty()) {
            return name;
        }
        if (!kind.isEmpty()) {
            return kind;
        }
        return firstNonEmptyPresentationText(targetsNodeText, tabText, tabName, searchString);
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

    private static String formatCategory(String usageCategory) {
        String safeCategory = usageCategory == null ? "" : usageCategory.trim();
        if (safeCategory.isEmpty()) {
            return "";
        }
        return "[" + safeCategory + "] ";
    }

    private static String normalizeLabel(String label) {
        String safeLabel = label == null ? "" : label.trim();
        if (safeLabel.isEmpty()) {
            return "aa";
        }
        return safeLabel;
    }

    private static String extractUsagePresentationKind(String text) {
        String normalized = normalizeUsagePresentationText(text);
        if (normalized.isEmpty()) {
            return "";
        }

        String lowerCase = normalized.toLowerCase();
        if ("method".equals(lowerCase) || "methods".equals(lowerCase)) {
            return "Method";
        }
        if ("function".equals(lowerCase) || "functions".equals(lowerCase)) {
            return "Function";
        }
        if ("class".equals(lowerCase) || "classes".equals(lowerCase)) {
            return "Class";
        }
        if ("interface".equals(lowerCase) || "interfaces".equals(lowerCase)) {
            return "Interface";
        }
        if ("struct".equals(lowerCase) || "structs".equals(lowerCase)) {
            return "Struct";
        }
        if ("type".equals(lowerCase) || "types".equals(lowerCase)) {
            return "Type";
        }
        if ("field".equals(lowerCase) || "fields".equals(lowerCase)) {
            return "Field";
        }

        if (normalized.length() == 1) {
            return normalized.toUpperCase();
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    private static String firstNonEmptyUsageName(String... candidates) {
        if (candidates == null) {
            return "";
        }

        for (int i = 0; i < candidates.length; i++) {
            String usageName = normalizeUsageTargetName(candidates[i]);
            if (!usageName.isEmpty()) {
                return usageName;
            }
        }
        return "";
    }

    private static String firstNonEmptyPresentationText(String... candidates) {
        if (candidates == null) {
            return "";
        }

        for (int i = 0; i < candidates.length; i++) {
            String normalized = normalizeUsagePresentationText(candidates[i]);
            if (!normalized.isEmpty()) {
                return normalized;
            }
        }
        return "";
    }

    private static String normalizeUsageTargetName(String text) {
        String normalized = normalizeUsagePresentationText(text);
        if (normalized.isEmpty()) {
            return "";
        }

        normalized = normalized.replaceFirst("(?i)\\s+in\\s+all\\s+places\\s*$", "");
        normalized = normalized.replaceFirst("(?i)\\s+in\\s+project\\s*$", "");
        normalized = normalized.replaceFirst("(?i)\\s+in\\s+.*$", "");
        normalized = normalized.replaceFirst("(?i)^methods?\\s+", "");
        normalized = normalized.replaceFirst("(?i)^functions?\\s+", "");
        normalized = normalized.replaceFirst("(?i)^classes?\\s+", "");
        normalized = normalized.replaceFirst("(?i)^interfaces?\\s+", "");
        normalized = normalized.replaceFirst("(?i)^structs?\\s+", "");
        normalized = normalized.replaceFirst("(?i)^types?\\s+", "");
        normalized = normalized.replaceFirst("(?i)^fields?\\s+", "");
        normalized = normalized.trim();

        String lowerCase = normalized.toLowerCase();
        if (normalized.isEmpty()
                || "method".equals(lowerCase)
                || "function".equals(lowerCase)
                || "class".equals(lowerCase)
                || "interface".equals(lowerCase)
                || "struct".equals(lowerCase)
                || "type".equals(lowerCase)
                || "field".equals(lowerCase)) {
            return "";
        }

        return normalized;
    }

    private static String normalizeUsagePresentationText(String text) {
        String safeText = text == null ? "" : text.trim();
        if (safeText.isEmpty()) {
            return "";
        }

        String normalized = safeText;
        normalized = normalized.replaceFirst("(?i)^find\\s+usages\\s+of\\s+", "");
        normalized = normalized.replaceFirst("(?i)^usages\\s+of\\s+", "");
        normalized = normalized.replaceFirst("(?i)^usage\\s+of\\s+", "");
        normalized = normalized.replaceFirst("(?i)^methods?\\s+to\\s+", "");
        normalized = normalized.replaceFirst("(?i)^functions?\\s+to\\s+", "");
        normalized = normalized.trim();
        if (normalized.isEmpty()) {
            return "";
        }

        if (normalized.indexOf('`') >= 0) {
            return normalized;
        }
        return normalized;
    }

    private static String trimTrailingLineBreaks(String text) {
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
