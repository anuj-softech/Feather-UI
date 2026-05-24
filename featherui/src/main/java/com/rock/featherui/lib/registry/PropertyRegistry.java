package com.rock.featherui.lib.registry;

import android.graphics.Color;
import com.rock.featherui.lib.node.ButtonNode;
import com.rock.featherui.lib.node.GridViewNode;
import com.rock.featherui.lib.node.ImageViewNode;
import com.rock.featherui.lib.node.LinearLayoutNode;
import com.rock.featherui.lib.node.ListViewNode;
import com.rock.featherui.lib.node.TextViewNode;
import com.rock.featherui.lib.node.VirtualNode;
import java.util.HashMap;
import java.util.Map;

public class PropertyRegistry {
    public static float density = 1.0f; // To be initialized by view context

    public interface PropertyUpdater {
        void update(VirtualNode node, Object value);
    }

    private static final Map<String, PropertyUpdater> registry = new HashMap<>();

    static {
        // Layout dimensions
        registerProperty("width", (node, val) -> node.width = toPx(val));
        registerProperty("height", (node, val) -> node.height = toPx(val));

        // Margins
        registerProperty("margin", (node, val) -> {
            int px = toPx(val);
            node.marginLeft = px;
            node.marginTop = px;
            node.marginRight = px;
            node.marginBottom = px;
        });
        registerProperty("margin_left", (node, val) -> node.marginLeft = toPx(val));
        registerProperty("margin_top", (node, val) -> node.marginTop = toPx(val));
        registerProperty("margin_right", (node, val) -> node.marginRight = toPx(val));
        registerProperty("margin_bottom", (node, val) -> node.marginBottom = toPx(val));

        // Padding
        registerProperty("padding", (node, val) -> {
            int px = toPx(val);
            node.paddingLeft = px;
            node.paddingTop = px;
            node.paddingRight = px;
            node.paddingBottom = px;
        });
        registerProperty("padding_left", (node, val) -> node.paddingLeft = toPx(val));
        registerProperty("padding_top", (node, val) -> node.paddingTop = toPx(val));
        registerProperty("padding_right", (node, val) -> node.paddingRight = toPx(val));
        registerProperty("padding_bottom", (node, val) -> node.paddingBottom = toPx(val));

        // Visual properties
        registerProperty("background_color", (node, val) -> node.backgroundColor = parseColor(val));
        registerProperty("border_color", (node, val) -> node.borderColor = parseColor(val));
        registerProperty("border_width", (node, val) -> node.borderWidth = toFloatPx(val));
        registerProperty("border_radius", (node, val) -> node.borderRadius = toFloatPx(val));
        registerProperty("glow_color", (node, val) -> node.glowColor = parseColor(val));
        registerProperty("glow_radius", (node, val) -> node.glowRadius = toFloatPx(val));
        registerProperty("backdrop_blur", (node, val) -> node.backdropBlur = toFloatPx(val));

        // Inner Shadow / Vignette Properties
        registerProperty("inner_shadow_color", (node, val) -> node.innerShadowColor = parseColor(val));
        registerProperty("inner_shadow_radius", (node, val) -> node.innerShadowRadius = toFloatPx(val));
        registerProperty("inner_shadow_origin", (node, val) -> {
            if (val instanceof String) {
                String[] parts = ((String) val).split(",");
                if (parts.length == 2) {
                    node.innerShadowXPercent = toFloat(parts[0].trim(), 50f) / 100f;
                    node.innerShadowYPercent = toFloat(parts[1].trim(), 50f) / 100f;
                }
            }
        });

        // Focus navigation
        registerProperty("focusable", (node, val) -> node.focusable = toBoolean(val));
        registerProperty("next_focus_up", (node, val) -> node.nextFocusUpId = toString(val));
        registerProperty("next_focus_down", (node, val) -> node.nextFocusDownId = toString(val));
        registerProperty("next_focus_left", (node, val) -> node.nextFocusLeftId = toString(val));
        registerProperty("next_focus_right", (node, val) -> node.nextFocusRightId = toString(val));

        // Transforms
        registerProperty("scale", (node, val) -> {
            float s = toFloat(val, 1.0f);
            node.scaleX = s;
            node.scaleY = s;
        });
        registerProperty("scale_x", (node, val) -> node.scaleX = toFloat(val, 1.0f));
        registerProperty("scale_y", (node, val) -> node.scaleY = toFloat(val, 1.0f));
        registerProperty("translation_x", (node, val) -> node.translationX = toFloatPx(val));
        registerProperty("translation_y", (node, val) -> node.translationY = toFloatPx(val));
        registerProperty("rotation", (node, val) -> node.rotation = toFloat(val, 0f));
        registerProperty("alpha", (node, val) -> node.alpha = toFloat(val, 1.0f));
        registerProperty("visibility", (node, val) -> node.visibility = toString(val));

        // Gravity/alignment
        registerProperty("gravity", (node, val) -> node.gravity = parseGravity(val));

        // Widget-specific: LinearLayout, ListView
        registerProperty("orientation", (node, val) -> {
            if (node instanceof LinearLayoutNode) {
                ((LinearLayoutNode) node).orientation = toString(val);
            } else if (node instanceof ListViewNode) {
                ((ListViewNode) node).orientation = toString(val);
            }
        });
        registerProperty("gap", (node, val) -> {
            if (node instanceof LinearLayoutNode) {
                ((LinearLayoutNode) node).gap = toPx(val);
            } else if (node instanceof ListViewNode) {
                ((ListViewNode) node).gap = toPx(val);
            } else if (node instanceof GridViewNode) {
                ((GridViewNode) node).gap = toPx(val);
            }
        });
        registerProperty("scroll_alignment", (node, val) -> {
            if (node instanceof ListViewNode) {
                ((ListViewNode) node).scrollAlignment = toString(val);
            } else if (node instanceof GridViewNode) {
                ((GridViewNode) node).scrollAlignment = toString(val);
            }
        });
        registerProperty("scroll_center_offset", (node, val) -> {
            if (node instanceof ListViewNode) {
                ((ListViewNode) node).scrollCenterOffset = toString(val);
            } else if (node instanceof GridViewNode) {
                ((GridViewNode) node).scrollCenterOffset = toString(val);
            }
        });

        // Widget-specific: TextView
        registerProperty("text", (node, val) -> {
            if (node instanceof TextViewNode) {
                ((TextViewNode) node).text = toString(val);
            }
        });
        registerProperty("text_size", (node, val) -> {
            if (node instanceof TextViewNode) {
                ((TextViewNode) node).textSize = toFloatPx(val);
            }
        });
        registerProperty("text_color", (node, val) -> {
            if (node instanceof TextViewNode) {
                ((TextViewNode) node).textColor = parseColor(val);
            }
        });
        registerProperty("font_family", (node, val) -> {
            if (node instanceof TextViewNode) {
                ((TextViewNode) node).fontFamily = toString(val);
            }
        });
        registerProperty("font_style", (node, val) -> {
            if (node instanceof TextViewNode) {
                ((TextViewNode) node).fontStyle = toString(val);
            }
        });

        // Widget-specific: ImageView
        registerProperty("src", (node, val) -> {
            if (node instanceof ImageViewNode) {
                ((ImageViewNode) node).setSrc(toString(val));
            }
        });
        registerProperty("image_url", (node, val) -> {
            if (node instanceof ImageViewNode) {
                ((ImageViewNode) node).setSrc(toString(val));
            }
        });
        registerProperty("scale_type", (node, val) -> {
            if (node instanceof ImageViewNode) {
                ((ImageViewNode) node).scaleType = toString(val);
            }
        });
        registerProperty("blur_radius", (node, val) -> {
            if (node instanceof ImageViewNode) {
                ((ImageViewNode) node).blurRadius = toFloat(val, 0f);
            }
        });

        // Widget-specific: GridView
        registerProperty("columns", (node, val) -> {
            if (node instanceof GridViewNode) {
                ((GridViewNode) node).columns = toInt(val, 1);
            }
        });

        // Widget-specific: Button States Customization
        registerProperty("focused_scale", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedScale = toFloat(val, 1.1f);
            }
        });
        registerProperty("focused_background_color", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedBgColor = parseColor(val);
            }
        });
        registerProperty("focused_border_color", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedBorderColor = parseColor(val);
            }
        });
        registerProperty("focused_border_width", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedBorderWidth = toFloatPx(val);
            }
        });
        registerProperty("focused_glow_color", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedGlowColor = parseColor(val);
            }
        });
        registerProperty("focused_glow_radius", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedGlowRadius = toFloatPx(val);
            }
        });
        registerProperty("focused_text_color", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedTextColor = parseColor(val);
            }
        });
        registerProperty("focused_tint_color", (node, val) -> {
            if (node instanceof ButtonNode) {
                ((ButtonNode) node).focusedTintColor = parseColor(val);
            }
        });
        registerProperty("tint_color", (node, val) -> {
            if (node instanceof ImageViewNode) {
                ((ImageViewNode) node).tintColor = parseColor(val);
            }
        });
    }

    public static void registerProperty(String key, PropertyUpdater updater) {
        registry.put(key, updater);
    }

    public static void apply(VirtualNode node, String key, Object value) {
        PropertyUpdater updater = registry.get(key);
        if (updater != null) {
            updater.update(node, value);
        }
    }

    public static int toPx(Object value) {
        if (value instanceof Number) {
            return Math.round(((Number) value).floatValue() * density);
        } else if (value instanceof String) {
            String s = (String) value;
            if (s.equalsIgnoreCase("match_parent")) return VirtualNode.MATCH_PARENT;
            if (s.equalsIgnoreCase("wrap_content")) return VirtualNode.WRAP_CONTENT;
            try {
                return Math.round(Float.parseFloat(s) * density);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return 0;
    }

    public static float toFloatPx(Object value) {
        if (value instanceof Number) {
            return ((Number) value).floatValue() * density;
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value) * density;
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return 0f;
    }

    public static float toFloat(Object value, float defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (value instanceof String) {
            try {
                return Float.parseFloat((String) value);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return defaultValue;
    }

    public static int toInt(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                // fall through
            }
        }
        return defaultValue;
    }

    public static int parseColor(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            String s = (String) value;
            if (s.startsWith("#")) {
                try {
                    return Color.parseColor(s);
                } catch (IllegalArgumentException e) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return false;
    }

    public static String toString(Object value) {
        return value == null ? null : value.toString();
    }

    private static int parseGravity(Object value) {
        if (value == null) return 0;
        String gravityStr = value.toString().toLowerCase();
        int gravityVal = 0;
        if (gravityStr.contains("center")) {
            gravityVal |= 17; // Gravity.CENTER
        } else {
            if (gravityStr.contains("left")) gravityVal |= 3; // Gravity.LEFT
            if (gravityStr.contains("right")) gravityVal |= 5; // Gravity.RIGHT
            if (gravityStr.contains("top")) gravityVal |= 48; // Gravity.TOP
            if (gravityStr.contains("bottom")) gravityVal |= 80; // Gravity.BOTTOM
        }
        return gravityVal;
    }
}
