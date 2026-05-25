package com.rock.featherui.lib.node;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import com.rock.featherui.lib.pool.ObjectPool;
import com.rock.featherui.lib.registry.PropertyRegistry;

import java.util.ArrayList;
import java.util.List;

public abstract class VirtualNode {
    public static final int MATCH_PARENT = -1;
    public static final int WRAP_CONTENT = -2;

    public String id;
    public String type;

    // Hierarchy
    public VirtualNode parent;
    public final List<VirtualNode> children = new ArrayList<>();

    // Layout Constraints (in Pixels)
    public int width = WRAP_CONTENT;
    public int height = WRAP_CONTENT;
    public int marginLeft, marginTop, marginRight, marginBottom;
    public int paddingLeft, paddingTop, paddingRight, paddingBottom;
    public int gravity = 0; // Layout specific

    // Computed absolute bounds
    public int left, top, right, bottom;
    public int measuredWidth, measuredHeight;
    public int boundIndex = -1; // To match recycled node indices

    // Focus properties
    public boolean focusable = false;
    public boolean focused = false;
    public String nextFocusUpId;
    public String nextFocusDownId;
    public String nextFocusLeftId;
    public String nextFocusRightId;

    // Transform properties
    public float scaleX = 1.0f;
    public float scaleY = 1.0f;
    public float translationX = 0f;
    public float translationY = 0f;
    public float rotation = 0f;
    public float alpha = 1.0f;
    public float backdropBlur = 0f;
    public String visibility = "visible"; // "visible", "invisible", "gone"

    // Styling Properties
    public int backgroundColor = 0;
    public int borderColor = 0;
    public float borderWidth = 0f;
    public float borderRadius = 0f;
    public int glowColor = 0;
    public float glowRadius = 0f;

    // Inner Vignette / Shadow
    public int innerShadowColor = 0;
    public float innerShadowRadius = 0f;
    public float innerShadowXPercent = 0.5f;
    public float innerShadowYPercent = 0.5f;

    // Cached objects to avoid allocations during rendering
    private android.graphics.RadialGradient cachedRadialGradient = null;
    private int cachedInnerShadowColor = 0;
    private float cachedInnerShadowX = -1f;
    private float cachedInnerShadowY = -1f;
    private float cachedInnerShadowR = -1f;
    private final int[] cachedGradientColors = new int[2];
    private final float[] cachedGradientStops = new float[] { 0.0f, 1.0f };

    // Cached backdrop node to avoid tree traversal on every frame
    private VirtualNode cachedBackdropNode = null;
    private boolean backdropNodeResolved = false;

    private Path clipPath;

    // Interactivity callbacks
    public interface OnClickListener {
        void onClick(VirtualNode node);
    }

    public interface OnFocusChangeListener {
        void onFocusChange(VirtualNode node, boolean hasFocus);
    }

    public OnClickListener onClickListener;
    public OnFocusChangeListener onFocusChangeListener;

    public void addChild(VirtualNode child) {
        child.parent = this;
        children.add(child);
    }

    public void applyProperty(String key, Object value) {
        PropertyRegistry.apply(this, key, value);
    }

    public final void measure(int parentWidth, int parentHeight) {
        onMeasure(parentWidth, parentHeight);
    }

    public final void layout(int absoluteLeft, int absoluteTop) {
        this.left = absoluteLeft;
        this.top = absoluteTop;
        this.right = absoluteLeft + measuredWidth;
        this.bottom = absoluteTop + measuredHeight;
        onLayout(absoluteLeft, absoluteTop);
    }

    protected abstract void onMeasure(int parentWidth, int parentHeight);
    protected abstract void onLayout(int absoluteLeft, int absoluteTop);

    public void draw(Canvas canvas) {
        if ("gone".equalsIgnoreCase(visibility) || "invisible".equalsIgnoreCase(visibility)) return;
        if (alpha <= 0.01f) return;

        // Occlusion culling: skip drawing if node is completely outside the screen/viewport bounds
        VirtualNode root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        int screenW = root.measuredWidth;
        int screenH = root.measuredHeight;
        if (screenW > 0 && screenH > 0) {
            float pad = 100f * Math.max(scaleX, scaleY);
            if (right + pad < 0 || left - pad > screenW ||
                bottom + pad < 0 || top - pad > screenH) {
                return;
            }
        }

        canvas.save();

        boolean hasAlphaLayer = false;
        if (alpha < 0.99f) {
            RectF bounds = ObjectPool.acquireRectF();
            bounds.set(left - 100, top - 100, right + 100, bottom + 100);
            canvas.saveLayerAlpha(bounds, Math.round(alpha * 255));
            ObjectPool.release(bounds);
            hasAlphaLayer = true;
        }

        // Calculate center for transformations
        float px = (left + right) / 2f;
        float py = (top + bottom) / 2f;

        // Apply scale, translate, rotate
        if (translationX != 0f || translationY != 0f) {
            canvas.translate(translationX, translationY);
        }
        if (scaleX != 1.0f || scaleY != 1.0f) {
            canvas.scale(scaleX, scaleY, px, py);
        }
        if (rotation != 0f) {
            canvas.rotate(rotation, px, py);
        }

        // Draw background, border, glow (before clipping)
        drawBackgroundAndBorders(canvas);

        // Apply rounded corners clip path for children content and custom onDraw
        boolean isClipped = false;
        if (borderRadius > 0) {
            canvas.save();
            if (clipPath == null) {
                clipPath = new Path();
            } else {
                clipPath.reset();
            }
            RectF rect = ObjectPool.acquireRectF();
            rect.set(left, top, right, bottom);
            clipPath.addRoundRect(rect, borderRadius, borderRadius, Path.Direction.CW);
            ObjectPool.release(rect);
            canvas.clipPath(clipPath);
            isClipped = true;
        }

        // Custom drawing logic of node
        onDraw(canvas);

        // Draw inner shadow/vignette overlay (drawn on top of content, but behind children)
        drawInnerShadow(canvas);

        // Draw children
        for (int i = 0; i < children.size(); i++) {
            children.get(i).draw(canvas);
        }

        if (isClipped) {
            canvas.restore();
        }

        if (hasAlphaLayer) {
            canvas.restore();
        }

        canvas.restore();
    }

    protected void onDraw(Canvas canvas) {
        // Subclasses override this
    }

    private void drawInnerShadow(Canvas canvas) {
        if (innerShadowColor == 0) return;

        float cx = left + measuredWidth * innerShadowXPercent;
        float cy = top + measuredHeight * innerShadowYPercent;
        float r = innerShadowRadius > 0 ? innerShadowRadius : Math.max(measuredWidth, measuredHeight);

        if (cachedRadialGradient == null ||
            innerShadowColor != cachedInnerShadowColor ||
            cx != cachedInnerShadowX ||
            cy != cachedInnerShadowY ||
            r != cachedInnerShadowR) {

            cachedInnerShadowColor = innerShadowColor;
            cachedInnerShadowX = cx;
            cachedInnerShadowY = cy;
            cachedInnerShadowR = r;
            cachedGradientColors[0] = 0x00000000;
            cachedGradientColors[1] = innerShadowColor;

            cachedRadialGradient = new android.graphics.RadialGradient(cx, cy, r, cachedGradientColors, cachedGradientStops, android.graphics.Shader.TileMode.CLAMP);
        }

        Paint paint = ObjectPool.acquirePaint();
        paint.setShader(cachedRadialGradient);
        paint.setStyle(Paint.Style.FILL);

        RectF rect = ObjectPool.acquireRectF();
        rect.set(left, top, right, bottom);
        if (borderRadius > 0) {
            canvas.drawRoundRect(rect, borderRadius, borderRadius, paint);
        } else {
            canvas.drawRect(rect, paint);
        }

        paint.setShader(null);
        ObjectPool.release(rect);
        ObjectPool.release(paint);
    }

    private void drawBackgroundAndBorders(Canvas canvas) {
        RectF rect = ObjectPool.acquireRectF();
        rect.set(left, top, right, bottom);

        Paint paint = ObjectPool.acquirePaint();

        // 1. Draw Shadow/Glow if set
        if (glowRadius > 0 && glowColor != 0) {
            paint.setColor(glowColor);
            paint.setStyle(Paint.Style.FILL);
            paint.setShadowLayer(glowRadius, 0f, 0f, glowColor);
            if (borderRadius > 0) {
                canvas.drawRoundRect(rect, borderRadius, borderRadius, paint);
            } else {
                canvas.drawRect(rect, paint);
            }
            paint.clearShadowLayer();
        }

        // 1.5 Draw Backdrop Blur if set
        if (backdropBlur > 0f) {
            if (!backdropNodeResolved) {
                cachedBackdropNode = findNodeInRootById("backdrop");
                backdropNodeResolved = true;
            }
            if (cachedBackdropNode instanceof ImageViewNode) {
                ImageViewNode ivNode = (ImageViewNode) cachedBackdropNode;
                Bitmap backdropBmp = ivNode.getBitmap();
                if (backdropBmp != null) {
                    drawBackdropBlur(canvas, ivNode, backdropBmp);
                }
            }
        }

        // 2. Draw Background
        if (backgroundColor != 0) {
            paint.setColor(backgroundColor);
            paint.setStyle(Paint.Style.FILL);
            if (borderRadius > 0) {
                canvas.drawRoundRect(rect, borderRadius, borderRadius, paint);
            } else {
                canvas.drawRect(rect, paint);
            }
        }

        // 3. Draw Border
        if (borderWidth > 0 && borderColor != 0) {
            paint.setColor(borderColor);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(borderWidth);
            if (borderRadius > 0) {
                canvas.drawRoundRect(rect, borderRadius, borderRadius, paint);
            } else {
                canvas.drawRect(rect, paint);
            }
        }

        ObjectPool.release(paint);
        ObjectPool.release(rect);
    }

    private Bitmap cachedBlurredSubBmp = null;
    private Bitmap lastUsedBackdropBmp = null;
    private float lastUsedBlurRadius = -1f;

    private void drawBackdropBlur(Canvas canvas, ImageViewNode backdropNode, Bitmap bmp) {
        int w = measuredWidth;
        int h = measuredHeight;
        if (w <= 0 || h <= 0) return;

        int bgW = backdropNode.measuredWidth;
        int bgH = backdropNode.measuredHeight;
        int bmpW = bmp.getWidth();
        int bmpH = bmp.getHeight();
        if (bgW <= 0 || bgH <= 0 || bmpW <= 0 || bmpH <= 0) return;

        // Recompute cached blur only when backdrop or radius changes
        if (bmp != lastUsedBackdropBmp || backdropBlur != lastUsedBlurRadius || cachedBlurredSubBmp == null) {
            if (cachedBlurredSubBmp != null) {
                cachedBlurredSubBmp.recycle();
                cachedBlurredSubBmp = null;
            }

            // Map this node's screen region back into the bitmap's coordinate space
            float scale = Math.max((float) bgW / bmpW, (float) bgH / bmpH);
            float displayedBmpWidth  = bgW / scale;
            float displayedBmpHeight = bgH / scale;
            float cropLeft = (bmpW - displayedBmpWidth)  / 2f;
            float cropTop  = (bmpH - displayedBmpHeight) / 2f;

            float relLeft   = left  - backdropNode.left;
            float relTop    = top   - backdropNode.top;
            float relRight  = right - backdropNode.left;
            float relBottom = bottom - backdropNode.top;

            int sl = Math.max(0, Math.min(bmpW - 1, Math.round(cropLeft + (relLeft   / bgW) * displayedBmpWidth)));
            int st = Math.max(0, Math.min(bmpH - 1, Math.round(cropTop  + (relTop    / bgH) * displayedBmpHeight)));
            int sr = Math.max(0, Math.min(bmpW,     Math.round(cropLeft + (relRight  / bgW) * displayedBmpWidth)));
            int sb = Math.max(0, Math.min(bmpH,     Math.round(cropTop  + (relBottom / bgH) * displayedBmpHeight)));

            int subW = sr - sl;
            int subH = sb - st;

            if (subW > 0 && subH > 0) {
                try {
                    // Crop the exact sub-region from the source bitmap
                    Bitmap subBmp = Bitmap.createBitmap(bmp, sl, st, subW, subH);

                    // Downsample for dramatic performance improvement on CPU-bound blur
                    int downsample = 4;
                    int targetW = Math.max(1, subW / downsample);
                    int targetH = Math.max(1, subH / downsample);
                    Bitmap scaledSubBmp = Bitmap.createScaledBitmap(subBmp, targetW, targetH, true);
                    subBmp.recycle();

                    // Force 32-bit to prevent colour-depth banding
                    if (scaledSubBmp.getConfig() != Bitmap.Config.ARGB_8888) {
                        Bitmap tmp = scaledSubBmp.copy(Bitmap.Config.ARGB_8888, true);
                        scaledSubBmp.recycle();
                        scaledSubBmp = tmp;
                    }

                    // Scale the blur radius proportionally to downsampled size
                    float scaledBlurRadius = Math.max(1f, backdropBlur / downsample);
                    Bitmap blurred = ImageViewNode.boxBlur(scaledSubBmp, Math.round(scaledBlurRadius));
                    scaledSubBmp.recycle();

                    if (blurred != null) {
                        // Scale to panel size only AFTER blurring
                        cachedBlurredSubBmp = Bitmap.createScaledBitmap(blurred, w, h, true);
                        blurred.recycle();
                    }
                } catch (Exception e) {
                    // silent
                }
            }
            lastUsedBackdropBmp = bmp;
            lastUsedBlurRadius  = backdropBlur;
        }

        if (cachedBlurredSubBmp != null) {
            Paint paint = ObjectPool.acquirePaint();
            paint.setFilterBitmap(true);
            paint.setDither(true);
            // Apply the alpha of the source backdrop image node (e.g. 0.2f -> 51 alpha)
            paint.setAlpha(Math.round(backdropNode.alpha * 255));

            RectF dstRect = ObjectPool.acquireRectF();
            dstRect.set(left, top, right, bottom);

            boolean clip = borderRadius > 0;
            if (clip) {
                canvas.save();
                Path p = new Path();
                p.addRoundRect(dstRect, borderRadius, borderRadius, Path.Direction.CW);
                canvas.clipPath(p);
            }

            // Draw a solid black background to completely block the sharp backdrop underneath
            Paint blackPaint = ObjectPool.acquirePaint();
            blackPaint.setColor(0xFF000000);
            blackPaint.setStyle(Paint.Style.FILL);
            canvas.drawRect(dstRect, blackPaint);
            ObjectPool.release(blackPaint);

            // Draw the full blurred bitmap stretched to exactly cover (left,top,right,bottom).
            // Using the srcRect→dstRect overload guarantees correct alignment regardless of
            // where the node sits on screen.
            android.graphics.Rect srcRect = ObjectPool.acquireRect();
            srcRect.set(0, 0, cachedBlurredSubBmp.getWidth(), cachedBlurredSubBmp.getHeight());
            canvas.drawBitmap(cachedBlurredSubBmp, srcRect, dstRect, paint);
            ObjectPool.release(srcRect);

            if (clip) canvas.restore();

            ObjectPool.release(dstRect);
            ObjectPool.release(paint);
        }
    }

    private VirtualNode findNodeInRootById(String targetId) {
        VirtualNode root = this;
        while (root.parent != null) {
            root = root.parent;
        }
        return findNodeById(root, targetId);
    }

    private VirtualNode findNodeById(VirtualNode node, String targetId) {
        if (node == null) return null;
        if (targetId.equals(node.id)) return node;
        for (int i = 0; i < node.children.size(); i++) {
            VirtualNode found = findNodeById(node.children.get(i), targetId);
            if (found != null) return found;
        }
        return null;
    }
}
