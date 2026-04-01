package org.hunmr.location;

public final class CollectedLocationFormatterTestRunner {
    public static void main(String[] args) {
        run("labels start at a", new Runnable() {
            @Override
            public void run() {
                assertEquals("@a", CollectedLocationFormatter.nextLabel(""), "first label should be @a");
            }
        });

        run("labels continue after z", new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                for (char c = 'a'; c <= 'z'; c++) {
                    builder.append("@").append(c).append(" x\n");
                }
                assertEquals("@aa", CollectedLocationFormatter.nextLabel(builder.toString()), "label after @z should be @aa");
            }
        });

        run("labels continue after az", new Runnable() {
            @Override
            public void run() {
                assertEquals("@ba", CollectedLocationFormatter.nextLabel("@az x\n"), "label after @az should be @ba");
            }
        });

        run("append adds newline", new Runnable() {
            @Override
            public void run() {
                assertEquals("@a x\n", CollectedLocationFormatter.appendEntry("", "@a x"), "entry should end with a newline");
            }
        });

        run("single line entry includes content and line", new Runnable() {
            @Override
            public void run() {
                assertEquals("@a `abc` /tmp/x.java:32",
                        CollectedLocationFormatter.formatEntry("@a", "abc", "/tmp/x.java", 32, 32, false),
                        "single line entry should include inline content");
            }
        });

        run("multiline entry uses two-line fenced preview", new Runnable() {
            @Override
            public void run() {
                assertEquals("@b ```\nline1\nline2\n``` /tmp/x.java:32-35",
                        CollectedLocationFormatter.formatEntry("@b", "line1\nline2\nline3", "/tmp/x.java", 32, 35, false),
                        "multiline entry should keep first two lines only");
            }
        });

        run("empty selection falls back to location only", new Runnable() {
            @Override
            public void run() {
                assertEquals("@c /tmp/x.java:9",
                        CollectedLocationFormatter.formatEntry("@c", "", "/tmp/x.java", 9, 9, false),
                        "no-selection entry should only contain location");
            }
        });

        run("full file selection uses path only", new Runnable() {
            @Override
            public void run() {
                assertEquals("@d `abc` /tmp/x.java",
                        CollectedLocationFormatter.formatEntry("@d", "abc", "/tmp/x.java", 1, 1, true),
                        "full file selection should use path only");
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
