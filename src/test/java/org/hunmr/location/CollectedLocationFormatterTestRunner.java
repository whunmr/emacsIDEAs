package org.hunmr.location;

public final class CollectedLocationFormatterTestRunner {
    public static void main(String[] args) {
        run("labels start at a", new Runnable() {
            @Override
            public void run() {
                assertEquals("a", CollectedLocationFormatter.nextLabel(""), "first label should be a");
            }
        });

        run("labels continue after z", new Runnable() {
            @Override
            public void run() {
                StringBuilder builder = new StringBuilder();
                for (char c = 'a'; c <= 'z'; c++) {
                    builder.append("- <").append(c).append(">= x\n");
                }
                assertEquals("aa", CollectedLocationFormatter.nextLabel(builder.toString()), "label after z should be aa");
            }
        });

        run("labels continue after az", new Runnable() {
            @Override
            public void run() {
                assertEquals("ba", CollectedLocationFormatter.nextLabel("- <az>= x\n"), "label after az should be ba");
            }
        });

        run("append adds newline", new Runnable() {
            @Override
            public void run() {
                assertEquals("- <a>= x\n", CollectedLocationFormatter.appendEntry("", "- <a>= x"), "entry should end with a newline");
            }
        });

        run("context template initializes around first entry", new Runnable() {
            @Override
            public void run() {
                assertEquals("Context:\n\n- <a>= x\n\nTask:\n- \n\nConstraints:\n- \n",
                        CollectedPromptFormatter.appendToContext("", "- <a>= x\n"),
                        "first collected entry should be inserted into the Context section");
            }
        });

        run("prompt header is prepended before template", new Runnable() {
            @Override
            public void run() {
                assertEquals("My prompt header\n\nContext:\n\nTask:\n- \n\nConstraints:\n- \n",
                        CollectedPromptFormatter.withPromptHeader("", "My prompt header"),
                        "prompt header should be added before the generated template");
            }
        });

        run("prompt header is not duplicated", new Runnable() {
            @Override
            public void run() {
                String existing = "My prompt header\n\nContext:\n\n- <a>= first\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals(existing,
                        CollectedPromptFormatter.withPromptHeader(existing, "My prompt header"),
                        "prompt header should not be inserted twice");
            }
        });

        run("context append inserts before task section", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n- <a>= first\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals("Context:\n\n- <a>= first\n- <b>= second\n\nTask:\n- do it\n\nConstraints:\n- keep api\n",
                        CollectedPromptFormatter.appendToContext(existing, "- <b>= second\n"),
                        "new context entries should appear before Task");
            }
        });

        run("context append keeps entries before call hierarchy section", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n[Call Hierarchy]\n- ```Call hierarchy```" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals("Context:\n\n- <a>= first\n\n[Call Hierarchy]\n- ```Call hierarchy```" +
                                "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n",
                        CollectedPromptFormatter.appendToContext(existing, "- <a>= first\n"),
                        "locations should be inserted before the call hierarchy section");
            }
        });

        run("context section append initializes call hierarchy section", new Runnable() {
            @Override
            public void run() {
                assertEquals("Context:\n\n[Call Hierarchy]\n- ```Call hierarchy```" +
                                "\n\nTask:\n- \n\nConstraints:\n- \n",
                        CollectedPromptFormatter.appendToContextSection("", "[Call Hierarchy]", "- ```Call hierarchy```"),
                        "call hierarchy section should be created inside Context");
            }
        });

        run("context section append initializes usages before call hierarchy", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n[Call Hierarchy]\n- ```Call hierarchy```" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals("Context:\n\n[Usages]\n- ```Usages```\n\n[Call Hierarchy]\n- ```Call hierarchy```" +
                                "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n",
                        CollectedPromptFormatter.appendToContextSection(existing, "[Usages]", "- ```Usages```"),
                        "usages section should be inserted before call hierarchy");
            }
        });

        run("context section append appends to existing section", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n- <a>= first\n\n[Call Hierarchy]\n- ```one```" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals("Context:\n\n- <a>= first\n\n[Call Hierarchy]\n- ```one```\n- ```two```" +
                                "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n",
                        CollectedPromptFormatter.appendToContextSection(existing, "[Call Hierarchy]", "- ```two```"),
                        "new hierarchy items should append inside the existing section");
            }
        });

        run("context section append separates multiline usage blocks", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n[Usages]\nUsages:\n  [call]\n  - (aa)= foo" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals("Context:\n\n[Usages]\nUsages:\n  [call]\n  - (aa)= foo\n\nUsages:\n  [read]\n  - (ab)= bar" +
                                "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n",
                        CollectedPromptFormatter.appendToContextSection(existing, "[Usages]", "Usages:\n  [read]\n  - (ab)= bar"),
                        "multiline usage blocks should be separated by a blank line");
            }
        });

        run("context section append separates multiline call hierarchy blocks", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n[Call Hierarchy]\nCall hierarchy:\n  [incoming callers]\n  [caller]= foo" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                assertEquals("Context:\n\n[Call Hierarchy]\nCall hierarchy:\n  [incoming callers]\n  [caller]= foo\n\nCall hierarchy:\n  [outgoing callees]\n  [callee]= bar" +
                                "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n",
                        CollectedPromptFormatter.appendToContextSection(existing, "[Call Hierarchy]", "Call hierarchy:\n  [outgoing callees]\n  [callee]= bar"),
                        "multiline call hierarchy blocks should be separated by a blank line");
            }
        });

        run("context section duplicate detection supports multiline call hierarchy blocks", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n[Call Hierarchy]\n- Call hierarchy for function `buildPlan`:\n" +
                        "  - [incoming callers]\n" +
                        "    - [caller depth-1] function `runPlan` in file `dummy.go`  (at /tmp/dummy.go:7)" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                String block = "- Call hierarchy for function `buildPlan`:\n" +
                        "  - [incoming callers]\n" +
                        "    - [caller depth-1] function `runPlan` in file `dummy.go`  (at /tmp/dummy.go:7)";
                assertTrue(
                        CollectedPromptFormatter.contextSectionContainsLine(existing, "[Call Hierarchy]", block),
                        "multiline call hierarchy block duplicates should be detected"
                );
            }
        });

        run("context section duplicate detection supports multiline usage blocks", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n[Usages]\n- Usages (Method buildPlan):\n" +
                        "  - [reference]\n" +
                        "    - (aa)= [reference] `runPlan()` located in { function `buildPlan` }  (at /tmp/dummy.go:9)" +
                        "\n\nTask:\n- do it\n\nConstraints:\n- keep api\n";
                String block = "- Usages (Method buildPlan):\n" +
                        "  - [reference]\n" +
                        "    - (aa)= [reference] `runPlan()` located in { function `buildPlan` }  (at /tmp/dummy.go:9)";
                assertTrue(
                        CollectedPromptFormatter.contextSectionContainsLine(existing, "[Usages]", block),
                        "multiline usage block duplicates should be detected"
                );
            }
        });

        run("single line entry includes content and line", new Runnable() {
            @Override
            public void run() {
                assertEquals("- <a>= `abc`  (at /tmp/x.java:32)",
                        CollectedLocationFormatter.formatEntry("a", "abc", "/tmp/x.java", 32, 32, false),
                        "single line entry should include inline content");
            }
        });

        run("multiline entry uses two-line fenced preview", new Runnable() {
            @Override
            public void run() {
                assertEquals("- <b>= ```line1\\n line2```  (at /tmp/x.java:32-35)",
                        CollectedLocationFormatter.formatEntry("b", "line1\nline2\nline3", "/tmp/x.java", 32, 35, false),
                        "multiline entry should keep first two lines only");
            }
        });

        run("empty selection falls back to location only", new Runnable() {
            @Override
            public void run() {
                assertEquals("- <c>= (at /tmp/x.java:9)",
                        CollectedLocationFormatter.formatEntry("c", "", "/tmp/x.java", 9, 9, false),
                        "no-selection entry should only contain location");
            }
        });

        run("full file selection uses path only", new Runnable() {
            @Override
            public void run() {
                assertEquals("- <d>= `abc`  (at /tmp/x.java)",
                        CollectedLocationFormatter.formatEntry("d", "abc", "/tmp/x.java", 1, 1, true),
                        "full file selection should use path only");
            }
        });

        run("symbol entry includes kind and container", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("method", "handleShowMarkersKey", "class", "Main");
                assertEquals("- <e>= method `handleShowMarkersKey` in class `Main`  (at /tmp/x.java:78)",
                        CollectedLocationFormatter.formatEntry("e", context, "", "/tmp/x.java", 78, 78, false),
                        "symbol entry should prefer symbol kind and container");
            }
        });

        run("symbol entry omits snippet fallback", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("attribute", "inheritedJdk", "tag", "orderEntry");
                assertEquals("- <f>= attribute `inheritedJdk` in tag `orderEntry`  (at /tmp/x.iml:8)",
                        CollectedLocationFormatter.formatEntry("f", context, "inheritedJdk", "/tmp/x.iml", 8, 8, false),
                        "symbol context should replace raw text preview");
            }
        });

        run("symbol entry includes selected single line when description does not contain it", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("method", "handleShowMarkersKey", "class", "Main");
                assertEquals("- <g>= `selected_content` located in { method `handleShowMarkersKey` in class `Main` }   (at /tmp/x.java:78)",
                        CollectedLocationFormatter.formatEntry("g", context, "selected_content", "/tmp/x.java", 78, 78, false),
                        "selected text should be emphasized when symbol description does not contain it");
            }
        });

        run("symbol entry keeps original symbol description when it already contains selected text", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("attribute", "inheritedJdk", "tag", "orderEntry");
                assertEquals("- <h>= attribute `inheritedJdk` in tag `orderEntry`  (at /tmp/x.iml:8)",
                        CollectedLocationFormatter.formatEntry("h", context, "inheritedJdk", "/tmp/x.iml", 8, 8, false),
                        "matching selected text should not be duplicated");
            }
        });

        run("symbol entry includes first three lines of multiline selection", new Runnable() {
            @Override
            public void run() {
                CollectedLocationContext context = new CollectedLocationContext("function", "handleJump", "package", "main");
                assertEquals("- <i>= ```line1\\n line2\\n line3``` located in { function `handleJump` in package `main` }   (at /tmp/x.go:12-18)",
                        CollectedLocationFormatter.formatEntry("i", context, "line1\nline2\nline3\nline4", "/tmp/x.go", 12, 18, false),
                        "multiline selected text should be limited to the first three lines");
            }
        });

        run("location duplicate ignores changing label", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n- <a>= `abc`  (at /tmp/x.java:32)\n\nTask:\n- \n\nConstraints:\n- \n";
                assertTrue(CollectedLocationFormatter.containsDuplicate(existing, "- <b>= `abc`  (at /tmp/x.java:32)"),
                        "duplicate check should ignore the auto label");
            }
        });

        run("location duplicate ignores reasons", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n- <a>= struct `WidgetCache` in package `pkg/cache`  (at /tmp/cache.go:18) \\n reason: field type of `WidgetService.widgetCache`\n\nTask:\n- \n\nConstraints:\n- \n";
                assertTrue(CollectedLocationFormatter.containsDuplicate(
                                existing,
                                "- <b>= struct `WidgetCache` in package `pkg/cache`  (at /tmp/cache.go:18) \\n reason: referenced variable type"
                        ),
                        "duplicate check should ignore reason text");
            }
        });

        run("merge duplicate entry adds first reason when missing", new Runnable() {
            @Override
            public void run() {
                String existing = "Context:\n\n- <a>= struct `widgetChannelInfo` in package `pkg/stats`  (at /tmp/stats.go:250)\n\nTask:\n- \n\nConstraints:\n- \n";
                String merged = CollectedLocationFormatter.mergeDuplicateEntry(
                        existing,
                        "- <b>= struct `widgetChannelInfo` in package `pkg/stats`  (at /tmp/stats.go:250) \\n reason: named element type \\n reason: referenced variable type"
                );
                assertEquals("Context:\n\n- <a>= struct `widgetChannelInfo` in package `pkg/stats`  (at /tmp/stats.go:250) \\n reason: named element type\n\nTask:\n- \n\nConstraints:\n- \n",
                        merged,
                        "first reason should be merged into the existing duplicate entry");
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

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
