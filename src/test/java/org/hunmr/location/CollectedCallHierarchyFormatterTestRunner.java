package org.hunmr.location;

public final class CollectedCallHierarchyFormatterTestRunner {
    public static void main(String[] args) {
        run("hierarchy labels start at aa", new Runnable() {
            @Override
            public void run() {
                assertEquals("aa", CollectedCallHierarchyFormatter.nextLabel(""), "first hierarchy label should be aa");
            }
        });

        run("hierarchy entry includes relation and container", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("method", "buildPlan", "class", "Main");
                assertEquals("- (aa)= [caller] method `buildPlan` in class `Main`  (at /tmp/x.java:18)",
                        CollectedCallHierarchyFormatter.formatEntry("aa", "caller", context, "/tmp/x.java", 18),
                        "hierarchy entry should include relation and symbol context");
            }
        });

        run("hierarchy block groups incoming and outgoing entries", new Runnable() {
            @Override
            public void run() {
                String block = CollectedCallHierarchyFormatter.formatBlock(
                        "method `buildPlan`",
                        "- (aa)= [caller] method `caller`  (at /tmp/x.java:10)\n",
                        "- (ab)= [callee] method `callee`  (at /tmp/x.java:20)\n"
                );
                assertEquals("Call hierarchy for method `buildPlan`:\n[incoming callers]\n- (aa)= [caller] method `caller`  (at /tmp/x.java:10)\n[outgoing callees]\n- (ab)= [callee] method `callee`  (at /tmp/x.java:20)\n",
                        block,
                        "hierarchy block should keep incoming and outgoing sections");
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
