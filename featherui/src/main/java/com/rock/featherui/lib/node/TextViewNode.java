package com.rock.featherui.lib.node;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import com.rock.featherui.lib.pool.ObjectPool;

import java.util.ArrayList;
import java.util.List;

public class TextViewNode extends VirtualNode {

    public String text = "";
    public float textSize = 14f; // Default 14sp/dp
    public int textColor = 0xFFFFFFFF;
    public String fontFamily;
    public String fontStyle;

    // Cache to prevent allocation in draw loop
    private final List<String> cachedLines = new ArrayList<>();
    private final List<Integer> cachedLineWidths = new ArrayList<>();
    private float lineHeight = 0f;
    private float fontBaselineOffset = 0f;

    @Override
    protected void onMeasure(int parentWidth, int parentHeight) {
        int availWidth = (width == MATCH_PARENT) ? parentWidth : parentWidth - marginLeft - marginRight;
        int availHeight = (height == MATCH_PARENT) ? parentHeight : parentHeight - marginTop - marginBottom;

        int maxWidth = availWidth - paddingLeft - paddingRight;

        Paint paint = ObjectPool.acquirePaint();
        setupPaint(paint);

        // Calculate line heights
        Paint.FontMetrics fm = paint.getFontMetrics();
        lineHeight = fm.bottom - fm.top;
        fontBaselineOffset = -fm.top;

        cachedLines.clear();
        cachedLineWidths.clear();

        if (text == null || text.isEmpty()) {
            measuredWidth = (width >= 0) ? width : paddingLeft + paddingRight;
            measuredHeight = (height >= 0) ? height : paddingTop + paddingBottom;
            ObjectPool.release(paint);
            return;
        }

        // Split by manual line breaks first
        String[] paragraphs = text.split("\n");
        int maxMeasuredLineWidth = 0;

        for (String paragraph : paragraphs) {
            int start = 0;
            int end = paragraph.length();
            if (end == 0) {
                cachedLines.add("");
                cachedLineWidths.add(0);
                continue;
            }

            while (start < end) {
                // Break text into lines that fit in maxWidth
                int count = paint.breakText(paragraph, start, end, true, maxWidth, null);
                if (count <= 0) {
                    break;
                }

                int breakIndex = start + count;
                if (breakIndex < end) {
                    // Try to break at word boundaries
                    int lastSpace = paragraph.substring(start, breakIndex).lastIndexOf(' ');
                    if (lastSpace > 0) {
                        breakIndex = start + lastSpace + 1;
                    }
                }

                String line = paragraph.substring(start, breakIndex);
                cachedLines.add(line);
                int lineWidth = Math.round(paint.measureText(line));
                cachedLineWidths.add(lineWidth);
                maxMeasuredLineWidth = Math.max(maxMeasuredLineWidth, lineWidth);
                start = breakIndex;
            }
        }

        // Set measured width
        if (width >= 0) {
            measuredWidth = width;
        } else if (width == MATCH_PARENT) {
            measuredWidth = parentWidth;
        } else {
            measuredWidth = maxMeasuredLineWidth + paddingLeft + paddingRight;
        }

        // Set measured height
        if (height >= 0) {
            measuredHeight = height;
        } else if (height == MATCH_PARENT) {
            measuredHeight = parentHeight;
        } else {
            measuredHeight = Math.round(cachedLines.size() * lineHeight) + paddingTop + paddingBottom;
        }

        ObjectPool.release(paint);
    }

    @Override
    protected void onLayout(int absoluteLeft, int absoluteTop) {
        // No additional layout needed for leaf TextView node
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (cachedLines.isEmpty()) return;

        Paint paint = ObjectPool.acquirePaint();
        setupPaint(paint);

        int hGravity = gravity & 7;
        int vGravity = gravity & 112;

        float currentY = top + paddingTop + fontBaselineOffset;

        // Apply vertical alignment
        if (vGravity == 16) { // Center Vertical
            float textHeight = cachedLines.size() * lineHeight;
            float contentHeight = measuredHeight - paddingTop - paddingBottom;
            if (contentHeight > textHeight) {
                currentY += (contentHeight - textHeight) / 2f;
            }
        } else if (vGravity == 80) { // Bottom
            float textHeight = cachedLines.size() * lineHeight;
            float contentHeight = measuredHeight - paddingTop - paddingBottom;
            if (contentHeight > textHeight) {
                currentY += (contentHeight - textHeight);
            }
        }

        for (int i = 0; i < cachedLines.size(); i++) {
            String line = cachedLines.get(i);
            if (!line.isEmpty()) {
                float drawX = left + paddingLeft;
                if (hGravity == 1) { // Center Horizontal
                    int lineWidth = cachedLineWidths.size() > i ? cachedLineWidths.get(i) : 0;
                    drawX += (measuredWidth - paddingLeft - paddingRight - lineWidth) / 2f;
                } else if (hGravity == 5) { // Right
                    int lineWidth = cachedLineWidths.size() > i ? cachedLineWidths.get(i) : 0;
                    drawX += (measuredWidth - paddingLeft - paddingRight - lineWidth);
                }
                canvas.drawText(line, drawX, currentY, paint);
            }
            currentY += lineHeight;
        }

        ObjectPool.release(paint);
    }

    private void setupPaint(Paint paint) {
        paint.setTextSize(textSize);
        paint.setColor(textColor);
        paint.setStyle(Paint.Style.FILL);

        // Apply Font Family & Style
        Typeface typeface = Typeface.DEFAULT;
        int style = Typeface.NORMAL;
        if ("bold".equalsIgnoreCase(fontStyle)) {
            style = Typeface.BOLD;
        } else if ("italic".equalsIgnoreCase(fontStyle)) {
            style = Typeface.ITALIC;
        }

        if (fontFamily != null) {
            try {
                typeface = Typeface.create(fontFamily, style);
            } catch (Exception e) {
                typeface = Typeface.create(Typeface.DEFAULT, style);
            }
        } else if (style != Typeface.NORMAL) {
            typeface = Typeface.create(Typeface.DEFAULT, style);
        }

        paint.setTypeface(typeface);
    }
}
