package org.hunmr.acejump.marker;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorFontType;
import org.hunmr.options.PluginConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.HashSet;

public class MarkersPanel extends JComponent {
    public static final Color PANEL_BACKGROUND_COLOR = new Color(128, 138, 142);
    public Editor _editor;
    private MarkerCollection _markerCollection;

    final PluginConfig _config = PluginConfig.getInstance();

    public MarkersPanel(Editor editor, MarkerCollection markerCollection) {
        _editor = editor;
        _markerCollection = markerCollection;
        setOpaque(false);
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
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (!isUsableEditor(_editor) || _markerCollection == null) {
            return;
        }

        Graphics2D graphics = (Graphics2D) g.create();
        try {
            Font font = _editor.getColorsScheme().getFont(EditorFontType.PLAIN);
            FontMetrics fontMetrics = graphics.getFontMetrics(font);

            graphics.setFont(font);
            drawPanelBackground(graphics);

            HashSet<JOffset> firstJumpOffsets = new HashSet<JOffset>();
            for (Marker marker : _markerCollection.values()) {
                if (!isRenderableMarker(marker)) continue;
                if (marker.getOffset().editor != _editor) continue;

                for (JOffset offset : marker.getOffsets()) {
                    if (!isRenderableOffset(offset)) continue;
                    firstJumpOffsets.add(offset);
                    drawMarker(graphics, offset, marker.getMarkerChar(), 0, _config.getFirstJumpBackground(), _config.getFirstJumpForeground(), fontMetrics);
                }
            }

            for (Marker marker : _markerCollection.values()) {
                if (!isRenderableMarker(marker))                                                            continue;
                if (marker.getOffset().editor != _editor)                                                   continue;
                if (marker.getMarker().length() == 1 || marker.isMappingToMultipleOffset())                 continue;
                if (isAlreadyHasFirstJumpCharInPlace(firstJumpOffsets, marker) && !isLineEndOffset(marker)) continue;

                int xOffset = charWidth(fontMetrics, marker.getMarkerChar());
                for (JOffset offset : marker.getOffsets()) {
                    if (!isRenderableOffset(offset)) continue;
                    drawMarker(graphics, offset, marker.getMarker().charAt(1), xOffset, _config.getSecondJumpBackground(), _config.getSecondJumpForeground(), fontMetrics);
                }
            }
        }
        finally {
            graphics.dispose();
        }
    }

    private boolean isAlreadyHasFirstJumpCharInPlace(HashSet<JOffset> firstJumpOffsets, Marker marker) {
        if (!isRenderableMarker(marker)) {
            return false;
        }

        JOffset o = new JOffset(marker.getOffset().editor, marker.getOffset().offset + 1);
        return firstJumpOffsets.contains(o);
    }

    private void drawMarker(Graphics2D graphics, JOffset offset, char markerChar, int xOffset, Color background, Color foreground, FontMetrics fontMetrics) {
        Point2D position = getPanelPosition(offset);
        int x = (int) Math.round(position.getX()) + xOffset;
        int y = (int) Math.round(position.getY());

        drawBackground(graphics, x, y, background, charWidth(fontMetrics, markerChar), _editor.getLineHeight());
        drawMarkerChar(graphics, x, y + _editor.getAscent(), markerChar, foreground);
    }

    private Point2D getPanelPosition(JOffset offset) {
        Point2D visiblePosition = getVisiblePosition(offset);
        return new Point2D.Double(visiblePosition.getX() - getX(), visiblePosition.getY() - getY());
    }

    private int charWidth(FontMetrics fontMetrics, char markerChar) {
        return Math.max(fontMetrics.charWidth(markerChar), 1);
    }

    private void drawMarkerChar(Graphics2D graphics, int x, int baseline, char markerChar, Color foreground) {
        graphics.setColor(foreground);
        graphics.drawString(String.valueOf(markerChar), x, baseline);
    }

    private void drawBackground(Graphics2D graphics, int x, int y, Color background, int width, int height) {
        graphics.setColor(background);
        graphics.fillRect(x, y, width, height);
    }

    private Point2D getVisiblePosition(JOffset offset) {
        return offset.editor.visualPositionToPoint2D(offset.editor.offsetToVisualPosition(offset.offset));
    }

    private void drawPanelBackground(Graphics2D graphics) {
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.3f));
        graphics.setColor(PANEL_BACKGROUND_COLOR);
        graphics.fillRect(0, 0, getWidth(), getHeight());
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
    }

    private void setupLocationAndBoundsOfPanel(Editor editor) {
        if (!isUsableEditor(editor)) {
            setBounds(0, 0, 0, 0);
            return;
        }

        Rectangle visibleArea = editor.getScrollingModel().getVisibleArea();
        setBounds(visibleArea.x, visibleArea.y, visibleArea.width, visibleArea.height);
    }

    private boolean isRenderableMarker(Marker marker) {
        return marker != null && marker.getOffset() != null && isRenderableOffset(marker.getOffset());
    }

    private boolean isRenderableOffset(JOffset offset) {
        return offset != null && isUsableEditor(offset.editor);
    }

    private boolean isUsableEditor(Editor editor) {
        return editor != null && !editor.isDisposed();
    }
}
