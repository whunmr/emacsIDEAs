package org.hunmr.acejump.runnable;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.marker.JOffset;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ShowMarkersRunnable implements Runnable {
    public static final char INFINITE_JUMP_CHAR = '/';
    private static final String MARKER_CHARSET =     "asdfjeghiybcmnopqrtuvwkl";   //TODO: customizable
    private final List<JOffset> _offsets;
    private final AceJumpAction _action;
    private final Editor _editor;
    private final ArrayList<Editor> _editors;
    private MarkerCollection _markerCollection;

    public ShowMarkersRunnable(List<JOffset> offsets, AceJumpAction currentExecutingAction) {
        _offsets = offsets;
        _editor = currentExecutingAction.getEditor();
        _editors = currentExecutingAction.getEditors();
        this._action = currentExecutingAction;
        _markerCollection = _action.getMarkerCollection();
    }

    @Override
    public void run() {
        if (_action == null || _editor == null || _editor.isDisposed() || _markerCollection == null || _editors == null) {
            return;
        }

        ArrayList<JOffset> usableOffsets = collectUsableOffsets();
        if (usableOffsets.isEmpty()) {
            return;
        }

        int caretOffset = _editor.getCaretModel().getOffset();
        sortOffsetsByDistanceToCaret(usableOffsets, caretOffset);
        sortOffsetsToImprovePriorityOfLineEnd(usableOffsets);

        int twiceJumpGroupCount = calcTwiceJumpGroupCount(usableOffsets.size());
        int singleJumpCount = Math.min(MARKER_CHARSET.length() - twiceJumpGroupCount, usableOffsets.size());

        createSingleJumpMarkers(usableOffsets, singleJumpCount);
        if (twiceJumpGroupCount > 0) {
            createMultipleJumpMarkers(usableOffsets, singleJumpCount);
        }

        ArrayList<MarkersPanel> panels = new ArrayList<MarkersPanel>();
        for (Editor editor : _editors) {
            if (editor != null && !editor.isDisposed()) {
                MarkersPanel markersPanel = new MarkersPanel(editor, _markerCollection);
                panels.add(markersPanel);
            }
        }

        if (!panels.isEmpty()) {
            _action.showNewMarkersPanel(panels);
        }
    }

    private ArrayList<JOffset> collectUsableOffsets() {
        ArrayList<JOffset> usableOffsets = new ArrayList<JOffset>();
        if (_offsets == null) {
            return usableOffsets;
        }

        for (JOffset offset : _offsets) {
            if (offset != null && offset.editor != null && !offset.editor.isDisposed()) {
                usableOffsets.add(offset);
            }
        }
        return usableOffsets;
    }

    private void createSingleJumpMarkers(List<JOffset> offsets, int singleJumpCount) {
        for (int i = 0; i < singleJumpCount ; i++) {
            String marker = String.valueOf(MARKER_CHARSET.charAt(i));
            _markerCollection.addMarker(marker, offsets.get(i));
        }
    }

    private void createMultipleJumpMarkers(List<JOffset> offsets, int singleJumpCount) {
        int i = singleJumpCount;

        for (;i < offsets.size(); i++) {
            int group = (i - singleJumpCount) / MARKER_CHARSET.length();
            int markerCharIndex = singleJumpCount + group;

            if (markerCharIndex > MARKER_CHARSET.length() - 1) {
                break;
            }

            char markerChar = MARKER_CHARSET.charAt(markerCharIndex);
            char secondJumpMarkerChar = MARKER_CHARSET.charAt((i - singleJumpCount) % MARKER_CHARSET.length());

            String marker = "" + markerChar + secondJumpMarkerChar;
            _markerCollection.addMarker(marker, offsets.get(i));
        }


        boolean hasMarkersNeedMoreJumps = i < offsets.size();
        if (hasMarkersNeedMoreJumps) {
            for (; i < offsets.size(); i++) {
                _markerCollection.addMarker(String.valueOf(INFINITE_JUMP_CHAR), offsets.get(i));
            }
        }
    }

    private int calcTwiceJumpGroupCount(int offsetCount) {
        int makerCharSetSize = MARKER_CHARSET.length();

        for (int groupsNeedMultipleJump = 0; groupsNeedMultipleJump <= makerCharSetSize; groupsNeedMultipleJump++) {
            int oneJumpMarkerCount = makerCharSetSize - groupsNeedMultipleJump;
            if (groupsNeedMultipleJump * makerCharSetSize + oneJumpMarkerCount >= offsetCount) {
                return groupsNeedMultipleJump;
            }
        }

        return makerCharSetSize;
    }

    private void sortOffsetsByDistanceToCaret(final List<JOffset> offsets, final int caretOffset) {
        Collections.sort(offsets, new Comparator<JOffset>() {
            @Override
            public int compare(JOffset joA, JOffset joB) {
                if (joA == null || joA.editor == null) {
                    return joB == null || joB.editor == null ? 0 : -1;
                }
                if (joB == null || joB.editor == null) {
                    return 1;
                }

                if (joA.editor != joB.editor) {
                    return Integer.compare(joA.editor.hashCode(), joB.editor.hashCode());
                }

                Integer oA = joA.offset;
                Integer oB = joB.offset;

                int distA = Math.abs(oA - caretOffset);
                int distB = Math.abs(oB - caretOffset);

                if (distA == distB) {
                    return oA - oB;
                }

                return distA - distB;
            }
        });
    }

    private void sortOffsetsToImprovePriorityOfLineEnd(final List<JOffset> offsets) {
        Collections.sort(offsets, new Comparator<JOffset>() {
            @Override
            public int compare(JOffset joA, JOffset joB) {
                if (joA == null || joA.editor == null) {
                    return joB == null || joB.editor == null ? 0 : -1;
                }
                if (joB == null || joB.editor == null) {
                    return 1;
                }

                if (joA.editor != joB.editor) {
                    return Integer.compare(joA.editor.hashCode(), joB.editor.hashCode());
                }

                Document document = joA.editor.getDocument();
                boolean oAIsLineEndOffset = isLineEndOffset(joA.offset, document);
                boolean oBIsLineEndOffset = isLineEndOffset(joB.offset, document);

                if (!(oAIsLineEndOffset ^ oBIsLineEndOffset)) {
                    return 0;
                }

                return oAIsLineEndOffset ? -1 : 1;
            }

            private boolean isLineEndOffset(Integer oA, Document document) {
                int lineA = document.getLineNumber(oA);
                int lineEndOffset = document.getLineEndOffset(lineA);
                return oA == lineEndOffset;
            }
        });
    }
}
