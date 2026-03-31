package org.hunmr.argument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ArgumentList {
    private final int _openOffset;
    private final int _closeOffset;
    private final List<ArgumentCandidate> _arguments;

    public ArgumentList(int openOffset, int closeOffset, List<ArgumentCandidate> arguments) {
        _openOffset = openOffset;
        _closeOffset = closeOffset;
        _arguments = Collections.unmodifiableList(new ArrayList<ArgumentCandidate>(arguments));
    }

    public int getOpenOffset() {
        return _openOffset;
    }

    public int getCloseOffset() {
        return _closeOffset;
    }

    public List<ArgumentCandidate> getArguments() {
        return _arguments;
    }

    public boolean contains(int offset) {
        return offset >= _openOffset && offset <= _closeOffset;
    }
}
