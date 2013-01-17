package org.hunmr.acejump.marker;

import java.util.HashMap;

public class MarkerCollection extends HashMap<Character, Marker> {

    public boolean keyMappingToMultipleMarkers(char key) {
        Marker marker = this.get(key);
        return marker != null && marker.isMappingToMultipleOffset();
    }

    public void addMarker(char key, Integer offset) {
        Marker marker = this.get(key);
        if (marker == null) {
            this.put(key, new Marker(key, offset));
            return;
        }

        marker.addOffsetToMarker(offset);
    }
}
