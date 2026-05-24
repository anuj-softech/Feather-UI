package com.rock.featherui.lib.node;

import com.rock.featherui.lib.animation.AnimationEngine;

public class ButtonNode extends FrameLayoutNode {

    public float normalScale = 1.0f;
    public float focusedScale = 1.08f;

    public int normalBgColor = 0;
    public int focusedBgColor = 0;

    public int normalBorderColor = 0;
    public int focusedBorderColor = 0;

    public float normalBorderWidth = 0f;
    public float focusedBorderWidth = 0f;

    public int normalGlowColor = 0;
    public int focusedGlowColor = 0;

    public float normalGlowRadius = 0f;
    public float focusedGlowRadius = 0f;

    public int normalTextColor = 0;
    public int focusedTextColor = 0;

    public int normalTintColor = 0;
    public int focusedTintColor = 0;

    private boolean isInitialized = false;

    public ButtonNode() {
        this.focusable = true;
    }

    public void setFocusState(boolean isFocused, boolean animate) {
        if (!isInitialized) {
            // Store the initial properties as the "normal" states if not explicitly set
            if (normalBgColor == 0) normalBgColor = backgroundColor;
            if (normalBorderColor == 0) normalBorderColor = borderColor;
            if (normalBorderWidth == 0f) normalBorderWidth = borderWidth;
            if (normalGlowColor == 0) normalGlowColor = glowColor;
            if (normalGlowRadius == 0f) normalGlowRadius = glowRadius;

            if (normalTextColor == 0) normalTextColor = findFirstTextViewColor(this);
            if (normalTintColor == 0) normalTintColor = findFirstImageViewTint(this);
            isInitialized = true;
        }

        this.focused = isFocused;

        float targetScale = isFocused ? focusedScale : normalScale;
        int targetBg = isFocused && focusedBgColor != 0 ? focusedBgColor : normalBgColor;
        int targetBorder = isFocused && focusedBorderColor != 0 ? focusedBorderColor : normalBorderColor;
        float targetBorderW = isFocused && focusedBorderWidth != 0f ? focusedBorderWidth : normalBorderWidth;
        int targetGlow = isFocused && focusedGlowColor != 0 ? focusedGlowColor : normalGlowColor;
        float targetGlowR = isFocused && focusedGlowRadius != 0f ? focusedGlowRadius : normalGlowRadius;

        int targetTextCol = isFocused && focusedTextColor != 0 ? focusedTextColor : normalTextColor;
        if (targetTextCol != 0) {
            updateChildrenTextViewColor(this, targetTextCol);
        }

        int targetTintCol = isFocused && focusedTintColor != 0 ? focusedTintColor : normalTintColor;
        if (targetTintCol == 0 && targetTextCol != 0) {
            targetTintCol = targetTextCol; // fallback to text color if tint not explicitly set
        }
        if (targetTintCol != 0) {
            updateChildrenImageViewTint(this, targetTintCol);
        }

        if (animate) {
            // Animate scale using AnimationEngine with smooth ease-out easing
            AnimationEngine.animate(this, "scaleX", scaleX, targetScale, 200, "ease_out");
            AnimationEngine.animate(this, "scaleY", scaleY, targetScale, 200, "ease_out");
        } else {
            this.scaleX = targetScale;
            this.scaleY = targetScale;
        }

        this.backgroundColor = targetBg;
        this.borderColor = targetBorder;
        this.borderWidth = targetBorderW;
        this.glowColor = targetGlow;
        this.glowRadius = targetGlowR;
    }

    private int findFirstTextViewColor(VirtualNode node) {
        if (node instanceof TextViewNode) {
            return ((TextViewNode) node).textColor;
        }
        for (int i = 0; i < node.children.size(); i++) {
            int col = findFirstTextViewColor(node.children.get(i));
            if (col != 0) return col;
        }
        return 0;
    }

    private void updateChildrenTextViewColor(VirtualNode node, int color) {
        if (node instanceof TextViewNode) {
            ((TextViewNode) node).textColor = color;
        }
        for (int i = 0; i < node.children.size(); i++) {
            updateChildrenTextViewColor(node.children.get(i), color);
        }
    }

    private int findFirstImageViewTint(VirtualNode node) {
        if (node instanceof ImageViewNode) {
            ImageViewNode iv = (ImageViewNode) node;
            if (iv.src != null && !iv.src.startsWith("http") && !iv.src.startsWith("assets/")) {
                return iv.tintColor;
            }
        }
        for (int i = 0; i < node.children.size(); i++) {
            int col = findFirstImageViewTint(node.children.get(i));
            if (col != 0) return col;
        }
        return 0;
    }

    private void updateChildrenImageViewTint(VirtualNode node, int color) {
        if (node instanceof ImageViewNode) {
            ImageViewNode iv = (ImageViewNode) node;
            if (iv.src != null && !iv.src.startsWith("http") && !iv.src.startsWith("assets/")) {
                iv.tintColor = color;
            }
        }
        for (int i = 0; i < node.children.size(); i++) {
            updateChildrenImageViewTint(node.children.get(i), color);
        }
    }
}
