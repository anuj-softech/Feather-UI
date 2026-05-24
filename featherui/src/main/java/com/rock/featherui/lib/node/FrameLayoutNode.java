package com.rock.featherui.lib.node;

public class FrameLayoutNode extends VirtualNode {

    @Override
    protected void onMeasure(int parentWidth, int parentHeight) {
        int availWidth = width >= 0 ? width : parentWidth - marginLeft - marginRight;
        int availHeight = height >= 0 ? height : parentHeight - marginTop - marginBottom;

        int maxChildWidth = 0;
        int maxChildHeight = 0;

        for (int i = 0; i < children.size(); i++) {
            VirtualNode child = children.get(i);
            if ("gone".equalsIgnoreCase(child.visibility)) continue;
            child.measure(availWidth - paddingLeft - paddingRight, availHeight - paddingTop - paddingBottom);
            maxChildWidth = Math.max(maxChildWidth, child.measuredWidth + child.marginLeft + child.marginRight);
            maxChildHeight = Math.max(maxChildHeight, child.measuredHeight + child.marginTop + child.marginBottom);
        }

        if (width >= 0) {
            measuredWidth = width;
        } else if (width == MATCH_PARENT) {
            measuredWidth = parentWidth;
        } else {
            measuredWidth = maxChildWidth + paddingLeft + paddingRight;
        }

        if (height >= 0) {
            measuredHeight = height;
        } else if (height == MATCH_PARENT) {
            measuredHeight = parentHeight;
        } else {
            measuredHeight = maxChildHeight + paddingTop + paddingBottom;
        }
    }

    @Override
    protected void onLayout(int absoluteLeft, int absoluteTop) {
        for (int i = 0; i < children.size(); i++) {
            VirtualNode child = children.get(i);
            if ("gone".equalsIgnoreCase(child.visibility)) continue;
            int childLeft = absoluteLeft + paddingLeft + child.marginLeft;
            int childTop = absoluteTop + paddingTop + child.marginTop;

            int gravity = child.gravity != 0 ? child.gravity : this.gravity;
            if (gravity != 0) {
                int hGravity = gravity & 7; // Horizontal gravity mask
                int vGravity = gravity & 112; // Vertical gravity mask

                if (hGravity == 5) { // Right
                    childLeft = absoluteLeft + measuredWidth - paddingRight - child.measuredWidth - child.marginRight;
                } else if (hGravity == 1) { // Center Horizontal
                    childLeft = absoluteLeft + paddingLeft + (measuredWidth - paddingLeft - paddingRight - child.measuredWidth) / 2 + child.marginLeft - child.marginRight;
                }

                if (vGravity == 80) { // Bottom
                    childTop = absoluteTop + measuredHeight - paddingBottom - child.measuredHeight - child.marginBottom;
                } else if (vGravity == 16) { // Center Vertical
                    childTop = absoluteTop + paddingTop + (measuredHeight - paddingTop - paddingBottom - child.measuredHeight) / 2 + child.marginTop - child.marginBottom;
                }
            }

            child.layout(childLeft, childTop);
        }
    }
}
