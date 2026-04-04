package org.hunmr.location;

public final class CollectedCallHierarchyFormatterTestRunner {
    public static void main(String[] args) {
        run("hierarchy entry includes relation and container", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("method", "buildPlan", "class", "Main");
                assertEquals("[caller] method `buildPlan` in class `Main`  (at /tmp/x.java:18)",
                        CollectedCallHierarchyFormatter.formatEntry("caller", context, "/tmp/x.java", 18),
                        "hierarchy entry should include relation and symbol context");
            }
        });

        run("hierarchy block groups incoming and outgoing entries", new Runnable() {
            @Override
            public void run() {
                String block = CollectedCallHierarchyFormatter.formatBlock(
                        "method `buildPlan`",
                        "[caller depth-1] method `caller`  (at /tmp/x.java:10)\n",
                        "[callee depth-1] method `callee`  (at /tmp/x.java:20)\n"
                );
                assertEquals("Call hierarchy for method `buildPlan`:\n[incoming callers]\n[caller depth-1] method `caller`  (at /tmp/x.java:10)\n[outgoing callees]\n[callee depth-1] method `callee`  (at /tmp/x.java:20)\n",
                        block,
                        "hierarchy block should keep incoming and outgoing sections");
            }
        });

        run("hierarchy section block keeps entries on separate indented lines", new Runnable() {
            @Override
            public void run() {
                assertEquals("Call hierarchy:\n  [incoming callers]\n  [caller] method `caller`  (at /tmp/x.java:10)",
                        CollectedCallHierarchyFormatter.formatSectionBlock("Call hierarchy:\n[incoming callers]\n[caller] method `caller`  (at /tmp/x.java:10)\n"),
                        "hierarchy section block should keep one entry per line with indentation");
            }
        });
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (AssertionError error) {
            System.err.println("FAIL " + name);
            throw error;
        }
    }

    private static void assertEquals(String expected, String actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }
}
