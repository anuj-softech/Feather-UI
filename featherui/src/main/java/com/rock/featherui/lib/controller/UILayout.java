package com.rock.featherui.lib.controller;

import com.rock.featherui.lib.node.VirtualNode;
import com.rock.featherui.lib.view.FeatherUIView;
import java.util.HashMap;
import java.util.Map;

public class UILayout {
    private final Map<String, VirtualNode> nodeRegistry = new HashMap<>();
    private FeatherUIView hostView;
    private VirtualNode rootNode;

    public UILayout(FeatherUIView hostView) {
        this.hostView = hostView;
    }

    public void registerNode(String id, VirtualNode node) {
        if (id != null && !id.isEmpty()) {
            nodeRegistry.put(id, node);
        }
    }

    public VirtualNode findViewById(String id) {
        return nodeRegistry.get(id);
    }

    public void updateProperty(String id, String property, Object value) {
        VirtualNode node = findViewById(id);
        if (node != null) {
            node.applyProperty(property, value);
            if (hostView != null) {
                hostView.requestLayoutOrPaint(node);
            }
        }
    }

    public void setRootNode(VirtualNode rootNode) {
        this.rootNode = rootNode;
    }

    public VirtualNode getRootNode() {
        return rootNode;
    }

    public void setHostView(FeatherUIView hostView) {
        this.hostView = hostView;
    }

    public Map<String, VirtualNode> getNodeRegistry() {
        return nodeRegistry;
    }
}
