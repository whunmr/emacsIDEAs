package org.hunmr.location;

public final class CollectedLocationContext {
    public static final CollectedLocationContext EMPTY = new CollectedLocationContext("", "", "", "");

    private final String symbolKind;
    private final String symbolName;
    private final String containerKind;
    private final String containerName;

    public CollectedLocationContext(String symbolKind, String symbolName, String containerKind, String containerName) {
        this.symbolKind = safe(symbolKind);
        this.symbolName = safe(symbolName);
        this.containerKind = safe(containerKind);
        this.containerName = safe(containerName);
    }

    public String getSymbolKind() {
        return symbolKind;
    }

    public String getSymbolName() {
        return symbolName;
    }

    public String getContainerKind() {
        return containerKind;
    }

    public String getContainerName() {
        return containerName;
    }

    public boolean hasSymbol() {
        return !symbolKind.isEmpty() && !symbolName.isEmpty();
    }

    public boolean hasContainer() {
        return !containerKind.isEmpty() && !containerName.isEmpty();
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
