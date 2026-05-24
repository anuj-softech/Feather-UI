package com.rock.featherui.lib.node;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import com.rock.featherui.lib.pool.ObjectPool;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.core.content.ContextCompat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.BitmapDrawable;

public class ImageViewNode extends VirtualNode {

    public static Context appContext;
    private static final ConcurrentHashMap<String, Bitmap> bitmapCache = new ConcurrentHashMap<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface ImageLoadListener {
        void onImageLoaded(ImageViewNode node, Bitmap bitmap);
    }

    public static ImageLoadListener globalLoadListener;

    public String src = "";
    public String scaleType = "fit_center"; // "fit_center" or "center_crop"
    public int tintColor = 0;
    public float blurRadius = 0f;

    public void setSrc(String newSrc) {
        if (newSrc == null) newSrc = "";
        if (!newSrc.equals(this.src)) {
            this.src = newSrc;
            this.bitmap = null;
            this.loadedSrc = "";
            checkAndLoadImage();
        }
    }

    private Bitmap bitmap;
    private boolean isLoading = false;
    private String loadedSrc = "";

    @Override
    protected void onMeasure(int parentWidth, int parentHeight) {
        int availWidth = (width == MATCH_PARENT) ? parentWidth : parentWidth - marginLeft - marginRight;
        int availHeight = (height == MATCH_PARENT) ? parentHeight : parentHeight - marginTop - marginBottom;

        // Leaf image view has either fixed size or wraps bitmap size or matches parent
        if (width >= 0) {
            measuredWidth = width;
        } else if (width == MATCH_PARENT) {
            measuredWidth = parentWidth;
        } else {
            measuredWidth = (bitmap != null) ? bitmap.getWidth() + paddingLeft + paddingRight : paddingLeft + paddingRight;
        }

        if (height >= 0) {
            measuredHeight = height;
        } else if (height == MATCH_PARENT) {
            measuredHeight = parentHeight;
        } else {
            measuredHeight = (bitmap != null) ? bitmap.getHeight() + paddingTop + paddingBottom : paddingTop + paddingBottom;
        }

        // Trigger load if needed
        checkAndLoadImage();
    }

    @Override
    protected void onLayout(int absoluteLeft, int absoluteTop) {
        // Leaf node
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (bitmap == null) {
            // Draw a simple placeholder or background if loading
            return;
        }

        Rect srcRect = ObjectPool.acquireRect();
        RectF dstRect = ObjectPool.acquireRectF();
        Paint paint = ObjectPool.acquirePaint();
        paint.setFilterBitmap(true);
        if (tintColor != 0) {
            paint.setColorFilter(new android.graphics.PorterDuffColorFilter(tintColor, android.graphics.PorterDuff.Mode.SRC_IN));
        }

        int viewWidth = right - left - paddingLeft - paddingRight;
        int viewHeight = bottom - top - paddingTop - paddingBottom;
        int bmpWidth = bitmap.getWidth();
        int bmpHeight = bitmap.getHeight();

        float leftPos = left + paddingLeft;
        float topPos = top + paddingTop;

        if ("center_crop".equalsIgnoreCase(scaleType)) {
            // Crop source to match target aspect ratio
            float scale = Math.max((float) viewWidth / bmpWidth, (float) viewHeight / bmpHeight);
            float srcWidth = viewWidth / scale;
            float srcHeight = viewHeight / scale;

            float srcLeft = (bmpWidth - srcWidth) / 2f;
            float srcTop = (bmpHeight - srcHeight) / 2f;

            srcRect.set((int) srcLeft, (int) srcTop, (int) (srcLeft + srcWidth), (int) (srcTop + srcHeight));
            dstRect.set(leftPos, topPos, leftPos + viewWidth, topPos + viewHeight);
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint);
        } else {
            // Fit Center: Fit entire image inside target keeping aspect ratio
            float scale = Math.min((float) viewWidth / bmpWidth, (float) viewHeight / bmpHeight);
            float dstWidth = bmpWidth * scale;
            float dstHeight = bmpHeight * scale;

            float dstLeft = leftPos + (viewWidth - dstWidth) / 2f;
            float dstTop = topPos + (viewHeight - dstHeight) / 2f;

            srcRect.set(0, 0, bmpWidth, bmpHeight);
            dstRect.set(dstLeft, dstTop, dstLeft + dstWidth, dstTop + dstHeight);
            canvas.drawBitmap(bitmap, srcRect, dstRect, paint);
        }

        ObjectPool.release(paint);
        ObjectPool.release(dstRect);
        ObjectPool.release(srcRect);
    }

    private void checkAndLoadImage() {
        if (src == null || src.isEmpty() || isLoading) {
            return;
        }

        final String cacheKey = src + "_blur_" + blurRadius;
        Bitmap cached = bitmapCache.get(cacheKey);
        if (cached != null) {
            this.bitmap = cached;
            this.loadedSrc = src;
            return;
        }

        isLoading = true;
        final String targetSrc = src;
        final float targetBlur = blurRadius;
        final String targetCacheKey = cacheKey;

        executor.execute(() -> {
            Bitmap bmp = null;
            try {
                if (targetSrc.startsWith("http")) {
                    HttpURLConnection conn = (HttpURLConnection) new URL(targetSrc).openConnection();
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream input = conn.getInputStream();
                    bmp = BitmapFactory.decodeStream(input);
                } else if (appContext != null) {
                    if (targetSrc.startsWith("assets/")) {
                        // Asset
                        String assetPath = targetSrc.substring(7);
                        InputStream input = appContext.getAssets().open(assetPath);
                        bmp = BitmapFactory.decodeStream(input);
                    } else {
                        // Drawable / Resource
                        int resId = appContext.getResources().getIdentifier(targetSrc, "drawable", appContext.getPackageName());
                        if (resId != 0) {
                            bmp = getBitmapFromVectorDrawable(appContext, resId, measuredWidth, measuredHeight);
                        }
                    }
                }

                if (bmp != null && targetBlur > 0f) {
                    bmp = boxBlur(bmp, Math.round(targetBlur));
                }
            } catch (Exception e) {
                // Silently handle error
            }

            final Bitmap finalBmp = bmp;
            mainHandler.post(() -> {
                isLoading = false;
                if (finalBmp != null) {
                    bitmapCache.put(targetCacheKey, finalBmp);
                    ImageViewNode.this.bitmap = finalBmp;
                    ImageViewNode.this.loadedSrc = targetSrc;
                    if (globalLoadListener != null) {
                        globalLoadListener.onImageLoaded(ImageViewNode.this, finalBmp);
                    }
                }
            });
        });
    }

    public Bitmap getBitmap() {
        return bitmap;
    }

    public static Bitmap boxBlur(Bitmap src, int radius) {
        if (radius < 1) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        // ─────────────────────────────────────────────────────────────────────────
        // Chromium / Skia Gaussian Blur approximation:
        //   Three passes of a separable box blur (horizontal + vertical) applied
        //   in sequence on the FULL-RESOLUTION image.
        //   By the Central Limit Theorem, 3 box convolutions converge to a true
        //   Gaussian. Each pass uses an O(1) sliding-window sum (prefix integral),
        //   so total cost is O(6 * w * h) regardless of blur radius.
        //   No downsampling → no colour banding, no bilinear-scaling artefacts.
        //   This is exactly what SkBlurMask.cpp and blink's BackdropFilter do.
        // ─────────────────────────────────────────────────────────────────────────
        Bitmap bitmap = src.copy(Bitmap.Config.ARGB_8888, true); // always 32-bit
        int[] pix = new int[w * h];
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        // Chromium box-size formula for 3 passes that best fits sigma = radius/2
        // (mirrors blink::CalculateBoxSizes):  box ≈ sigma * 2 | 1 (odd)
        int r = Math.max(1, radius);
        for (int pass = 0; pass < 3; pass++) {
            boxBlurH(pix, w, h, r); // horizontal pass
            boxBlurV(pix, w, h, r); // vertical  pass
        }

        bitmap.setPixels(pix, 0, w, 0, 0, w, h);
        return bitmap;
    }

    /** Single horizontal box-blur pass using a sliding-window O(1) sum. */
    private static void boxBlurH(int[] pix, int w, int h, int r) {
        float inv = 1f / (r + r + 1);
        for (int y = 0; y < h; y++) {
            int off = y * w;
            // Seed the initial window with edge-extension (clamp border)
            int rSum = 0, gSum = 0, bSum = 0, aSum = 0;
            int firstPx = pix[off];
            int lastPx  = pix[off + w - 1];
            for (int i = -r; i <= r; i++) {
                int idx = off + Math.max(0, Math.min(w - 1, i));
                int px = pix[idx];
                aSum += (px >>> 24) & 0xFF;
                rSum += (px >> 16)  & 0xFF;
                gSum += (px >> 8)   & 0xFF;
                bSum +=  px         & 0xFF;
            }
            for (int x = 0; x < w; x++) {
                pix[off + x] =
                    ((Math.round(aSum * inv) & 0xFF) << 24) |
                    ((Math.round(rSum * inv) & 0xFF) << 16) |
                    ((Math.round(gSum * inv) & 0xFF) << 8)  |
                     (Math.round(bSum * inv) & 0xFF);
                // Slide window: remove left, add right (clamp at edges)
                int addIdx = off + Math.min(x + r + 1, w - 1);
                int remIdx = off + Math.max(x - r,     0);
                int addPx  = pix[addIdx];
                int remPx  = pix[remIdx];
                aSum += ((addPx >>> 24) & 0xFF) - ((remPx >>> 24) & 0xFF);
                rSum += ((addPx >> 16)  & 0xFF) - ((remPx >> 16)  & 0xFF);
                gSum += ((addPx >> 8)   & 0xFF) - ((remPx >> 8)   & 0xFF);
                bSum += ( addPx         & 0xFF) - ( remPx         & 0xFF);
            }
        }
    }

    /** Single vertical box-blur pass using a sliding-window O(1) sum. */
    private static void boxBlurV(int[] pix, int w, int h, int r) {
        float inv = 1f / (r + r + 1);
        for (int x = 0; x < w; x++) {
            int aSum = 0, rSum = 0, gSum = 0, bSum = 0;
            for (int i = -r; i <= r; i++) {
                int idx = Math.max(0, Math.min(h - 1, i)) * w + x;
                int px = pix[idx];
                aSum += (px >>> 24) & 0xFF;
                rSum += (px >> 16)  & 0xFF;
                gSum += (px >> 8)   & 0xFF;
                bSum +=  px         & 0xFF;
            }
            for (int y = 0; y < h; y++) {
                pix[y * w + x] =
                    ((Math.round(aSum * inv) & 0xFF) << 24) |
                    ((Math.round(rSum * inv) & 0xFF) << 16) |
                    ((Math.round(gSum * inv) & 0xFF) << 8)  |
                     (Math.round(bSum * inv) & 0xFF);
                int addY = Math.min(y + r + 1, h - 1);
                int remY = Math.max(y - r,     0);
                int addPx = pix[addY * w + x];
                int remPx = pix[remY * w + x];
                aSum += ((addPx >>> 24) & 0xFF) - ((remPx >>> 24) & 0xFF);
                rSum += ((addPx >> 16)  & 0xFF) - ((remPx >> 16)  & 0xFF);
                gSum += ((addPx >> 8)   & 0xFF) - ((remPx >> 8)   & 0xFF);
                bSum += ( addPx         & 0xFF) - ( remPx         & 0xFF);
            }
        }
    }

    // ── Keep the old signature so VirtualNode.boxBlur() still compiles ──────────
    @SuppressWarnings("unused")
    public static Bitmap _unused_boxBlur_compat(Bitmap src, int radius) {
        return boxBlur(src, radius);
    }




    private static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId, int width, int height) {
        try {
            Drawable drawable = ContextCompat.getDrawable(context, drawableId);
            if (drawable != null) {
                if (drawable instanceof BitmapDrawable) {
                    return ((BitmapDrawable) drawable).getBitmap();
                }
                int w = width > 0 ? width : drawable.getIntrinsicWidth();
                int h = height > 0 ? height : drawable.getIntrinsicHeight();
                if (w <= 0) w = 100;
                if (h <= 0) h = 100;
                Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
                return bitmap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
