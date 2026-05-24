package com.rock.featherui.lib.node;

import android.graphics.Canvas;
import com.rock.featherui.lib.parser.JSONLayoutInflater;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GridViewNode extends VirtualNode {

    public int columns = 1;
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
    public int itemWidth = 0;
    public int itemHeight = 0;

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

        if (itemsData.isEmpty() || itemTemplateJson.isEmpty() || columns <= 0) {
            return;
        }

        // Calculate item width based on columns and gaps
        int contentWidth = measuredWidth - paddingLeft - paddingRight;
        itemWidth = (contentWidth - (columns - 1) * gap) / columns;

        // Measure one dummy template to find item height
        if (recycledNodes.isEmpty()) {
            VirtualNode dummy = JSONLayoutInflater.inflate(itemTemplateJson);
            if (dummy != null) {
                dummy.parent = this;
                dummy.width = itemWidth;
                dummy.measure(itemWidth, measuredHeight);
                itemHeight = dummy.measuredHeight + dummy.marginTop + dummy.marginBottom;
                recycledNodes.add(dummy);
            }
        }

        // Determine how many nodes can be visible at once
        if (itemHeight > 0) {
            int visibleRows = (measuredHeight / itemHeight) + 2;
            int visibleCount = visibleRows * columns;
            while (recycledNodes.size() < visibleCount && recycledNodes.size() < itemsData.size()) {
                VirtualNode node = JSONLayoutInflater.inflate(itemTemplateJson);
                if (node != null) {
                    node.parent = this;
                    node.width = itemWidth;
                    recycledNodes.add(node);
                }
            }
        }

        // Measure all recycled active nodes
        for (int i = 0; i < recycledNodes.size(); i++) {
            recycledNodes.get(i).measure(itemWidth, measuredHeight);
        }
    }

    @Override
    protected void onLayout(int absoluteLeft, int absoluteTop) {
        if (itemsData.isEmpty() || recycledNodes.isEmpty() || itemHeight <= 0 || columns <= 0) return;

        int totalItems = itemsData.size();
        int rowHeight = itemHeight + gap;
        int totalRows = (totalItems + columns - 1) / columns;
        int contentSize = measuredHeight - paddingTop - paddingBottom;
        int maxScroll = Math.max(0, totalRows * rowHeight - gap - contentSize);

        // Check if we are currently in an overscroll state
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

        // Determine rows currently visible in viewport
        int startRow = scrollOffset / rowHeight;
        if (startRow < 0) startRow = 0;
        int endRow = (scrollOffset + measuredHeight) / rowHeight;
        if (endRow >= totalRows) endRow = totalRows - 1;

        int startIndex = startRow * columns;
        int endIndex = Math.min((endRow + 1) * columns - 1, totalItems - 1);

        int activeNodeCount = recycledNodes.size();

        for (int index = startIndex; index <= endIndex; index++) {
            int poolIndex = index % activeNodeCount;
            VirtualNode node = recycledNodes.get(poolIndex);

            // Bind data to node
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

            int col = index % columns;
            int row = index / columns;

            int itemLeft = col * (itemWidth + gap) - node.marginLeft;
            int itemTop = row * rowHeight - scrollOffset + node.marginTop;

            int childLeft = absoluteLeft + paddingLeft + itemLeft;
            int childTop = absoluteTop + paddingTop + itemTop;

            node.layout(childLeft, childTop);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        if (itemsData.isEmpty() || recycledNodes.isEmpty()) return;

        // Perform local layout pass dynamically
        onLayout(left, top);

        // Clip grid boundaries during draw with ±50px overflow on all sides.
        // The floating side panel renders last (highest Z), naturally masking any overflow.
        canvas.save();
        canvas.clipRect(left - 50, top - 50, right + 50, bottom + 50);

        int totalItems = itemsData.size();
        int rowHeight = itemHeight + gap;

        int startRow = scrollOffset / rowHeight;
        if (startRow < 0) startRow = 0;
        int endRow = (scrollOffset + measuredHeight) / rowHeight;
        int totalRows = (totalItems + columns - 1) / columns;
        if (endRow >= totalRows) endRow = totalRows - 1;

        int startIndex = startRow * columns;
        int endIndex = Math.min((endRow + 1) * columns - 1, totalItems - 1);

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

        int row = index / columns;
        int rowHeight = itemHeight + gap;
        int startPos = row * rowHeight;
        int endPos = startPos + itemHeight;

        int contentSize = measuredHeight - paddingTop - paddingBottom;

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
            int itemCenter = startPos + itemHeight / 2;
            targetScrollOffset = itemCenter - Math.round(contentSize * alignFraction);
        }

        // Clamp scroll offset
        int totalItems = itemsData.size();
        int totalRows = (totalItems + columns - 1) / columns;
        int maxScroll = Math.max(0, totalRows * rowHeight - gap - contentSize);
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
}
