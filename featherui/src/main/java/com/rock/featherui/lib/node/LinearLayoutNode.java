package com.rock.featherui.lib.node;

public class LinearLayoutNode extends VirtualNode {
    public static final String HORIZONTAL = "horizontal";
    public static final String VERTICAL = "vertical";

    public String orientation = HORIZONTAL;
    public int gap = 0;

    @Override
    protected void onMeasure(int parentWidth, int parentHeight) {
        int availWidth = width >= 0 ? width : parentWidth - marginLeft - marginRight;
        int availHeight = height >= 0 ? height : parentHeight - marginTop - marginBottom;

        int totalWidth = 0;
        int totalHeight = 0;
        int maxChildWidth = 0;
        int maxChildHeight = 0;

        int visibleChildren = 0;
        for (int i = 0; i < children.size(); i++) {
            VirtualNode child = children.get(i);
            // Measure children
            child.measure(availWidth - paddingLeft - paddingRight, availHeight - paddingTop - paddingBottom);
            if (!"gone".equalsIgnoreCase(child.visibility)) {
                visibleChildren++;
                maxChildWidth = Math.max(maxChildWidth, child.measuredWidth + child.marginLeft + child.marginRight);
                maxChildHeight = Math.max(maxChildHeight, child.measuredHeight + child.marginTop + child.marginBottom);

                if (VERTICAL.equalsIgnoreCase(orientation)) {
                    totalHeight += child.measuredHeight + child.marginTop + child.marginBottom;
                } else {
                    totalWidth += child.measuredWidth + child.marginLeft + child.marginRight;
                }
            }
        }

        int gapSpacing = (visibleChildren > 1) ? gap * (visibleChildren - 1) : 0;
        if (VERTICAL.equalsIgnoreCase(orientation)) {
            totalHeight += gapSpacing;
        } else {
            totalWidth += gapSpacing;
        }

        // Set measured width
        if (width >= 0) {
            measuredWidth = width;
        } else if (width == MATCH_PARENT) {
            measuredWidth = parentWidth;
        } else {
            measuredWidth = (VERTICAL.equalsIgnoreCase(orientation) ? maxChildWidth : totalWidth) + paddingLeft + paddingRight;
        }

        // Set measured height
        if (height >= 0) {
            measuredHeight = height;
        } else if (height == MATCH_PARENT) {
            measuredHeight = parentHeight;
        } else {
            measuredHeight = (VERTICAL.equalsIgnoreCase(orientation) ? totalHeight : maxChildHeight) + paddingTop + paddingBottom;
        }
    }

    @Override
    protected void onLayout(int absoluteLeft, int absoluteTop) {
        int currentX = absoluteLeft + paddingLeft;
        int currentY = absoluteTop + paddingTop;

        for (int i = 0; i < children.size(); i++) {
            VirtualNode child = children.get(i);
            if ("gone".equalsIgnoreCase(child.visibility)) continue;

            int childLeft = currentX + child.marginLeft;
            int childTop = currentY + child.marginTop;

            int gravity = child.gravity != 0 ? child.gravity : this.gravity;
            // Handle alignment orthogonal to orientation
            if (VERTICAL.equalsIgnoreCase(orientation)) {
                // Horizontal alignment for children inside vertical layout
                int hGravity = gravity & 7;
                if (hGravity == 5) { // Right
                    childLeft = absoluteLeft + measuredWidth - paddingRight - child.measuredWidth - child.marginRight;
                } else if (hGravity == 1) { // Center Horizontal
                    childLeft = absoluteLeft + paddingLeft + (measuredWidth - paddingLeft - paddingRight - child.measuredWidth) / 2 + child.marginLeft - child.marginRight;
                }
                child.layout(childLeft, childTop);
                currentY += child.measuredHeight + child.marginTop + child.marginBottom + gap;
            } else {
                // Vertical alignment for children inside horizontal layout
                int vGravity = gravity & 112;
                if (vGravity == 80) { // Bottom
                    childTop = absoluteTop + measuredHeight - paddingBottom - child.measuredHeight - child.marginBottom;
                } else if (vGravity == 16) { // Center Vertical
                    childTop = absoluteTop + paddingTop + (measuredHeight - paddingTop - paddingBottom - child.measuredHeight) / 2 + child.marginTop - child.marginBottom;
                }
                child.layout(childLeft, childTop);
                currentX += child.measuredWidth + child.marginLeft + child.marginRight + gap;
            }
        }
    }
}
