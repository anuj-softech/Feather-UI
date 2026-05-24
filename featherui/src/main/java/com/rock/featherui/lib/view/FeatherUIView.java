package com.rock.featherui.lib.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.VelocityTracker;
import com.rock.featherui.lib.animation.AnimationEngine;
import com.rock.featherui.lib.controller.UILayout;
import com.rock.featherui.lib.focus.SpatialFocusEngine;
import com.rock.featherui.lib.node.ButtonNode;
import com.rock.featherui.lib.node.GridViewNode;
import com.rock.featherui.lib.node.ListViewNode;
import com.rock.featherui.lib.node.VirtualNode;
import com.rock.featherui.lib.node.LinearLayoutNode;
import com.rock.featherui.lib.node.ImageViewNode;
import java.util.List;

public class FeatherUIView extends SurfaceView implements SurfaceHolder.Callback {

    private UILayout uiLayout;
    private RenderThread renderThread;
    private VirtualNode focusedNode;

    private boolean isSurfaceReady = false;
    private boolean needsLayout = true;
    private final Rect dirtyRect = new Rect();
    private boolean hasDirtyRect = false;

    // Touch scrolling state
    private float lastTouchX;
    private float lastTouchY;
    private VirtualNode lastTouchDownNode;
    private VirtualNode activeTouchScrollNode;
    private int initialScrollOffset;
    private VelocityTracker velocityTracker;
    private boolean isDragging = false;
    private static final int DRAG_SLOP = 8; // px before recognizing as scroll drag

    public FeatherUIView(Context context) {
        super(context);
        init();
    }

    public FeatherUIView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FeatherUIView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        getHolder().addCallback(this);
        setFocusable(true);
        setFocusableInTouchMode(true);
        AnimationEngine.setHostView(this);
        ImageViewNode.appContext = getContext().getApplicationContext();
        ImageViewNode.globalLoadListener = new ImageViewNode.ImageLoadListener() {
            @Override
            public void onImageLoaded(ImageViewNode node, android.graphics.Bitmap bitmap) {
                needsLayout = true;
                triggerRender(null);
            }
        };
    }

    public void setLayout(UILayout layout) {
        this.uiLayout = layout;
        if (layout != null) {
            layout.setHostView(this);
            needsLayout = true;
            // Focus on first focusable item
            VirtualNode first = SpatialFocusEngine.findFirstFocusable(layout.getRootNode());
            if (first != null) {
                setFocusedNode(first, false);
            }
        }
        triggerRender(null);
    }

    public UILayout getUILayout() {
        return uiLayout;
    }

    public void requestLayoutOrPaint(VirtualNode node) {
        needsLayout = true;
        triggerRender(null);
    }

    public synchronized void requestLayoutOrPaint(VirtualNode node, Rect dirty) {
        if (dirty != null) {
            if (hasDirtyRect) {
                dirtyRect.union(dirty);
            } else {
                dirtyRect.set(dirty);
                hasDirtyRect = true;
            }
        }
        triggerRender(dirty);
    }

    private void triggerRender(Rect dirty) {
        if (renderThread != null) {
            renderThread.requestRender(dirty);
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        isSurfaceReady = true;
        renderThread = new RenderThread(holder);
        renderThread.start();
        triggerRender(null);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        needsLayout = true;
        triggerRender(null);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        isSurfaceReady = false;
        if (renderThread != null) {
            renderThread.quit();
            renderThread = null;
        }
    }

    // Touch Handling (Self-sufficient touch system for mobile with fling support)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (uiLayout == null || uiLayout.getRootNode() == null) {
            return super.onTouchEvent(event);
        }

        // Feed VelocityTracker on every event
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(event);

        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastTouchX = x;
                lastTouchY = y;
                isDragging = false;
                lastTouchDownNode = SpatialFocusEngine.findHitNode(uiLayout.getRootNode(), x, y);

                // Find if touch started inside a scrollable container
                activeTouchScrollNode = null;
                VirtualNode temp = lastTouchDownNode;
                while (temp != null) {
                    if (temp instanceof ListViewNode || temp instanceof GridViewNode) {
                        activeTouchScrollNode = temp;
                        break;
                    }
                    temp = temp.parent;
                }
                if (activeTouchScrollNode != null) {
                    if (activeTouchScrollNode instanceof ListViewNode) {
                        ListViewNode lv = (ListViewNode) activeTouchScrollNode;
                        initialScrollOffset = lv.scrollOffset;
                        lv.scrollVelocity = 0f; // cancel any existing fling
                    } else {
                        GridViewNode gv = (GridViewNode) activeTouchScrollNode;
                        initialScrollOffset = gv.scrollOffset;
                        gv.scrollVelocity = 0f;
                    }
                }

                if (lastTouchDownNode != null) {
                    setFocusedNode(lastTouchDownNode, true);
                    return true;
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                float moveDistX = x - lastTouchX;
                float moveDistY = y - lastTouchY;

                if (!isDragging) {
                    // Recognize drag only after slop threshold
                    if (Math.abs(moveDistX) > DRAG_SLOP || Math.abs(moveDistY) > DRAG_SLOP) {
                        isDragging = true;
                    }
                }

                if (isDragging && activeTouchScrollNode != null) {
                    float totalDx = lastTouchX - x;
                    float totalDy = lastTouchY - y;

                    if (activeTouchScrollNode instanceof ListViewNode) {
                        ListViewNode lv = (ListViewNode) activeTouchScrollNode;
                        boolean isVertical = LinearLayoutNode.VERTICAL.equalsIgnoreCase(lv.orientation);
                        if (lv.measuredWidth > 0) {
                            int totalItems = lv.getItemsData().size();
                            int lvContentSize = (isVertical ? lv.measuredHeight : lv.measuredWidth) - lv.paddingLeft - lv.paddingRight;
                            List<VirtualNode> activeNodes = lv.getActiveNodes();
                            int activeItemSize = (activeNodes != null && !activeNodes.isEmpty()) ?
                                (isVertical ? (activeNodes.get(0).measuredHeight + activeNodes.get(0).marginTop + activeNodes.get(0).marginBottom)
                                            : (activeNodes.get(0).measuredWidth + activeNodes.get(0).marginLeft + activeNodes.get(0).marginRight)) : 100;
                            int stepSize = activeItemSize + lv.gap;
                            int maxScroll = Math.max(0, totalItems * stepSize - lv.gap - lvContentSize);
                            float delta = isVertical ? totalDy : totalDx;
                            
                            float rawOffset = initialScrollOffset + delta;
                            float finalOffset;
                            if (rawOffset < 0) {
                                finalOffset = rawOffset * 0.38f; // iOS rubber-banding resistance
                            } else if (rawOffset > maxScroll) {
                                finalOffset = maxScroll + (rawOffset - maxScroll) * 0.38f;
                            } else {
                                finalOffset = rawOffset;
                            }
                            lv.targetScrollOffset = (int) finalOffset;
                            lv.scrollOffset = (int) finalOffset;
                        }
                    } else {
                        GridViewNode gv = (GridViewNode) activeTouchScrollNode;
                        if (gv.measuredWidth > 0) {
                            int totalItems = gv.getItemsData().size();
                            int columns = gv.columns;
                            int totalRows = (totalItems + columns - 1) / columns;
                            List<VirtualNode> activeNodes = gv.getActiveNodes();
                            int activeItemHeight = (activeNodes != null && !activeNodes.isEmpty()) ?
                                (activeNodes.get(0).measuredHeight + activeNodes.get(0).marginTop + activeNodes.get(0).marginBottom) : 100;
                            int rowHeight = activeItemHeight + gv.gap;
                            int gvContentSize = gv.measuredHeight - gv.paddingTop - gv.paddingBottom;
                            int maxScroll = Math.max(0, totalRows * rowHeight - gv.gap - gvContentSize);
                            
                            float rawOffset = initialScrollOffset + totalDy;
                            float finalOffset;
                            if (rawOffset < 0) {
                                finalOffset = rawOffset * 0.38f; // iOS rubber-banding resistance
                            } else if (rawOffset > maxScroll) {
                                finalOffset = maxScroll + (rawOffset - maxScroll) * 0.38f;
                            } else {
                                finalOffset = rawOffset;
                            }
                            gv.targetScrollOffset = (int) finalOffset;
                            gv.scrollOffset = (int) finalOffset;
                        }
                    }
                    needsLayout = true;
                    triggerRender(null);
                    return true;
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && activeTouchScrollNode != null) {
                    // Compute fling velocity and inject into scroll node
                    velocityTracker.computeCurrentVelocity(16); // pixels per frame (~60fps)
                    float vx = velocityTracker.getXVelocity();
                    float vy = velocityTracker.getYVelocity();

                    if (activeTouchScrollNode instanceof ListViewNode) {
                        ListViewNode lv = (ListViewNode) activeTouchScrollNode;
                        boolean isVertical = LinearLayoutNode.VERTICAL.equalsIgnoreCase(lv.orientation);
                        float flingVelocity = isVertical ? -vy : -vx;
                        // Only inject if fast enough to feel like a fling (>2 px/frame)
                        if (Math.abs(flingVelocity) > 2f) {
                            lv.scrollVelocity = flingVelocity;
                            // Keep targetScrollOffset at current position so fling coasts freely
                            lv.targetScrollOffset = lv.scrollOffset;
                        }
                    } else if (activeTouchScrollNode instanceof GridViewNode) {
                        GridViewNode gv = (GridViewNode) activeTouchScrollNode;
                        if (Math.abs(vy) > 2f) {
                            gv.scrollVelocity = -vy;
                            gv.targetScrollOffset = gv.scrollOffset;
                        }
                    }

                    needsLayout = true;
                    triggerRender(null);
                    activeTouchScrollNode = null;
                    isDragging = false;
                } else if (!isDragging) {
                    // Tap (no drag) — trigger click
                    VirtualNode hitUp = SpatialFocusEngine.findHitNode(uiLayout.getRootNode(), x, y);
                    if (hitUp != null && hitUp == lastTouchDownNode && hitUp.onClickListener != null) {
                        hitUp.onClickListener.onClick(hitUp);
                    }
                }

                velocityTracker.recycle();
                velocityTracker = null;
                isDragging = false;
                return true;
        }

        return super.onTouchEvent(event);
    }

    // D-Pad Navigation (Self-sufficient key system for Android TV)
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (uiLayout == null || uiLayout.getRootNode() == null) {
            return super.onKeyDown(keyCode, event);
        }

        int direction = 0;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP:
                direction = SpatialFocusEngine.DIR_UP;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                direction = SpatialFocusEngine.DIR_DOWN;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                direction = SpatialFocusEngine.DIR_LEFT;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                direction = SpatialFocusEngine.DIR_RIGHT;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (focusedNode != null && focusedNode.onClickListener != null) {
                    focusedNode.onClickListener.onClick(focusedNode);
                    return true;
                }
                break;
        }

        if (direction != 0) {
            // Check if current focused node is in a ListView or GridView
            ListViewNode parentListView = null;
            GridViewNode parentGridView = null;
            VirtualNode temp = focusedNode;
            while (temp != null) {
                if (temp instanceof ListViewNode) {
                    parentListView = (ListViewNode) temp;
                    break;
                } else if (temp instanceof GridViewNode) {
                    parentGridView = (GridViewNode) temp;
                    break;
                }
                temp = temp.parent;
            }

            if (parentListView != null) {
                boolean isVertical = LinearLayoutNode.VERTICAL.equalsIgnoreCase(parentListView.orientation);
                int curIndex = focusedNode.boundIndex;
                int totalItems = parentListView.getItemsData().size();
                int nextIndex = -1;

                if (isVertical) {
                    if (direction == SpatialFocusEngine.DIR_DOWN) nextIndex = curIndex + 1;
                    else if (direction == SpatialFocusEngine.DIR_UP) nextIndex = curIndex - 1;
                } else {
                    if (direction == SpatialFocusEngine.DIR_RIGHT) nextIndex = curIndex + 1;
                    else if (direction == SpatialFocusEngine.DIR_LEFT) nextIndex = curIndex - 1;
                }

                if (nextIndex >= 0 && nextIndex < totalItems) {
                    // Navigate internally within the list view
                    parentListView.focusedItemIndex = nextIndex;
                    parentListView.requestChildFocus(nextIndex);

                    needsLayout = true;
                    triggerRender(null);
                    return true;
                }
            } else if (parentGridView != null) {
                int curIndex = focusedNode.boundIndex;
                int totalItems = parentGridView.getItemsData().size();
                int columns = parentGridView.columns;
                int nextIndex = -1;

                if (direction == SpatialFocusEngine.DIR_RIGHT) {
                    if ((curIndex % columns) + 1 < columns) nextIndex = curIndex + 1;
                } else if (direction == SpatialFocusEngine.DIR_LEFT) {
                    if ((curIndex % columns) - 1 >= 0) nextIndex = curIndex - 1;
                } else if (direction == SpatialFocusEngine.DIR_DOWN) {
                    nextIndex = curIndex + columns;
                } else if (direction == SpatialFocusEngine.DIR_UP) {
                    nextIndex = curIndex - columns;
                }

                if (nextIndex >= 0 && nextIndex < totalItems) {
                    parentGridView.focusedItemIndex = nextIndex;
                    parentGridView.requestChildFocus(nextIndex);

                    needsLayout = true;
                    triggerRender(null);
                    return true;
                }
            }

            // Default spatial focus navigation
            VirtualNode nextFocus = SpatialFocusEngine.findNextFocus(uiLayout.getRootNode(), focusedNode, direction);
            if (nextFocus != null) {
                setFocusedNode(nextFocus, true);
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void setFocusedNode(VirtualNode node, boolean animate) {
        if (node == focusedNode) return;

        if (focusedNode != null) {
            if (focusedNode instanceof ButtonNode) {
                ((ButtonNode) focusedNode).setFocusState(false, animate);
            } else {
                focusedNode.focused = false;
            }
            if (focusedNode.onFocusChangeListener != null) {
                focusedNode.onFocusChangeListener.onFocusChange(focusedNode, false);
            }
        }

        focusedNode = node;

        if (uiLayout != null && uiLayout.getRootNode() != null) {
            clearAllListFocusedIndices(uiLayout.getRootNode());
        }

        if (focusedNode != null) {
            if (focusedNode instanceof ButtonNode) {
                ((ButtonNode) focusedNode).setFocusState(true, animate);
            } else {
                focusedNode.focused = true;
            }

            // Scroll management
            adjustListViewOrGridViewScroll(focusedNode);

            if (focusedNode.onFocusChangeListener != null) {
                focusedNode.onFocusChangeListener.onFocusChange(focusedNode, true);
            }
        }

        needsLayout = true;
        triggerRender(null);
    }

    private void adjustListViewOrGridViewScroll(VirtualNode node) {
        VirtualNode parent = node.parent;
        while (parent != null) {
            if (parent instanceof ListViewNode) {
                ListViewNode lv = (ListViewNode) parent;
                lv.focusedItemIndex = node.boundIndex;
                lv.requestChildFocus(node);
                break;
            } else if (parent instanceof GridViewNode) {
                GridViewNode gv = (GridViewNode) parent;
                gv.focusedItemIndex = node.boundIndex;
                gv.requestChildFocus(node);
                break;
            }
            parent = parent.parent;
        }
    }

    private boolean checkScrolling(VirtualNode node) {
        if (node == null) return false;
        if (node instanceof ListViewNode) {
            ListViewNode lv = (ListViewNode) node;
            if (lv.scrollOffset != lv.targetScrollOffset || Math.abs(lv.scrollVelocity) > 0.5f) {
                return true;
            }
        } else if (node instanceof GridViewNode) {
            GridViewNode gv = (GridViewNode) node;
            if (gv.scrollOffset != gv.targetScrollOffset || Math.abs(gv.scrollVelocity) > 0.5f) {
                return true;
            }
        }
        for (int i = 0; i < node.children.size(); i++) {
            if (checkScrolling(node.children.get(i))) {
                return true;
            }
        }
        return false;
    }

    private void syncFocusedNode() {
        if (uiLayout == null || uiLayout.getRootNode() == null) return;
        VirtualNode activeListOrGrid = findActiveFocusedListOrGrid(uiLayout.getRootNode());
        if (activeListOrGrid instanceof ListViewNode) {
            ListViewNode lv = (ListViewNode) activeListOrGrid;
            VirtualNode oldFocused = focusedNode;
            VirtualNode newFocused = null;
            for (VirtualNode node : lv.getActiveNodes()) {
                if (node.boundIndex == lv.focusedItemIndex) {
                    newFocused = node;
                    break;
                }
            }
            if (newFocused != null && newFocused != oldFocused) {
                final VirtualNode finalOld = oldFocused;
                final VirtualNode finalNew = newFocused;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (finalOld != null) {
                        if (finalOld instanceof ButtonNode) {
                            ((ButtonNode) finalOld).setFocusState(false, true);
                        } else {
                            finalOld.focused = false;
                        }
                        if (finalOld.onFocusChangeListener != null) {
                            finalOld.onFocusChangeListener.onFocusChange(finalOld, false);
                        }
                    }
                    if (finalNew != null) {
                        if (finalNew instanceof ButtonNode) {
                            ((ButtonNode) finalNew).setFocusState(true, true);
                        } else {
                            finalNew.focused = true;
                        }
                        if (finalNew.onFocusChangeListener != null) {
                            finalNew.onFocusChangeListener.onFocusChange(finalNew, true);
                        }
                    }
                });
                focusedNode = newFocused;
            }
        } else if (activeListOrGrid instanceof GridViewNode) {
            GridViewNode gv = (GridViewNode) activeListOrGrid;
            VirtualNode oldFocused = focusedNode;
            VirtualNode newFocused = null;
            for (VirtualNode node : gv.getActiveNodes()) {
                if (node.boundIndex == gv.focusedItemIndex) {
                    newFocused = node;
                    break;
                }
            }
            if (newFocused != null && newFocused != oldFocused) {
                final VirtualNode finalOld = oldFocused;
                final VirtualNode finalNew = newFocused;
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    if (finalOld != null) {
                        if (finalOld instanceof ButtonNode) {
                            ((ButtonNode) finalOld).setFocusState(false, true);
                        } else {
                            finalOld.focused = false;
                        }
                        if (finalOld.onFocusChangeListener != null) {
                            finalOld.onFocusChangeListener.onFocusChange(finalOld, false);
                        }
                    }
                    if (finalNew != null) {
                        if (finalNew instanceof ButtonNode) {
                            ((ButtonNode) finalNew).setFocusState(true, true);
                        } else {
                            finalNew.focused = true;
                        }
                        if (finalNew.onFocusChangeListener != null) {
                            finalNew.onFocusChangeListener.onFocusChange(finalNew, true);
                        }
                    }
                });
                focusedNode = newFocused;
            }
        }
    }

    private VirtualNode findActiveFocusedListOrGrid(VirtualNode node) {
        if (node == null) return null;
        if (node instanceof ListViewNode) {
            ListViewNode lv = (ListViewNode) node;
            if (lv.focusedItemIndex != -1) return lv;
        } else if (node instanceof GridViewNode) {
            GridViewNode gv = (GridViewNode) node;
            if (gv.focusedItemIndex != -1) return gv;
        }
        for (int i = 0; i < node.children.size(); i++) {
            VirtualNode found = findActiveFocusedListOrGrid(node.children.get(i));
            if (found != null) return found;
        }
        return null;
    }

    private void clearAllListFocusedIndices(VirtualNode node) {
        if (node == null) return;
        if (node instanceof ListViewNode) {
            ((ListViewNode) node).focusedItemIndex = -1;
        } else if (node instanceof GridViewNode) {
            ((GridViewNode) node).focusedItemIndex = -1;
        }
        for (int i = 0; i < node.children.size(); i++) {
            clearAllListFocusedIndices(node.children.get(i));
        }
    }

    // High performance background Render Loop
    private class RenderThread extends Thread {
        private final SurfaceHolder surfaceHolder;
        private boolean running = true;
        private boolean isDirty = false;
        private final Rect pendingDirty = new Rect();
        private boolean usePendingDirty = false;

        public RenderThread(SurfaceHolder holder) {
            this.surfaceHolder = holder;
        }

        public synchronized void requestRender(Rect dirty) {
            isDirty = true;
            if (dirty != null) {
                if (usePendingDirty) {
                    pendingDirty.union(dirty);
                } else {
                    pendingDirty.set(dirty);
                    usePendingDirty = true;
                }
            } else {
                usePendingDirty = false; // full redraw
            }
            notifyAll();
        }

        public synchronized void quit() {
            running = false;
            notifyAll();
        }

        @Override
        public void run() {
            while (running) {
                synchronized (this) {
                    while (running && !isDirty) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            // ignore
                        }
                    }
                    if (!running) break;
                    isDirty = false;
                }

                if (!isSurfaceReady || uiLayout == null || uiLayout.getRootNode() == null) {
                    continue;
                }

                Canvas canvas = null;
                try {
                    synchronized (surfaceHolder) {
                        // Lock canvas
                        Rect lockRect = null;
                        synchronized (this) {
                            if (usePendingDirty) {
                                lockRect = new Rect(pendingDirty);
                                usePendingDirty = false;
                            }
                        }

                        if (lockRect != null) {
                            canvas = surfaceHolder.lockCanvas(lockRect);
                        } else {
                            canvas = surfaceHolder.lockCanvas();
                        }

                        if (canvas != null) {
                            // Clear canvas or draw background
                            canvas.drawColor(Color.TRANSPARENT, android.graphics.PorterDuff.Mode.CLEAR);
                            canvas.drawColor(0xFF0F172A); // Slate 900 Background for TV premium feel

                            // Run layout/measure pass on render thread before drawing if requested
                            if (needsLayout) {
                                int w = getWidth();
                                int h = getHeight();
                                uiLayout.getRootNode().measure(w, h);
                                uiLayout.getRootNode().layout(0, 0);
                                syncFocusedNode();
                                needsLayout = false;
                            }

                            // Draw root node (recursively draws subtree)
                            uiLayout.getRootNode().draw(canvas);

                            // Check scrolling animation and tick if active
                            boolean isScrolling = checkScrolling(uiLayout.getRootNode());
                            if (isScrolling) {
                                requestRender(null);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (canvas != null) {
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }
            }
        }
    }
}
