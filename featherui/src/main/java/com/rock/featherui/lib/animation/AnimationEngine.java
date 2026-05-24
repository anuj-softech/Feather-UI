package com.rock.featherui.lib.animation;

import android.graphics.Rect;
import android.view.Choreographer;
import com.rock.featherui.lib.node.VirtualNode;
import com.rock.featherui.lib.view.FeatherUIView;

import java.util.ArrayList;
import java.util.List;

public class AnimationEngine {

    private static class NodeAnimation {
        VirtualNode node;
        String property;
        float startValue;
        float endValue;
        long startTime;
        long duration;
        String easing;
        boolean isFinished = false;
    }

    private static final List<NodeAnimation> activeAnimations = new ArrayList<>();
    private static boolean isTickerRunning = false;
    private static FeatherUIView hostView;

    private static final Choreographer.FrameCallback frameCallback = new Choreographer.FrameCallback() {
        @Override
        public void doFrame(long frameTimeNanos) {
            long now = System.currentTimeMillis();
            boolean hasPending = false;
            
            synchronized (activeAnimations) {
                for (int i = activeAnimations.size() - 1; i >= 0; i--) {
                    NodeAnimation anim = activeAnimations.get(i);
                    long elapsed = now - anim.startTime;
                    float progress = (float) elapsed / anim.duration;
                    
                    if (progress >= 1.0f) {
                        progress = 1.0f;
                        anim.isFinished = true;
                    } else {
                        hasPending = true;
                    }

                    float eased = applyEasing(progress, anim.easing);
                    float currentValue = anim.startValue + (anim.endValue - anim.startValue) * eased;

                    // Calculate dirty rectangle before changing value
                    Rect dirtyRect = null;
                    if (hostView != null) {
                        dirtyRect = new Rect(anim.node.left, anim.node.top, anim.node.right, anim.node.bottom);
                    }

                    // Apply value
                    applyProperty(anim.node, anim.property, currentValue);

                    // Combine dirty rect after value changes
                    if (hostView != null && dirtyRect != null) {
                        dirtyRect.union(anim.node.left, anim.node.top, anim.node.right, anim.node.bottom);
                        // Add padding/glow buffer (80px ensures large scales + glow halos are completely cleared)
                        dirtyRect.inset(-80, -80);
                        hostView.requestLayoutOrPaint(anim.node, dirtyRect);
                    }

                    if (anim.isFinished) {
                        activeAnimations.remove(i);
                    }
                }

                if (hasPending) {
                    Choreographer.getInstance().postFrameCallback(this);
                } else {
                    isTickerRunning = false;
                }
            }
        }
    };

    public static void setHostView(FeatherUIView view) {
        hostView = view;
    }

    public static void animate(VirtualNode node, String property, float start, float end, long duration) {
        animate(node, property, start, end, duration, "ease_out");
    }

    public static void animate(VirtualNode node, String property, float start, float end, long duration, String easing) {
        synchronized (activeAnimations) {
            // Cancel existing animation on the same property for this node
            for (int i = activeAnimations.size() - 1; i >= 0; i--) {
                NodeAnimation a = activeAnimations.get(i);
                if (a.node == node && a.property.equals(property)) {
                    activeAnimations.remove(i);
                }
            }

            NodeAnimation anim = new NodeAnimation();
            anim.node = node;
            anim.property = property;
            anim.startValue = start;
            anim.endValue = end;
            anim.startTime = System.currentTimeMillis();
            anim.duration = duration;
            anim.easing = easing;

            activeAnimations.add(anim);

            if (!isTickerRunning) {
                isTickerRunning = true;
                Choreographer.getInstance().postFrameCallback(frameCallback);
            }
        }
    }

    private static float applyEasing(float t, String curve) {
        if ("ease_in_out".equalsIgnoreCase(curve)) {
            return t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
        } else if ("ease_out".equalsIgnoreCase(curve)) {
            return t * (2f - t); // Quadratic ease out
        } else if ("ease_in".equalsIgnoreCase(curve)) {
            return t * t; // Quadratic ease in
        } else if ("spring".equalsIgnoreCase(curve) || "elastic".equalsIgnoreCase(curve)) {
            // iOS style quick spring bounce: damping factor = 0.5, period = 0.3
            if (t == 0) return 0f;
            if (t == 1) return 1f;
            return (float) (Math.pow(2, -10 * t) * Math.sin((t - 0.075f) * (2 * Math.PI) / 0.3f) + 1.0f);
        }
        return t; // Linear
    }

    private static void applyProperty(VirtualNode node, String property, float val) {
        if ("scaleX".equals(property)) {
            node.scaleX = val;
        } else if ("scaleY".equals(property)) {
            node.scaleY = val;
        } else if ("scale".equals(property)) {
            node.scaleX = val;
            node.scaleY = val;
        } else if ("translationX".equals(property)) {
            node.translationX = val;
        } else if ("translationY".equals(property)) {
            node.translationY = val;
        } else if ("rotation".equals(property)) {
            node.rotation = val;
        } else if ("alpha".equals(property)) {
            node.alpha = val;
        }
    }
}
