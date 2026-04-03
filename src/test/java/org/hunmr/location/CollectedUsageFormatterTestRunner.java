package org.hunmr.location;

public final class CollectedUsageFormatterTestRunner {
    public static void main(String[] args) {
        run("usage labels start at aa", new Runnable() {
            @Override
            public void run() {
                assertEquals("aa", CollectedUsageFormatter.nextLabel(""), "first usage label should be aa");
            }
        });

        run("usage labels continue after az", new Runnable() {
            @Override
            public void run() {
                assertEquals("ba", CollectedUsageFormatter.nextLabel("- (az)= `x`  (at /tmp/x.go:1)\n"),
                        "label after az should be ba");
            }
        });

        run("usage entry includes container", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("reference", "target", "method", "buildPlan");
                assertEquals("- (aa)= `runPlan(target)` located in { method `buildPlan` }  (at /tmp/x.go:18)",
                        CollectedUsageFormatter.formatEntry("aa", "runPlan(target)", context, "/tmp/x.go", 18),
                        "usage entry should include line content and container");
            }
        });

        run("usage entry falls back to symbol when container is absent", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("function", "handleJump", "", "");
                assertEquals("- (ab)= `handleJump()` located in { function `handleJump` }  (at /tmp/x.go:21)",
                        CollectedUsageFormatter.formatEntry("ab", "handleJump()", context, "/tmp/x.go", 21),
                        "usage entry should fall back to symbol description");
            }
        });

        run("usage entry uses empty marker for blank lines", new Runnable() {
            @Override
            public void run() {
                assertEquals("- (ac)= `<empty>`  (at /tmp/x.go:22)",
                        CollectedUsageFormatter.formatEntry("ac", "   ", CollectedLocationContext.EMPTY, "/tmp/x.go", 22),
                        "blank line usages should still be representable");
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
