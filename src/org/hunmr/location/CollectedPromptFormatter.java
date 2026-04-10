package org.hunmr.location;

public final class CollectedPromptFormatter {
    private static final String USAGES_SECTION = "[Usages]";
    private static final String CALL_HIERARCHY_SECTION = "[Call Hierarchy]";
    private static final String CONTEXT_HEADER = "Context:\n";
    private static final String PROBLEM_HEADER = "Problem:\n";
    private static final String DESIRED_OUTCOME_HEADER = "Desired Outcome:\n";
    private static final String TASK_HEADER = "Task:\n";
    private static final String CONSTRAINTS_HEADER = "Constraints:\n";
    private static final String DEFAULT_TEMPLATE = CONTEXT_HEADER + "\n"
            + PROBLEM_HEADER + "- \n\n"
            + DESIRED_OUTCOME_HEADER + "- \n\n"
            + TASK_HEADER + "- \n\n"
            + CONSTRAINTS_HEADER + "- \n";

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
        String safeLine = normalizeLineBreaks(trimTrailingLineBreaks(line));
        if (safeHeader.isEmpty() || safeLine.isEmpty()) {
            return false;
        }

        String sectionBody = normalizeLineBreaks(extractContextSection(normalizeTemplate(existingText), safeHeader));
        if (safeLine.indexOf('\n') >= 0) {
            return containsSectionBlock(sectionBody, safeLine);
        }

        String[] lines = splitLines(sectionBody);
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
        normalized = ensureSection(normalized, PROBLEM_HEADER, DESIRED_OUTCOME_HEADER, TASK_HEADER, CONSTRAINTS_HEADER);
        normalized = ensureSection(normalized, DESIRED_OUTCOME_HEADER, TASK_HEADER, CONSTRAINTS_HEADER);
        normalized = ensureSection(normalized, TASK_HEADER, CONSTRAINTS_HEADER);
        normalized = ensureSection(normalized, CONSTRAINTS_HEADER);
        if (!normalized.endsWith("\n")) {
            normalized += '\n';
        }
        return normalized;
    }

    private static String ensureSection(String text, String sectionHeader, String... beforeHeaders) {
        String sectionLabel = trimTrailingLineBreaks(sectionHeader);
        if (text.contains(sectionLabel)) {
            return text;
        }

        String sectionBlock = sectionHeader + "- \n";
        int insertIndex = findFirstHeaderIndex(text, beforeHeaders);
        if (insertIndex < 0) {
            return trimTrailingLineBreaks(text) + "\n\n" + sectionBlock;
        }

        String before = trimTrailingLineBreaks(text.substring(0, insertIndex));
        String after = text.substring(insertIndex);
        return before + "\n\n" + sectionBlock + "\n" + after;
    }

    private static int findFirstHeaderIndex(String text, String... headers) {
        int firstIndex = -1;
        for (int i = 0; i < headers.length; i++) {
            int index = indexOfHeader(text, headers[i]);
            if (index >= 0 && (firstIndex < 0 || index < firstIndex)) {
                firstIndex = index;
            }
        }
        return firstIndex;
    }

    private static int findPromptTailIndex(String text) {
        return findFirstHeaderIndex(text, PROBLEM_HEADER, DESIRED_OUTCOME_HEADER, TASK_HEADER, CONSTRAINTS_HEADER);
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
        int tailIndex = findPromptTailIndex(template);
        if (tailIndex < 0) {
            return new PromptParts(template, "");
        }
        return new PromptParts(template.substring(0, tailIndex), template.substring(tailIndex));
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
        String separator = sectionLine.indexOf('\n') >= 0 ? "\n\n" : "\n";
        return trimTrailingLineBreaks(beforeSection) + "\n\n" + trimTrailingLineBreaks(sectionBody) + separator + sectionLine + '\n';
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
        int index = indexOfHeader(contextPart, sectionHeader + "\n");
        if (index >= 0) {
            return index;
        }
        return -1;
    }

    private static int indexOfHeader(String text, String header) {
        int index = text.indexOf("\n" + header);
        if (index >= 0) {
            return index + 1;
        }
        if (text.startsWith(header)) {
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
        String safeText = normalizeLineBreaks(trimTrailingLineBreaks(text));
        if (safeText.isEmpty()) {
            return new String[0];
        }
        return safeText.split("\n");
    }

    private static String normalizeLineBreaks(String text) {
        return (text == null ? "" : text).replace("\r\n", "\n").replace('\r', '\n');
    }

    private static boolean containsSectionBlock(String sectionBody, String block) {
        String safeSectionBody = trimTrailingLineBreaks(normalizeLineBreaks(sectionBody));
        String safeBlock = trimTrailingLineBreaks(normalizeLineBreaks(block));
        if (safeSectionBody.isEmpty() || safeBlock.isEmpty()) {
            return false;
        }
        if (safeSectionBody.equals(safeBlock)) {
            return true;
        }
        return safeSectionBody.startsWith(safeBlock + "\n\n")
                || safeSectionBody.contains("\n\n" + safeBlock + "\n\n")
                || safeSectionBody.endsWith("\n\n" + safeBlock);
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
