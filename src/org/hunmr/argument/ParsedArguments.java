package org.hunmr.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class ParsedArguments {
    private final String _text;
    private final List<ArgumentCandidate> _arguments;
    private final List<ArgumentList> _lists;

    public ParsedArguments(String text, List<ArgumentCandidate> arguments, List<ArgumentList> lists) {
        _text = text == null ? "" : text;
        _arguments = sortArguments(arguments);
        _lists = sortLists(lists);
    }

    public List<ArgumentCandidate> getArguments() {
        return _arguments;
    }

    public List<ArgumentList> getLists() {
        return _lists;
    }

    public ArgumentList findInnermostList(int offset) {
        ArgumentList best = null;
        for (ArgumentList list : _lists) {
            if (list == null || !list.contains(offset)) {
                continue;
            }

            if (best == null || widthOf(list) < widthOf(best)) {
                best = list;
            }
        }

        return best;
    }

    public ArgumentCandidate findArgumentAtOrNear(int offset) {
        ArgumentList list = findInnermostList(offset);
        if (list == null) {
            return null;
        }

        ArgumentCandidate best = null;
        int bestDistance = Integer.MAX_VALUE;

        for (ArgumentCandidate candidate : list.getArguments()) {
            if (candidate == null) {
                continue;
            }

            if (candidate.contains(offset)) {
                return candidate;
            }

            int distance = candidate.distanceTo(offset);
            if (distance < bestDistance) {
                best = candidate;
                bestDistance = distance;
            }
        }

        return best;
    }

    public String getText() {
        return _text;
    }

    private List<ArgumentCandidate> sortArguments(List<ArgumentCandidate> arguments) {
        ArrayList<ArgumentCandidate> sorted = new ArrayList<ArgumentCandidate>(arguments);
        Collections.sort(sorted, new Comparator<ArgumentCandidate>() {
            @Override
            public int compare(ArgumentCandidate left, ArgumentCandidate right) {
                if (left == null || left.getRange() == null) {
                    return right == null || right.getRange() == null ? 0 : -1;
                }
                if (right == null || right.getRange() == null) {
                    return 1;
                }

                int startCompare = Integer.compare(left.getRange().getStartOffset(), right.getRange().getStartOffset());
                if (startCompare != 0) {
                    return startCompare;
                }

                return Integer.compare(left.getRange().getEndOffset(), right.getRange().getEndOffset());
            }
        });
        return Collections.unmodifiableList(sorted);
    }

    private List<ArgumentList> sortLists(List<ArgumentList> lists) {
        ArrayList<ArgumentList> sorted = new ArrayList<ArgumentList>(lists);
        Collections.sort(sorted, new Comparator<ArgumentList>() {
            @Override
            public int compare(ArgumentList left, ArgumentList right) {
                if (left == null) {
                    return right == null ? 0 : -1;
                }
                if (right == null) {
                    return 1;
                }

                int startCompare = Integer.compare(left.getOpenOffset(), right.getOpenOffset());
                if (startCompare != 0) {
                    return startCompare;
                }

                return Integer.compare(widthOf(left), widthOf(right));
            }
        });
        return Collections.unmodifiableList(sorted);
    }

    private static int widthOf(ArgumentList list) {
        return list.getCloseOffset() - list.getOpenOffset();
    }
}
