package com.rock.featherui.lib.registry;

import com.rock.featherui.lib.node.ButtonNode;
import com.rock.featherui.lib.node.FrameLayoutNode;
import com.rock.featherui.lib.node.GridViewNode;
import com.rock.featherui.lib.node.ImageViewNode;
import com.rock.featherui.lib.node.LinearLayoutNode;
import com.rock.featherui.lib.node.ListViewNode;
import com.rock.featherui.lib.node.TextViewNode;
import com.rock.featherui.lib.node.VirtualNode;
import java.util.HashMap;
import java.util.Map;

public class WidgetFactory {
    public interface NodeCreator {
        VirtualNode create();
    }

    private static final Map<String, NodeCreator> factoryRegistry = new HashMap<>();

    static {
        register("frame_layout", FrameLayoutNode::new);
        register("linear_layout", LinearLayoutNode::new);
        register("text_view", TextViewNode::new);
        register("image_view", ImageViewNode::new);
        register("button", ButtonNode::new);
        register("list_view", ListViewNode::new);
        register("grid_view", GridViewNode::new);
    }

    public static void register(String typeName, NodeCreator creator) {
        factoryRegistry.put(typeName, creator);
    }

    public static VirtualNode createNode(String typeName) {
        NodeCreator creator = factoryRegistry.get(typeName);
        if (creator != null) {
            VirtualNode node = creator.create();
            node.type = typeName;
            return node;
        }
        return null;
    }

    public static boolean isRegistered(String typeName) {
        return factoryRegistry.containsKey(typeName);
    }
}
