package org.hunmr.location;

public final class CollectedPromptFormatter {
    private static final String USAGES_SECTION = "[Usages]";
    private static final String CALL_HIERARCHY_SECTION = "[Call Hierarchy]";
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
        PromptParts parts = splitPrompt(template);
        String updatedContext = appendMainContext(parts.contextPart, safeBlock);
        return joinPrompt(updatedContext, parts.tailPart);
    }

    public static String withPromptHeader(String existingText, String promptHeader) {
        String normalized = normalizeTemplate(existingText);
        String safeHeader = trimTrailingLineBreaks(promptHeader);
        if (safeHeader.trim().isEmpty()) {
            return normalized;
        }
        if (startsWithPromptHeader(normalized, safeHeader)) {
            return normalized;
        }
        return safeHeader + "\n\n" + normalized;
    }

    public static String appendToContextSection(String existingText, String sectionHeader, String sectionLine) {
        String safeHeader = trimTrailingLineBreaks(sectionHeader);
        String safeLine = trimTrailingLineBreaks(sectionLine);
        if (safeHeader.isEmpty() || safeLine.isEmpty()) {
            return normalizeTemplate(existingText);
        }

        String template = normalizeTemplate(existingText);
        PromptParts parts = splitPrompt(template);
        String updatedContext = appendContextSection(parts.contextPart, safeHeader, safeLine);
        return joinPrompt(updatedContext, parts.tailPart);
    }

    public static boolean contextContainsLine(String existingText, String line) {
        String safeLine = trimTrailingLineBreaks(line);
        if (safeLine.isEmpty()) {
            return false;
        }

        String[] lines = splitLines(extractContextPart(normalizeTemplate(existingText)));
        for (int i = 0; i < lines.length; i++) {
            if (safeLine.equals(lines[i])) {
                return true;
            }
        }
        return false;
    }

    public static boolean contextSectionContainsLine(String existingText, String sectionHeader, String line) {
        String safeHeader = trimTrailingLineBreaks(sectionHeader);
        String safeLine = trimTrailingLineBreaks(line);
        if (safeHeader.isEmpty() || safeLine.isEmpty()) {
            return false;
        }

        String[] lines = splitLines(extractContextSection(normalizeTemplate(existingText), safeHeader));
        for (int i = 0; i < lines.length; i++) {
            if (safeLine.equals(lines[i])) {
                return true;
            }
        }
        return false;
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

    private static PromptParts splitPrompt(String template) {
        int taskIndex = findTaskHeaderIndex(template);
        if (taskIndex < 0) {
            return new PromptParts(template, "");
        }
        return new PromptParts(template.substring(0, taskIndex), template.substring(taskIndex));
    }

    private static String joinPrompt(String contextPart, String tailPart) {
        StringBuilder builder = new StringBuilder();
        builder.append(trimTrailingLineBreaks(contextPart)).append('\n');
        if (tailPart != null && !tailPart.isEmpty()) {
            if (!tailPart.startsWith("\n")) {
                builder.append('\n');
            }
            builder.append(tailPart);
        }
        return builder.toString();
    }

    private static String appendMainContext(String contextPart, String block) {
        String trimmedContext = trimTrailingLineBreaks(contextPart);
        int firstSectionIndex = findFirstContextSectionIndex(trimmedContext);
        if (firstSectionIndex >= 0) {
            String beforeSection = trimTrailingLineBreaks(trimmedContext.substring(0, firstSectionIndex));
            String sectionAndAfter = trimmedContext.substring(firstSectionIndex);
            if (trimTrailingLineBreaks(beforeSection).equals("Context:")) {
                return beforeSection + "\n\n" + block + "\n\n" + sectionAndAfter;
            }
            return beforeSection + "\n" + block + "\n\n" + sectionAndAfter;
        }

        if (trimmedContext.equals("Context:")) {
            return trimmedContext + "\n\n" + block + '\n';
        }
        return trimmedContext + "\n" + block + '\n';
    }

    private static String appendContextSection(String contextPart, String sectionHeader, String sectionLine) {
        String trimmedContext = trimTrailingLineBreaks(contextPart);
        int sectionIndex = indexOfSection(trimmedContext, sectionHeader);
        if (sectionIndex < 0) {
            int insertBeforeIndex = findNextSectionIndex(trimmedContext, sectionHeader);
            if (insertBeforeIndex >= 0) {
                String beforeSection = trimTrailingLineBreaks(trimmedContext.substring(0, insertBeforeIndex));
                String sectionAndAfter = trimmedContext.substring(insertBeforeIndex);
                if (trimTrailingLineBreaks(beforeSection).equals("Context:")) {
                    return beforeSection + "\n\n" + sectionHeader + "\n" + sectionLine + "\n\n" + sectionAndAfter;
                }
                return beforeSection + "\n\n" + sectionHeader + "\n" + sectionLine + "\n\n" + sectionAndAfter;
            }
            if (trimmedContext.equals("Context:")) {
                return trimmedContext + "\n\n" + sectionHeader + "\n" + sectionLine + '\n';
            }
            return trimmedContext + "\n\n" + sectionHeader + "\n" + sectionLine + '\n';
        }

        String beforeSection = trimmedContext.substring(0, sectionIndex);
        String sectionBody = trimmedContext.substring(sectionIndex);
        return trimTrailingLineBreaks(beforeSection) + "\n\n" + trimTrailingLineBreaks(sectionBody) + "\n" + sectionLine + '\n';
    }

    private static String extractContextPart(String template) {
        return splitPrompt(template).contextPart;
    }

    private static String extractContextSection(String template, String sectionHeader) {
        String contextPart = trimTrailingLineBreaks(extractContextPart(template));
        int sectionIndex = indexOfSection(contextPart, sectionHeader);
        if (sectionIndex < 0) {
            return "";
        }
        String sectionBody = contextPart.substring(sectionIndex);
        String prefix = sectionHeader + "\n";
        if (!sectionBody.startsWith(prefix)) {
            return "";
        }
        return sectionBody.substring(prefix.length());
    }

    private static int indexOfSection(String contextPart, String sectionHeader) {
        int index = contextPart.indexOf("\n" + sectionHeader + "\n");
        if (index >= 0) {
            return index + 1;
        }
        if (contextPart.startsWith(sectionHeader + "\n")) {
            return 0;
        }
        return -1;
    }

    private static int findFirstContextSectionIndex(String contextPart) {
        int usagesIndex = indexOfSection(contextPart, USAGES_SECTION);
        int callHierarchyIndex = indexOfSection(contextPart, CALL_HIERARCHY_SECTION);
        if (usagesIndex < 0) {
            return callHierarchyIndex;
        }
        if (callHierarchyIndex < 0) {
            return usagesIndex;
        }
        return Math.min(usagesIndex, callHierarchyIndex);
    }

    private static int findNextSectionIndex(String contextPart, String sectionHeader) {
        if (USAGES_SECTION.equals(sectionHeader)) {
            return indexOfSection(contextPart, CALL_HIERARCHY_SECTION);
        }
        return -1;
    }

    private static String[] splitLines(String text) {
        String safeText = trimTrailingLineBreaks(text).replace("\r\n", "\n").replace('\r', '\n');
        if (safeText.isEmpty()) {
            return new String[0];
        }
        return safeText.split("\n");
    }

    private static boolean startsWithPromptHeader(String text, String promptHeader) {
        String safeText = text == null ? "" : text;
        return safeText.startsWith(promptHeader + "\n") || safeText.equals(promptHeader);
    }

    private static final class PromptParts {
        private final String contextPart;
        private final String tailPart;

        private PromptParts(String contextPart, String tailPart) {
            this.contextPart = contextPart == null ? "" : contextPart;
            this.tailPart = tailPart == null ? "" : tailPart;
        }
    }
}
