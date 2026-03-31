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

        run("insert inside current argument appends after that argument", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(ab|c)", "X", "foo(abc, X)");
            }
        });

        run("insert at the start of the current argument prepends before it", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(a, |char aaa, b)", "X", "foo(a, X, char aaa, b)");
            }
        });

        run("insert inside a declaration-style argument appends after it", new Runnable() {
            @Override
            public void run() {
                assertInsertion("foo(a, c|har aaa, b)", "X", "foo(a, char aaa, X, b)");
            }
        });

        run("delete first argument cleans the comma", new Runnable() {
            @Override
            public void run() {
                assertDelete("foo(^a, b, c)", "foo(b, c)");
            }
        });

        run("delete middle argument cleans both sides", new Runnable() {
            @Override
            public void run() {
                assertDelete("foo(a, ^b, c)", "foo(a, c)");
            }
        });

        run("delete last argument removes the leading comma", new Runnable() {
            @Override
            public void run() {
                assertDelete("foo(a, b, ^c)", "foo(a, b)");
            }
        });

        run("delete only argument leaves an empty list", new Runnable() {
            @Override
            public void run() {
                assertDelete("foo(^only)", "foo()");
            }
        });

        run("delete argument with leading block comment", new Runnable() {
            @Override
            public void run() {
                assertDelete("foo(a, ^/*AAA, BBB*/ b, c)", "foo(a, c)");
            }
        });

        run("obtain target argument and replace the current argument", new Runnable() {
            @Override
            public void run() {
                assertObtainAndReplace("foo(|a, b); bar(^x, y)", "foo(x, b); bar(x, y)");
            }
        });

        run("replace target argument with the current argument", new Runnable() {
            @Override
            public void run() {
                assertAndReplace("foo(|a, b); bar(^x, y)", "foo(a, b); bar(a, y)");
            }
        });

        run("move target argument into an empty parameter list", new Runnable() {
            @Override
            public void run() {
                assertMove("foo(|); bar(^x, y)", "foo(x); bar(y)");
            }
        });

        run("move target argument before another argument in the same list", new Runnable() {
            @Override
            public void run() {
                assertMove("foo(a, | b, ^c)", "foo(a, c, b)");
            }
        });

        run("move target argument across nested calls", new Runnable() {
            @Override
            public void run() {
                assertMove("outer(|x, y, inner(a, ^b, c))", "outer(b, x, y, inner(a, c))");
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

    private static void assertDelete(String inputWithTargetMarker, String expectedOutput) {
        Markers markers = stripMarkers(inputWithTargetMarker);
        ParsedArguments parsed = ArgumentParser.parse(markers.text);
        ArgumentCandidate target = findTargetCandidate(parsed, markers);
        ArgumentInsertionPlan plan = ArgumentEditPlanner.planDelete(markers.text, target);
        assertEquals(expectedOutput, ArgumentEditPlanner.apply(markers.text, plan), "delete result should match");
    }

    private static void assertObtainAndReplace(String inputWithMarkers, String expectedOutput) {
        Markers markers = stripMarkers(inputWithMarkers);
        ParsedArguments parsed = ArgumentParser.parse(markers.text);
        ArgumentCandidate source = parsed.findArgumentAtOrNear(markers.sourceOffset);
        ArgumentCandidate target = findTargetCandidate(parsed, markers);
        String targetText = target.getText(markers.text);
        ArgumentInsertionPlan plan = ArgumentEditPlanner.planReplace(source, targetText);
        assertEquals(expectedOutput, ArgumentEditPlanner.apply(markers.text, plan), "obtain-and-replace result should match");
    }

    private static void assertAndReplace(String inputWithMarkers, String expectedOutput) {
        Markers markers = stripMarkers(inputWithMarkers);
        ParsedArguments parsed = ArgumentParser.parse(markers.text);
        ArgumentCandidate source = parsed.findArgumentAtOrNear(markers.sourceOffset);
        ArgumentCandidate target = findTargetCandidate(parsed, markers);
        String sourceText = source.getText(markers.text);
        ArgumentInsertionPlan plan = ArgumentEditPlanner.planReplace(target, sourceText);
        assertEquals(expectedOutput, ArgumentEditPlanner.apply(markers.text, plan), "replace result should match");
    }

    private static void assertMove(String inputWithMarkers, String expectedOutput) {
        Markers markers = stripMarkers(inputWithMarkers);
        ParsedArguments parsed = ArgumentParser.parse(markers.text);
        ArgumentCandidate target = findTargetCandidate(parsed, markers);
        String targetText = target.getText(markers.text);

        if (target.contains(markers.sourceOffset)) {
            throw new AssertionError("move tests must not place the source caret inside the target argument");
        }

        ArgumentInsertionPlan deletePlan = ArgumentEditPlanner.planDelete(markers.text, target);
        String textAfterDelete = ArgumentEditPlanner.apply(markers.text, deletePlan);
        int adjustedSourceOffset = ArgumentEditPlanner.adjustOffsetAfterPlan(markers.sourceOffset, deletePlan);
        ArgumentInsertionPlan insertPlan = ArgumentInsertionPlanner.plan(textAfterDelete, adjustedSourceOffset, targetText);
        String finalText = ArgumentEditPlanner.apply(textAfterDelete, insertPlan);

        assertEquals(expectedOutput, finalText, "move result should match");
    }

    private static Marker stripMarker(String textWithMarker) {
        int offset = textWithMarker.indexOf('|');
        if (offset < 0) {
            throw new AssertionError("missing caret marker in test text: " + textWithMarker);
        }

        String text = textWithMarker.substring(0, offset) + textWithMarker.substring(offset + 1);
        return new Marker(text, offset);
    }

    private static Markers stripMarkers(String markedText) {
        StringBuilder builder = new StringBuilder();
        int sourceOffset = -1;
        int targetOffset = -1;

        for (int i = 0; i < markedText.length(); i++) {
            char c = markedText.charAt(i);
            if (c == '|') {
                sourceOffset = builder.length();
                continue;
            }
            if (c == '^') {
                targetOffset = builder.length();
                continue;
            }
            builder.append(c);
        }

        return new Markers(builder.toString(), sourceOffset, targetOffset);
    }

    private static ArgumentCandidate findTargetCandidate(ParsedArguments parsed, Markers markers) {
        if (markers.targetOffset < 0) {
            throw new AssertionError("missing target marker in test");
        }

        ArgumentCandidate bestContaining = null;
        for (ArgumentCandidate candidate : parsed.getArguments()) {
            if (candidate.getAnchorOffset() == markers.targetOffset) {
                return candidate;
            }

            if (!candidate.contains(markers.targetOffset)) {
                continue;
            }

            if (bestContaining == null
                    || candidate.getRange().length() < bestContaining.getRange().length()) {
                bestContaining = candidate;
            }
        }

        if (bestContaining != null) {
            return bestContaining;
        }

        ArgumentCandidate candidate = parsed.findArgumentAtOrNear(markers.targetOffset);
        if (candidate == null) {
            throw new AssertionError("failed to resolve target candidate");
        }
        return candidate;
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

    private static final class Markers {
        private final String text;
        private final int sourceOffset;
        private final int targetOffset;

        private Markers(String text, int sourceOffset, int targetOffset) {
            this.text = text;
            this.sourceOffset = sourceOffset;
            this.targetOffset = targetOffset;
        }
    }
}
