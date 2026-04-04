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
                assertEquals("- (aa)= [call] `runPlan(target)` located in { method `buildPlan` }  (at /tmp/x.go:18)",
                        CollectedUsageFormatter.formatEntry("aa", "call", "runPlan(target)", context, "/tmp/x.go", 18),
                        "usage entry should include line content and container");
            }
        });

        run("usage entry falls back to symbol when container is absent", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("function", "handleJump", "", "");
                assertEquals("- (ab)= [reference] `handleJump()` located in { function `handleJump` }  (at /tmp/x.go:21)",
                        CollectedUsageFormatter.formatEntry("ab", "reference", "handleJump()", context, "/tmp/x.go", 21),
                        "usage entry should fall back to symbol description");
            }
        });

        run("usage entry uses empty marker for blank lines", new Runnable() {
            @Override
            public void run() {
                assertEquals("- (ac)= `<empty>`  (at /tmp/x.go:22)",
                        CollectedUsageFormatter.formatEntry("ac", "", "   ", CollectedLocationContext.EMPTY, "/tmp/x.go", 22),
                        "blank line usages should still be representable");
            }
        });

        run("usage block groups entries by category", new Runnable() {
            @Override
            public void run() {
                java.util.Map<String, java.util.List<String>> grouped = new java.util.LinkedHashMap<String, java.util.List<String>>();
                java.util.List<String> calls = new java.util.ArrayList<String>();
                calls.add("- (aa)= [call] `foo()`  (at /tmp/x.go:1)");
                grouped.put("call", calls);
                java.util.List<String> reads = new java.util.ArrayList<String>();
                reads.add("- (ab)= [read] `value`  (at /tmp/x.go:2)");
                grouped.put("read", reads);
                assertEquals("- Usages:\n- [call]\n  - (aa)= [call] `foo()`  (at /tmp/x.go:1)\n- [read]\n  - (ab)= [read] `value`  (at /tmp/x.go:2)\n",
                        CollectedUsageFormatter.formatBlock("Usages", grouped),
                        "usage blocks should emit category headings");
            }
        });

        run("usage block keeps descriptive title", new Runnable() {
            @Override
            public void run() {
                java.util.Map<String, java.util.List<String>> grouped = new java.util.LinkedHashMap<String, java.util.List<String>>();
                java.util.List<String> refs = new java.util.ArrayList<String>();
                refs.add("- (aa)= [reference] `loadWidgetIds()`  (at /tmp/x.go:1)");
                grouped.put("reference", refs);
                assertEquals("- Usages (method `loadWidgetIds`):\n- [reference]\n  - (aa)= [reference] `loadWidgetIds()`  (at /tmp/x.go:1)\n",
                        CollectedUsageFormatter.formatBlock("Usages (method `loadWidgetIds`)", grouped),
                        "usage title should preserve the described target");
            }
        });

        run("usage section block keeps entries on separate indented lines", new Runnable() {
            @Override
            public void run() {
                assertEquals("- Usages:\n  - [call]\n    - (aa)= [call] `foo()`  (at /tmp/x.go:1)",
                        CollectedUsageFormatter.formatSectionBlock("- Usages:\n- [call]\n  - (aa)= [call] `foo()`  (at /tmp/x.go:1)\n"),
                        "usage section block should keep one usage per line with indentation");
            }
        });

        run("usage title extracts name from presentation", new Runnable() {
            @Override
            public void run() {
                assertEquals("Method fetchWidgetMap",
                        CollectedUsageFormatter.describeUsagePresentation(
                                "Method",
                                "fetchWidgetMap",
                                "fetchWidgetMap in All Places",
                                ""
                        ),
                        "usage title should combine kind and symbol name from the usage presentation");
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
