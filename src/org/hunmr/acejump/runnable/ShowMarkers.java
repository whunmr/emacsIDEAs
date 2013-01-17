package org.hunmr.acejump.runnable;

import com.intellij.openapi.editor.Editor;
import org.hunmr.acejump.AceJumpAction;
import org.hunmr.acejump.marker.MarkerCollection;
import org.hunmr.acejump.marker.MarkersPanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ShowMarkers implements Runnable {
    private final String _markerCharSet = "abcdefghijklmnopqrstuvwxy"; //ABCDEFGHIJKLMNOPQRSTUVWXY
    private ArrayList<Integer> _offsets;
    private final AceJumpAction _action;
    private final Editor _editor;

    public ShowMarkers(ArrayList<Integer> offsets, AceJumpAction currentExecutingAction) {
        _offsets = offsets;
        this._action = currentExecutingAction;
        _editor = currentExecutingAction.getEditor();
    }

    @Override
    public void run() {
        int totalOffsetsCount = _offsets.size();
        if (totalOffsetsCount == 0) {
            return;
        }

        sortAllJumpTargetOffsetsByDistanceToCaret(_offsets, _editor.getCaretModel().getOffset());

        MarkerCollection markerCollection = _action.getMarkerCollection();

        int groupsNeedMultipleJump = determineMultipleJumpGroupCount(totalOffsetsCount);

        int singleJumpCount = _markerCharSet.length() - groupsNeedMultipleJump;
        for (int i = 0; i < Math.min(singleJumpCount,totalOffsetsCount) ; i++) {
            char markerChar = _markerCharSet.charAt(i);
            markerCollection.addMarker(markerChar, _offsets.get(i));
        }

        for (int j = singleJumpCount; j < totalOffsetsCount; j++) {
            int group = (j - singleJumpCount) / _markerCharSet.length();
            char markerChar = _markerCharSet.charAt(singleJumpCount + group);
            markerCollection.addMarker(markerChar, _offsets.get(j));
        }

        MarkersPanel markersPanel = new MarkersPanel(_editor, markerCollection);
        _action.showNewMarkersPanel(markersPanel);
    }

    private int determineMultipleJumpGroupCount(int totalOffsetsCount) {
        int groupsNeedMultipleJump;
        int makerCharSetSize = _markerCharSet.length();

        for (groupsNeedMultipleJump = 0; groupsNeedMultipleJump <= makerCharSetSize; groupsNeedMultipleJump++) {
            int oneJumpMarkerCount = makerCharSetSize - groupsNeedMultipleJump;
            if (groupsNeedMultipleJump * makerCharSetSize + oneJumpMarkerCount > totalOffsetsCount) {
                break;
            }
        }

        return groupsNeedMultipleJump;
    }

    private void sortAllJumpTargetOffsetsByDistanceToCaret(ArrayList<Integer> offsets, final int caretOffset) {
        Collections.sort(offsets, new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return Math.abs(o1 - caretOffset) - Math.abs(o2 - caretOffset);
            }
        });
    }
}
