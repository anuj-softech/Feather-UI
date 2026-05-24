package com.rock.featherui.lib.pool;

import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import java.util.ArrayList;
import java.util.List;

/**
 * High-performance object pool to ensure zero allocations during layout and render cycles.
 */
public class ObjectPool {

    private static final int MAX_POOL_SIZE = 128;

    private static final List<Rect> rectPool = new ArrayList<>(MAX_POOL_SIZE);
    private static final List<RectF> rectFPool = new ArrayList<>(MAX_POOL_SIZE);
    private static final List<Paint> paintPool = new ArrayList<>(MAX_POOL_SIZE);
    private static final List<Matrix> matrixPool = new ArrayList<>(MAX_POOL_SIZE);

    static {
        for (int i = 0; i < MAX_POOL_SIZE; i++) {
            rectPool.add(new Rect());
            rectFPool.add(new RectF());
            paintPool.add(new Paint(Paint.ANTI_ALIAS_FLAG));
            matrixPool.add(new Matrix());
        }
    }

    public static synchronized Rect acquireRect() {
        if (!rectPool.isEmpty()) {
            return rectPool.remove(rectPool.size() - 1);
        }
        return new Rect();
    }

    public static synchronized void release(Rect rect) {
        if (rect != null && rectPool.size() < MAX_POOL_SIZE) {
            rect.setEmpty();
            rectPool.add(rect);
        }
    }

    public static synchronized RectF acquireRectF() {
        if (!rectFPool.isEmpty()) {
            return rectFPool.remove(rectFPool.size() - 1);
        }
        return new RectF();
    }

    public static synchronized void release(RectF rectF) {
        if (rectF != null && rectFPool.size() < MAX_POOL_SIZE) {
            rectF.setEmpty();
            rectFPool.add(rectF);
        }
    }

    public static synchronized Paint acquirePaint() {
        if (!paintPool.isEmpty()) {
            Paint paint = paintPool.remove(paintPool.size() - 1);
            paint.reset();
            paint.setAntiAlias(true);
            return paint;
        }
        return new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    public static synchronized void release(Paint paint) {
        if (paint != null && paintPool.size() < MAX_POOL_SIZE) {
            paint.reset();
            paintPool.add(paint);
        }
    }

    public static synchronized Matrix acquireMatrix() {
        if (!matrixPool.isEmpty()) {
            Matrix matrix = matrixPool.remove(matrixPool.size() - 1);
            matrix.reset();
            return matrix;
        }
        return new Matrix();
    }

    public static synchronized void release(Matrix matrix) {
        if (matrix != null && matrixPool.size() < MAX_POOL_SIZE) {
            matrix.reset();
            matrixPool.add(matrix);
        }
    }
}
