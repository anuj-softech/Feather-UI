package com.rock.featherui.lib.node;

import android.graphics.Canvas;
import com.rock.featherui.lib.parser.JSONLayoutInflater;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ListViewNode extends VirtualNode {

    public String orientation = LinearLayoutNode.HORIZONTAL; // TV lists scroll horizontally
    public int gap = 0;
    public String scrollAlignment = "center"; // "start", "center", "end"
    public String scrollCenterOffset = "50%"; // e.g. "10%", "20%", default 50%

    // Recycler dataset
    private final List<Map<String, Object>> itemsData = new ArrayList<>();
    private String itemTemplateJson = "";

    // Scroll state
    public int scrollOffset = 0;
    public int targetScrollOffset = 0;
    public int focusedItemIndex = -1;
    public float scrollVelocity = 0f;   // pixels/frame — set externally for fling

    // Recycled child views
    private final List<VirtualNode> recycledNodes = new ArrayList<>();
    public int itemSize = 0; // width for horizontal, height for vertical

    public void setItemsData(List<Map<String, Object>> data, String templateJson) {
        this.itemsData.clear();
        if (data != null) {
            this.itemsData.addAll(data);
        }
        this.itemTemplateJson = templateJson;
        this.recycledNodes.clear();
        this.scrollOffset = 0;
        this.targetScrollOffset = 0;
        this.scrollVelocity = 0f;
    }

    public List<Map<String, Object>> getItemsData() {
        return itemsData;
    }

    @Override
    protected void onMeasure(int parentWidth, int parentHeight) {
        int availWidth = (width == MATCH_PARENT) ? parentWidth : parentWidth - marginLeft - marginRight;
        int availHeight = (height == MATCH_PARENT) ? parentHeight : parentHeight - marginTop - marginBottom;

        measuredWidth = (width >= 0) ? width : availWidth;
        measuredHeight = (height >= 0) ? height : availHeight;

        if (itemsData.isEmpty() || itemTemplateJson.isEmpty()) {
            return;
        }

        // Measure one dummy template to find item size
        if (recycledNodes.isEmpty()) {
            VirtualNode dummy = JSONLayoutInflater.inflate(itemTemplateJson);
            if (dummy != null) {
                dummy.parent = this;
                setupNodeListeners(dummy);
                dummy.measure(measuredWidth, measuredHeight);
                itemSize = LinearLayoutNode.VERTICAL.equalsIgnoreCase(orientation) ?
                        dummy.measuredHeight + dummy.marginTop + dummy.marginBottom :
                        dummy.measuredWidth + dummy.marginLeft + childMarginRight(dummy);
                recycledNodes.add(dummy);
            }
        }

        // Determine how many nodes can be visible at once
        int viewportSize = LinearLayoutNode.VERTICAL.equalsIgnoreCase(orientation) ? measuredHeight : measuredWidth;
        if (itemSize > 0) {
            int visibleCount = (viewportSize / itemSize) + 2;
            while (recycledNodes.size() < visibleCount && recycledNodes.size() < itemsData.size()) {
                VirtualNode node = JSONLayoutInflater.inflate(itemTemplateJson);
                if (node != null) {
                    node.parent = this;
                    setupNodeListeners(node);
                    recycledNodes.add(node);
                }
            }
        }

        // Bind and measure the active recycled nodes that are currently in the viewport
        int step = itemSize + gap;
        if (step > 0 && !recycledNodes.isEmpty()) {
            int startIndex = scrollOffset / step;
            if (startIndex < 0) startIndex = 0;
            int endIndex = (scrollOffset + viewportSize) / step;
            if (endIndex >= itemsData.size()) endIndex = itemsData.size() - 1;

            int activeNodeCount = recycledNodes.size();
            for (int index = startIndex; index <= endIndex; index++) {
                int poolIndex = index % activeNodeCount;
                VirtualNode node = recycledNodes.get(poolIndex);
                bindData(node, itemsData.get(index));
                node.boundIndex = index;
                node.measure(measuredWidth, measuredHeight);
            }
        }
    }

    private int childMarginRight(VirtualNode node) {
        return node.marginRight;
    }

    @Override
    protected void onLayout(int absoluteLeft, int absoluteTop) {
        if (itemsData.isEmpty() || recycledNodes.isEmpty() || itemSize <= 0) return;

        int totalItems = itemsData.size();
        int step = itemSize + gap;
        boolean isVertical = LinearLayoutNode.VERTICAL.equalsIgnoreCase(orientation);
        int viewportSize = isVertical ? measuredHeight : measuredWidth;
        int contentSize = viewportSize - (isVertical ? paddingTop + paddingBottom : paddingLeft + paddingRight);
        int maxScroll = Math.max(0, totalItems * step - gap - contentSize);

        // Check if we are currently in an overscroll state (out of bounds)
        boolean isOverscrolled = (scrollOffset < 0 || scrollOffset > maxScroll);

        if (isOverscrolled) {
            // Apply iOS bounce-back spring physics to return to bounds
            int target = (scrollOffset < 0) ? 0 : maxScroll;
            int delta = target - scrollOffset;
            scrollVelocity = scrollVelocity * 0.72f + delta * 0.18f;
            scrollOffset += (int) scrollVelocity;
            
            if (Math.abs(delta) <= 1 && Math.abs(scrollVelocity) < 0.5f) {
                scrollOffset = target;
                targetScrollOffset = target;
                scrollVelocity = 0f;
            }
        } else if (scrollOffset != targetScrollOffset || Math.abs(scrollVelocity) > 0.5f) {
            if (Math.abs(scrollVelocity) > 0.5f && scrollOffset == targetScrollOffset) {
                // Free fling: coast with friction
                scrollVelocity *= 0.94f; // Smooth iOS-like deceleration
                scrollOffset += (int) scrollVelocity;
                targetScrollOffset = scrollOffset;
                
                if (Math.abs(scrollVelocity) < 0.5f) scrollVelocity = 0f;
            } else {
                // Ease-out snap: D-pad navigation or target lock
                int delta = targetScrollOffset - scrollOffset;
                if (Math.abs(delta) <= 1) {
                    scrollOffset = targetScrollOffset;
                    scrollVelocity = 0f;
                } else {
                    int easeStep = Math.round(delta * 0.25f);
                    if (easeStep == 0) easeStep = (delta > 0) ? 1 : -1;
                    scrollOffset += easeStep;
                    scrollVelocity = easeStep;
                }
            }
        } else {
            scrollVelocity = 0f;
        }

        // Find which items are currently in viewport
        int startIndex = scrollOffset / step;
        if (startIndex < 0) startIndex = 0;
        int endIndex = (scrollOffset + viewportSize) / step;
        if (endIndex >= totalItems) endIndex = totalItems - 1;

        int activeNodeCount = recycledNodes.size();

        // Layout visible items, recycling the virtual nodes
        for (int index = startIndex; index <= endIndex; index++) {
            int poolIndex = index % activeNodeCount;
            VirtualNode node = recycledNodes.get(poolIndex);

            // Bind data to the node for this index
            bindData(node, itemsData.get(index));
            node.boundIndex = index;

            if (index == focusedItemIndex) {
                if (node instanceof ButtonNode) {
                    ((ButtonNode) node).setFocusState(true, false);
                } else {
                    node.focused = true;
                }
            } else {
                if (node instanceof ButtonNode) {
                    ((ButtonNode) node).setFocusState(false, false);
                } else {
                    node.focused = false;
                }
            }

            int itemPosition = index * step - scrollOffset;

            int childLeft, childTop;
            if (LinearLayoutNode.VERTICAL.equalsIgnoreCase(orientation)) {
                childLeft = absoluteLeft + paddingLeft + node.marginLeft;
                childTop = absoluteTop + paddingTop + itemPosition + node.marginTop;
            } else {
                childLeft = absoluteLeft + paddingLeft + itemPosition + node.marginLeft;
                childTop = absoluteTop + paddingTop + node.marginTop;
            }

            node.layout(childLeft, childTop);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (itemsData.isEmpty() || recycledNodes.isEmpty()) return;

        // Perform local layout pass dynamically
        onLayout(left, top);

        // Clip list boundaries during draw with ±50px overflow on all sides.
        // This breathing room allows focus-scale halos and glows to render without hard clipping.
        // The floating side panel is drawn LAST in the Z-order (highest child index in frame_layout),
        // so it naturally paints on top of any list overflow into the side panel's region.
        canvas.save();
        canvas.clipRect(left - 50, top - 50, right + 50, bottom + 50);

        int totalItems = itemsData.size();
        int step = itemSize + gap;
        int viewportSize = LinearLayoutNode.VERTICAL.equalsIgnoreCase(orientation) ? measuredHeight : measuredWidth;
        int startIndex = scrollOffset / step;
        if (startIndex < 0) startIndex = 0;
        int endIndex = (scrollOffset + viewportSize) / step;
        if (endIndex >= totalItems) endIndex = totalItems - 1;

        int activeNodeCount = recycledNodes.size();

        // Draw only visible/active recycled nodes
        for (int index = startIndex; index <= endIndex; index++) {
            int poolIndex = index % activeNodeCount;
            VirtualNode node = recycledNodes.get(poolIndex);
            node.draw(canvas);
        }

        canvas.restore();
    }

    private void bindData(VirtualNode node, Map<String, Object> data) {
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.contains(":")) {
                // Format: "childId:propertyName" -> updates child's property
                String[] parts = key.split(":");
                if (parts.length == 2) {
                    VirtualNode target = findNodeById(node, parts[0]);
                    if (target != null) {
                        target.applyProperty(parts[1], value);
                    }
                }
            } else {
                node.applyProperty(key, value);
            }
        }
    }

    private VirtualNode findNodeById(VirtualNode root, String targetId) {
        if (targetId.equals(root.id)) return root;
        for (int i = 0; i < root.children.size(); i++) {
            VirtualNode found = findNodeById(root.children.get(i), targetId);
            if (found != null) return found;
        }
        return null;
    }

    // Dynamic focus scroll alignment: keeps the focused node inside viewport
    public void requestChildFocus(VirtualNode focusedChild) {
        VirtualNode directChild = focusedChild;
        while (directChild != null && directChild.parent != this) {
            directChild = directChild.parent;
        }
        if (directChild == null) return;
        requestChildFocus(directChild.boundIndex);
    }

    public void requestChildFocus(int index) {
        if (index == -1) return;

        int step = itemSize + gap;
        int startPos = index * step;
        int endPos = startPos + itemSize;

        int contentSize = (LinearLayoutNode.VERTICAL.equalsIgnoreCase(orientation) ? measuredHeight : measuredWidth) - paddingLeft - paddingRight;

        if ("start".equalsIgnoreCase(scrollAlignment)) {
            targetScrollOffset = startPos;
        } else if ("end".equalsIgnoreCase(scrollAlignment)) {
            targetScrollOffset = endPos - contentSize;
        } else {
            // "center" alignment with custom offset percent/ratio (default 50%)
            float alignFraction = 0.5f;
            if (scrollCenterOffset != null) {
                String offsetStr = scrollCenterOffset.trim();
                if (offsetStr.endsWith("%")) {
                    try {
                        alignFraction = Float.parseFloat(offsetStr.replace("%", "").trim()) / 100f;
                    } catch (NumberFormatException e) {}
                } else {
                    try {
                        alignFraction = Float.parseFloat(offsetStr);
                    } catch (NumberFormatException e) {}
                }
            }
            int itemCenter = startPos + itemSize / 2;
            targetScrollOffset = itemCenter - Math.round(contentSize * alignFraction);
        }

        // Clamp scroll offset
        int totalItems = itemsData.size();
        int maxScroll = Math.max(0, totalItems * step - gap - contentSize);
        if (targetScrollOffset < 0) {
            targetScrollOffset = 0;
        } else if (targetScrollOffset > maxScroll) {
            targetScrollOffset = maxScroll;
        }

        // Instant snap for D-Pad navigation (no spring/lerp)
        scrollOffset = targetScrollOffset;
        scrollVelocity = 0f;
    }

    public List<VirtualNode> getActiveNodes() {
        return recycledNodes;
    }

    public interface OnItemClickListener {
        void onItemClick(int index, Map<String, Object> itemData, VirtualNode node);
    }

    public interface OnItemFocusChangeListener {
        void onItemFocusChange(int index, Map<String, Object> itemData, boolean focused, VirtualNode node);
    }

    private OnItemClickListener onItemClickListener;
    private OnItemFocusChangeListener onItemFocusChangeListener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
        for (VirtualNode node : recycledNodes) {
            setupNodeListeners(node);
        }
    }

    public void setOnItemFocusChangeListener(OnItemFocusChangeListener listener) {
        this.onItemFocusChangeListener = listener;
        for (VirtualNode node : recycledNodes) {
            setupNodeListeners(node);
        }
    }

    private void setupNodeListeners(VirtualNode node) {
        node.onClickListener = clickedNode -> {
            int idx = clickedNode.boundIndex;
            if (idx >= 0 && idx < itemsData.size() && onItemClickListener != null) {
                onItemClickListener.onItemClick(idx, itemsData.get(idx), clickedNode);
            }
        };
        node.onFocusChangeListener = (focusedNode, hasFocus) -> {
            int idx = focusedNode.boundIndex;
            if (idx >= 0 && idx < itemsData.size() && onItemFocusChangeListener != null) {
                onItemFocusChangeListener.onItemFocusChange(idx, itemsData.get(idx), hasFocus, focusedNode);
            }
        };
    }
}
