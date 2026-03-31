package org.hunmr.argument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class ArgumentParserTestRunner {
    public static void main(String[] args) {
        run("simple call arguments", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "detachKeyListener(AAA, BBB, CCC)",
                        "AAA",
                        "BBB",
                        "CCC"
                );
            }
        });

        run("method declaration arguments", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "private void detachKeyListener(A keyEvent, B keyEvent, C keyListener) {}",
                        "A keyEvent",
                        "B keyEvent",
                        "C keyListener"
                );
            }
        });

        run("nested function arguments", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "funcA(B, C, funcD(E, F, G, funcH(I, J)))",
                        "B",
                        "C",
                        "funcD(E, F, G, funcH(I, J))",
                        "E",
                        "F",
                        "G",
                        "funcH(I, J)",
                        "I",
                        "J"
                );
            }
        });

        run("generic types stay in one argument", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "foo(Map<String, List<Integer>>, x)",
                        "Map<String, List<Integer>>",
                        "x"
                );
            }
        });

        run("comparisons with spaces do not look like generics", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "foo(a < b, c > d)",
                        "a < b",
                        "c > d"
                );
            }
        });

        run("lambda expression keeps nested call intact", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "foo(a, x -> bar(y, z))",
                        "a",
                        "x -> bar(y, z)",
                        "y",
                        "z"
                );
            }
        });

        run("annotation array and ternary stay intact", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "foo(@Anno(a, b) value, new int[]{a, b}, x ? y : z)",
                        "@Anno(a, b) value",
                        "a",
                        "b",
                        "new int[]{a, b}",
                        "x ? y : z"
                );
            }
        });

        run("string and raw string commas are ignored", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "foo(\"A, B)\", 'x', `raw, )`, z)",
                        "\"A, B)\"",
                        "'x'",
                        "`raw, )`",
                        "z"
                );
            }
        });

        run("comments do not split arguments", new Runnable() {
            @Override
            public void run() {
                assertArgumentTexts(
                        "funcX(x,  /*AAA, BBB, CCC*/ y, z)",
                        "x",
                        "/*AAA, BBB, CCC*/ y",
                        "z"
                );
            }
        });

        run("anchor skips leading comment trivia", new Runnable() {
            @Override
            public void run() {
                String text = "funcX(x,  /*AAA, BBB, CCC*/ y, z)";
                ParsedArguments parsed = ArgumentParser.parse(text);
                ArgumentCandidate candidate = parsed.getArguments().get(1);
                assertEquals('y', text.charAt(candidate.getAnchorOffset()), "anchor should land on the first real token");
            }
        });

        run("selector query prefers the nearest argument", new Runnable() {
            @Override
            public void run() {
                Marker marker = stripMarker("foo(a,  |b, c)");
                ParsedArguments parsed = ArgumentParser.parse(marker.text);
                ArgumentCandidate candidate = parsed.findArgumentAtOrNear(marker.offset);
                assertEquals("b", candidate.getText(marker.text), "caret should resolve to the next argument");
            }
        });

        run("insert into empty argument list", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(|)", "bar", "foo(bar)");
            }
        });

        run("insert before first argument", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(|b)", "bar", "foo(bar, b)");
            }
        });

        run("insert between arguments after comma", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(a, |b)", "bar", "foo(a, bar, b)");
            }
        });

        run("insert before closing parenthesis", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(a, b |)", "bar", "foo(a, b, bar)");
            }
        });

        run("plain insert outside argument list", new Runnable() {
            @Override
            public void run() {
                assertInsertion("value = |x", "bar", "value = barx");
            }
        });

        run("plain insert inside a token falls back safely", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(ab|c)", "X", "foo(abXc)");
            }
        });
    }

    private static void assertArgumentTexts(String text, String... expected) {
        ParsedArguments parsed = ArgumentParser.parse(text);
        ArrayList<String> actual = new ArrayList<String>();
        for (ArgumentCandidate candidate : parsed.getArguments()) {
            actual.add(candidate.getText(text));
        }
        assertListEquals(Arrays.asList(expected), actual, "argument texts should match");
    }

    private static void assertInsertion(String inputWithCaret, String argumentText, String expectedOutput) {
        Marker marker = stripMarker(inputWithCaret);
        ArgumentInsertionPlan plan = ArgumentInsertionPlanner.plan(marker.text, marker.offset, argumentText);

        StringBuilder builder = new StringBuilder(marker.text);
        builder.replace(plan.getReplaceStartOffset(), plan.getReplaceEndOffset(), plan.getInsertedText());

        assertEquals(expectedOutput, builder.toString(), "insertion result should match");
    }

    private static Marker stripMarker(String textWithMarker) {
        int offset = textWithMarker.indexOf('|');
        if (offset < 0) {
            throw new AssertionError("missing caret marker in test text: " + textWithMarker);
        }

        String text = textWithMarker.substring(0, offset) + textWithMarker.substring(offset + 1);
        return new Marker(text, offset);
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
            System.out.println("PASS " + name);
        } catch (AssertionError e) {
            System.err.println("FAIL " + name);
            throw e;
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(message + "\nexpected: " + expected + "\nactual:   " + actual);
        }
    }

    private static void assertListEquals(List<String> expected, List<String> actual, String message) {
        if (expected.size() != actual.size()) {
            throw new AssertionError(message
                    + "\nexpected size: " + expected.size()
                    + "\nactual size:   " + actual.size()
                    + "\nexpected:      " + describe(expected)
                    + "\nactual:        " + describe(actual));
        }

        for (int i = 0; i < expected.size(); i++) {
            String expectedValue = expected.get(i);
            String actualValue = actual.get(i);
            if (expectedValue == null ? actualValue != null : !expectedValue.equals(actualValue)) {
                throw new AssertionError(message
                        + "\nindex:    " + i
                        + "\nexpected: [" + expectedValue + "]"
                        + "\nactual:   [" + actualValue + "]");
            }
        }
    }

    private static String describe(List<String> values) {
        StringBuilder builder = new StringBuilder();
        builder.append('[');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(i).append(':').append('[').append(values.get(i)).append(']');
        }
        builder.append(']');
        return builder.toString();
    }

    private static final class Marker {
        private final String text;
        private final int offset;

        private Marker(String text, int offset) {
            this.text = text;
            this.offset = offset;
        }
    }
}
