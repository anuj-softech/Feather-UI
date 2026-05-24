package com.rock.featherui.lib.focus;

import com.rock.featherui.lib.node.VirtualNode;
import java.util.ArrayList;
import java.util.List;

public class SpatialFocusEngine {

    public static final int DIR_UP = 1;
    public static final int DIR_DOWN = 2;
    public static final int DIR_LEFT = 3;
    public static final int DIR_RIGHT = 4;

    public static VirtualNode findNextFocus(VirtualNode root, VirtualNode current, int direction) {
        if (root == null || current == null) return null;

        String targetId = null;
        switch (direction) {
            case DIR_UP: targetId = current.nextFocusUpId; break;
            case DIR_DOWN: targetId = current.nextFocusDownId; break;
            case DIR_LEFT: targetId = current.nextFocusLeftId; break;
            case DIR_RIGHT: targetId = current.nextFocusRightId; break;
        }

        if (targetId != null && !targetId.isEmpty()) {
            VirtualNode targetNode = findNodeById(root, targetId);
            if (targetNode != null && targetNode.focusable && targetNode.alpha > 0.01f) {
                return targetNode;
            }
        }

        List<VirtualNode> focusables = new ArrayList<>();
        collectFocusableNodes(root, focusables);
        focusables.remove(current);

        if (focusables.isEmpty()) return null;

        VirtualNode bestCandidate = null;
        float minCost = Float.MAX_VALUE;

        float cx = (current.left + current.right) / 2f;
        float cy = (current.top + current.bottom) / 2f;

        for (int i = 0; i < focusables.size(); i++) {
            VirtualNode candidate = focusables.get(i);
            float tx = (candidate.left + candidate.right) / 2f;
            float ty = (candidate.top + candidate.bottom) / 2f;

            float dx = tx - cx;
            float dy = ty - cy;

            boolean isValidDirection = false;
            float cost = 0f;

            switch (direction) {
                case DIR_UP:
                    if (ty < cy && Math.abs(dx) < Math.abs(dy) * 1.5f) {
                        isValidDirection = true;
                        cost = Math.abs(dy) + 2.5f * Math.abs(dx);
                    }
                    break;
                case DIR_DOWN:
                    if (ty > cy && Math.abs(dx) < Math.abs(dy) * 1.5f) {
                        isValidDirection = true;
                        cost = Math.abs(dy) + 2.5f * Math.abs(dx);
                    }
                    break;
                case DIR_LEFT:
                    if (tx < cx && Math.abs(dy) < Math.abs(dx) * 1.5f) {
                        isValidDirection = true;
                        cost = Math.abs(dx) + 2.5f * Math.abs(dy);
                    }
                    break;
                case DIR_RIGHT:
                    if (tx > cx && Math.abs(dy) < Math.abs(dx) * 1.5f) {
                        isValidDirection = true;
                        cost = Math.abs(dx) + 2.5f * Math.abs(dy);
                    }
                    break;
            }

            if (isValidDirection && cost < minCost) {
                minCost = cost;
                bestCandidate = candidate;
            }
        }

        // If strict direction constraints fail, try a looser nearest Euclidean check in that direction hemisphere
        if (bestCandidate == null) {
            for (int i = 0; i < focusables.size(); i++) {
                VirtualNode candidate = focusables.get(i);
                float tx = (candidate.left + candidate.right) / 2f;
                float ty = (candidate.top + candidate.bottom) / 2f;
                float dx = tx - cx;
                float dy = ty - cy;

                boolean inHemisphere = false;
                switch (direction) {
                    case DIR_UP: inHemisphere = (ty < cy); break;
                    case DIR_DOWN: inHemisphere = (ty > cy); break;
                    case DIR_LEFT: inHemisphere = (tx < cx); break;
                    case DIR_RIGHT: inHemisphere = (tx > cx); break;
                }

                if (inHemisphere) {
                    float dist = dx * dx + dy * dy;
                    if (dist < minCost) {
                        minCost = dist;
                        bestCandidate = candidate;
                    }
                }
            }
        }

        return bestCandidate;
    }

    public static VirtualNode findHitNode(VirtualNode root, float x, float y) {
        if (root == null || root.alpha <= 0.01f) return null;

        // Traverse children in reverse draw order (top-most elements first)
        for (int i = root.children.size() - 1; i >= 0; i--) {
            VirtualNode hit = findHitNode(root.children.get(i), x, y);
            if (hit != null) return hit;
        }

        // For ListView / GridView recycling nodes
        if (root instanceof com.rock.featherui.lib.node.ListViewNode) {
            List<VirtualNode> active = ((com.rock.featherui.lib.node.ListViewNode) root).getActiveNodes();
            for (int i = active.size() - 1; i >= 0; i--) {
                VirtualNode hit = findHitNode(active.get(i), x, y);
                if (hit != null) return hit;
            }
        } else if (root instanceof com.rock.featherui.lib.node.GridViewNode) {
            List<VirtualNode> active = ((com.rock.featherui.lib.node.GridViewNode) root).getActiveNodes();
            for (int i = active.size() - 1; i >= 0; i--) {
                VirtualNode hit = findHitNode(active.get(i), x, y);
                if (hit != null) return hit;
            }
        }

        if (root.focusable && x >= root.left && x <= root.right && y >= root.top && y <= root.bottom) {
            return root;
        }

        return null;
    }

    public static VirtualNode findFirstFocusable(VirtualNode root) {
        if (root == null || root.alpha <= 0.01f) return null;
        if (root.focusable) return root;

        for (int i = 0; i < root.children.size(); i++) {
            VirtualNode found = findFirstFocusable(root.children.get(i));
            if (found != null) return found;
        }
        return null;
    }

    private static void collectFocusableNodes(VirtualNode node, List<VirtualNode> list) {
        if (node == null || node.alpha <= 0.01f) return;
        if (node.focusable) {
            list.add(node);
        }

        // Add standard children
        for (int i = 0; i < node.children.size(); i++) {
            collectFocusableNodes(node.children.get(i), list);
        }

        // Collect recycled nodes if it is a list/grid
        if (node instanceof com.rock.featherui.lib.node.ListViewNode) {
            List<VirtualNode> active = ((com.rock.featherui.lib.node.ListViewNode) node).getActiveNodes();
            for (int i = 0; i < active.size(); i++) {
                collectFocusableNodes(active.get(i), list);
            }
        } else if (node instanceof com.rock.featherui.lib.node.GridViewNode) {
            List<VirtualNode> active = ((com.rock.featherui.lib.node.GridViewNode) node).getActiveNodes();
            for (int i = 0; i < active.size(); i++) {
                collectFocusableNodes(active.get(i), list);
            }
        }
    }

    private static VirtualNode findNodeById(VirtualNode root, String id) {
        if (root == null) return null;
        if (id.equals(root.id)) return root;

        for (int i = 0; i < root.children.size(); i++) {
            VirtualNode found = findNodeById(root.children.get(i), id);
            if (found != null) return found;
        }

        if (root instanceof com.rock.featherui.lib.node.ListViewNode) {
            List<VirtualNode> active = ((com.rock.featherui.lib.node.ListViewNode) root).getActiveNodes();
            for (int i = 0; i < active.size(); i++) {
                VirtualNode found = findNodeById(active.get(i), id);
                if (found != null) return found;
            }
        } else if (root instanceof com.rock.featherui.lib.node.GridViewNode) {
            List<VirtualNode> active = ((com.rock.featherui.lib.node.GridViewNode) root).getActiveNodes();
            for (int i = 0; i < active.size(); i++) {
                VirtualNode found = findNodeById(active.get(i), id);
                if (found != null) return found;
            }
        }
        return null;
    }
}
