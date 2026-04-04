package org.hunmr.location;

public final class CollectedCallHierarchyFormatter {
    private CollectedCallHierarchyFormatter() {
    }

    public static String formatBlock(String targetDescription, String callers, String callees) {
        StringBuilder builder = new StringBuilder();
        builder.append("- Call hierarchy");
        if (targetDescription != null && !targetDescription.isEmpty()) {
            builder.append(" for ").append(targetDescription);
        }
        builder.append(":\n");
        if (callers != null && !callers.isEmpty()) {
            builder.append("- [incoming callers]\n").append(callers);
        }
        if (callees != null && !callees.isEmpty()) {
            builder.append("- [outgoing callees]\n").append(callees);
        }
        return builder.toString();
    }

    public static String formatEntry(String relation,
                                     CollectedLocationContext context,
                                     String absolutePath,
                                     int lineNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append("- ");
        if (relation != null && !relation.isEmpty()) {
            builder.append('[').append(relation).append("] ");
        }
        builder.append(formatContext(context));
        builder.append("  ").append(formatLocation(absolutePath, lineNumber));
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

}
