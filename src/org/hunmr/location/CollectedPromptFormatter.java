package org.hunmr.location;

public final class CollectedPromptFormatter {
    private static final String CONTEXT_HEADER = "Context:\n";
    private static final String TASK_HEADER = "Task:\n";
    private static final String CONSTRAINTS_HEADER = "Constraints:\n";
    private static final String DEFAULT_TEMPLATE = CONTEXT_HEADER + "\n" + TASK_HEADER + "- \n\n" + CONSTRAINTS_HEADER + "- \n";

    private CollectedPromptFormatter() {
    }

    public static String appendToContext(String existingText, String contextBlock) {
        String safeBlock = trimTrailingLineBreaks(contextBlock);
        if (safeBlock.isEmpty()) {
            return normalizeTemplate(existingText);
        }

        String template = normalizeTemplate(existingText);
        int taskIndex = findTaskHeaderIndex(template);
        StringBuilder builder = new StringBuilder();
        if (taskIndex < 0) {
            builder.append(template);
            if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            builder.append(safeBlock);
            if (builder.charAt(builder.length() - 1) != '\n') {
                builder.append('\n');
            }
            return builder.toString();
        }

        String contextPart = template.substring(0, taskIndex);
        String tailPart = template.substring(taskIndex);
        boolean emptyContextBody = trimTrailingLineBreaks(contextPart).equals("Context:");
        builder.append(contextPart);
        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        if (!emptyContextBody && builder.toString().endsWith("\n\n")) {
            builder.setLength(builder.length() - 1);
        }
        builder.append(safeBlock);
        if (builder.charAt(builder.length() - 1) != '\n') {
            builder.append('\n');
        }
        if (!tailPart.startsWith("\n")) {
            builder.append('\n');
        }
        builder.append(tailPart);
        return builder.toString();
    }

    private static String normalizeTemplate(String text) {
        String safeText = text == null ? "" : text;
        if (safeText.trim().isEmpty()) {
            return DEFAULT_TEMPLATE;
        }

        String normalized = safeText;
        if (!normalized.contains("Context:")) {
            normalized = CONTEXT_HEADER + '\n' + trimTrailingLineBreaks(normalized) + '\n';
        }
        if (!normalized.contains("Task:")) {
            normalized = trimTrailingLineBreaks(normalized) + "\n\n" + TASK_HEADER + "- \n";
        }
        if (!normalized.contains("Constraints:")) {
            normalized = trimTrailingLineBreaks(normalized) + "\n\n" + CONSTRAINTS_HEADER + "- \n";
        }
        if (!normalized.endsWith("\n")) {
            normalized += '\n';
        }
        return normalized;
    }

    private static int findTaskHeaderIndex(String text) {
        int index = text.indexOf("\n" + TASK_HEADER);
        if (index >= 0) {
            return index + 1;
        }
        if (text.startsWith(TASK_HEADER)) {
            return 0;
        }
        return -1;
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
