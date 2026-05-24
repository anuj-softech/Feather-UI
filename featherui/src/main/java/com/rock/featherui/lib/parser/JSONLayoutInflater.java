package com.rock.featherui.lib.parser;

import android.content.Context;
import com.rock.featherui.lib.controller.UILayout;
import com.rock.featherui.lib.node.ImageViewNode;
import com.rock.featherui.lib.node.VirtualNode;
import com.rock.featherui.lib.registry.PropertyRegistry;
import com.rock.featherui.lib.registry.WidgetFactory;
import com.rock.featherui.lib.view.FeatherUIView;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;

public class JSONLayoutInflater {

    public static UILayout inflate(Context context, String jsonString, FeatherUIView host) {
        // Initialize globals
        PropertyRegistry.density = context.getResources().getDisplayMetrics().density;
        ImageViewNode.appContext = context.getApplicationContext();

        UILayout layout = new UILayout(host);
        try {
            JSONObject json = new JSONObject(jsonString);
            VirtualNode root = parseNode(json, layout);
            layout.setRootNode(root);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return layout;
    }

    public static VirtualNode inflate(String jsonString) {
        try {
            JSONObject json = new JSONObject(jsonString);
            return parseNode(json, null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static VirtualNode parseNode(JSONObject json, UILayout layout) throws Exception {
        String type = json.optString("type");
        if (type == null || type.isEmpty()) {
            return null;
        }

        VirtualNode node = WidgetFactory.createNode(type);
        if (node == null) {
            return null;
        }

        String id = json.optString("id", null);
        if (id != null) {
            node.id = id;
            if (layout != null) {
                layout.registerNode(id, node);
            }
        }

        // Apply all properties
        Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            if ("type".equals(key) || "id".equals(key) || "children".equals(key)) {
                continue;
            }
            Object val = json.get(key);
            node.applyProperty(key, val);
        }

        // Parse children
        JSONArray childrenArray = json.optJSONArray("children");
        if (childrenArray != null) {
            for (int i = 0; i < childrenArray.length(); i++) {
                JSONObject childJson = childrenArray.optJSONObject(i);
                if (childJson != null) {
                    VirtualNode child = parseNode(childJson, layout);
                    if (child != null) {
                        node.addChild(child);
                    }
                }
            }
        }

        return node;
    }
}
