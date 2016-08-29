package org.hunmr.acejump.marker;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import org.hunmr.options.PluginConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.*;

public class MarkersPanel extends JComponent {
    public static final Color PANEL_BACKGROUND_COLOR = new Color(128, 138, 142);
    public Editor _editor;
    private MarkerCollection _markerCollection;
    //private JComponent _parent;
    //private Font _fontInEditor;

    final PluginConfig _config = ServiceManager.getService(PluginConfig.class);

    public MarkersPanel(Editor editor, MarkerCollection markerCollection) {
        _editor = editor;
        _markerCollection = markerCollection;
        //_parent = editor.getContentComponent();
        setupLocationAndBoundsOfPanel(editor);
    }

    private boolean isLineEndOffset(Marker marker) {
        int offset = marker.getOffset().offset;
        Editor editor = marker.getOffset().editor;

        int lineA = editor.getDocument().getLineNumber(offset);
        int lineEndOffset = editor.getDocument().getLineEndOffset(lineA);

        return offset == lineEndOffset;
    }

    @Override
    public void paint(Graphics g) {
        drawPanelBackground(g); //TODO: draw panel in editors

        HashSet<JOffset> firstJumpOffsets = new HashSet<JOffset>();

        for (Marker marker : _markerCollection.values()) {
            if (marker.getOffset().editor != _editor) {
                continue;
            }

            for (JOffset offset : marker.getOffsets()) {
                firstJumpOffsets.add(offset);

                Point parentLocation = offset.editor.getContentComponent().getLocation();
                double x = getVisiblePosition(offset).getX() + parentLocation.getX();
                double y = getVisiblePosition(offset).getY() + parentLocation.getY();

                drawBackgroundOfMarupChar(g, marker, x, y);
                drawMarkerChar(g, marker, x, y);
            }
        }

        for (Marker marker : _markerCollection.values()) {
            if (marker.getOffset().editor != _editor) {
                continue;
            }

            if (marker.getMarker().length() == 1 || marker.isMappingToMultipleOffset()) {
                continue;
            }

            JOffset o = new JOffset(marker.getOffset().editor, marker.getOffset().offset + 1);
            boolean alreadyHasFirstJumpCharInPlace = firstJumpOffsets.contains(o);

            boolean isAtLineEnd = isLineEndOffset(marker);

            if (alreadyHasFirstJumpCharInPlace && !isAtLineEnd) {
                continue;
            }

            for (JOffset offset : marker.getOffsets()) {
                Point parentLocation = offset.editor.getContentComponent().getLocation();
                double x = getVisiblePosition(offset).getX() + parentLocation.getX();
                double y = getVisiblePosition(offset).getY() + parentLocation.getY();
                drawBackgroundOfSecondMarupChar(g, marker, x, y);
                drawSecondMarkerChar(g, marker, x, y);
            }
        }

        super.paint(g);
    }

    private void drawMarkerChar(Graphics g, Marker marker, double x, double y) {
        Font font = marker.getOffset().editor.getColorsScheme().getFont(EditorFontType.BOLD);
        g.setFont(font); //todo: set font for each editor
        float buttomYOfMarkerChar = (float) (y + font.getSize());

        if (marker.isMappingToMultipleOffset()) {
            g.setColor(Color.BLACK);
        } else {
            g.setColor(_config.getFirstJumpForeground());
        }

        ((Graphics2D)g).drawString(String.valueOf(marker.getMarkerChar()), (float)x, buttomYOfMarkerChar);
    }

    private void drawSecondMarkerChar(Graphics g, Marker marker, double x, double y) {
        Font font = marker.getOffset().editor.getColorsScheme().getFont(EditorFontType.BOLD);
        float buttomYOfMarkerChar = (float) (y + font.getSize());

        String markerStr = marker.getMarker();
        if (markerStr.length() > 1) {
            g.setColor(_config.getSecondJumpForeground());
            JComponent parent = marker.getOffset().editor.getContentComponent();
            Rectangle2D fontRect = parent.getFontMetrics(font).getStringBounds(String.valueOf(marker.getMarkerChar()), g);
            ((Graphics2D)g).drawString(String.valueOf(markerStr.charAt(1)), (float)(x + fontRect.getWidth()), buttomYOfMarkerChar);
        }
    }

    private void drawBackgroundOfMarupChar(Graphics g, Marker marker, double x, double y) {
        //Rectangle2D fontRect = _parent.getFontMetrics(_fontInEditor).getStringBounds(String.valueOf(marker.getMarkerChar()), g);
        Editor editor = marker.getOffset().editor;
        JComponent parent = editor.getContentComponent();
        Font font = editor.getColorsScheme().getFont(EditorFontType.BOLD);
        Rectangle2D fontRect = parent.getFontMetrics(font).getMaxCharBounds(g);

        if (marker.isMappingToMultipleOffset()) {
            g.setColor(Color.YELLOW);
        } else {
            g.setColor(_config.getFirstJumpBackground());
        }

        g.fillRect((int)x, (int)y, (int) fontRect.getWidth(), (int) (fontRect.getHeight() * 1.08));
    }


    private void drawBackgroundOfSecondMarupChar(Graphics g, Marker marker, double x, double y) {
        //Rectangle2D fontRect = _parent.getFontMetrics(_fontInEditor).getStringBounds(String.valueOf(marker.getMarkerChar()), g);
        Editor editor = marker.getOffset().editor;
        JComponent parent = editor.getContentComponent();
        Font font = editor.getColorsScheme().getFont(EditorFontType.BOLD);
        Rectangle2D fontRect = parent.getFontMetrics(font).getMaxCharBounds(g);

        g.setColor(_config.getSecondJumpBackground());
        String markerStr = marker.getMarker();
        if (markerStr.length() > 1) {
            g.fillRect((int)(x + fontRect.getWidth()), (int)y, (int) fontRect.getWidth(), (int) (fontRect.getHeight() * 1.08));
        }
    }

    private Point getVisiblePosition(JOffset offset) {
        return offset.editor.visualPositionToXY(offset.editor.offsetToVisualPosition(offset.offset));
    }

    private void drawPanelBackground(Graphics g) {
        ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        g.setColor(PANEL_BACKGROUND_COLOR);
        g.fillRect(0, 0, (int) this.getBounds().getWidth(), (int) this.getBounds().getHeight());
        ((Graphics2D)g).setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void setupLocationAndBoundsOfPanel(Editor editor) {
        this.setLocation(0, 0);
        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        JComponent parent = editor.getContentComponent();
        int x = (int) (parent.getLocation().getX() + visibleArea.getX() + editor.getScrollingModel().getHorizontalScrollOffset());
        this.setBounds(x, (int) (visibleArea.getY()), (int) visibleArea.getWidth(), (int) visibleArea.getHeight());
    }
}
