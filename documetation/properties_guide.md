# Properties Reference & Troubleshooting Guide

This guide details the attributes supported by FeatherUI elements, their syntax, and solutions to common integration pitfalls.

---

## 1. Core Layout & Dimension Properties

### `width` & `height`
Specifies the dimensions of the virtual node.
*   **Formats:** Integer pixel value (e.g., `250`), or `"match_parent"`.
*   **Common Pitfalls:**
    *   *Problem: Parent-Child Loop / Infinite Sizing.* Setting `width` or `height` to `"match_parent"` inside a parent container that is also sized dynamically (`"wrap_content"` or unresolved dimensions) can result in the node resolving to a size of `0`.
    *   *Solution:* Always ensure that at least one ancestor node in the tree has an explicit pixel dimension.

### `margin` & `padding`
Defines empty spacing around (margin) or inside (padding) a node.
*   **Sub-properties:** `margin_left`, `margin_top`, `margin_right`, `margin_bottom`, `padding_left`, `padding_top`, `padding_right`, `padding_bottom`.
*   **Common Pitfalls:**
    *   *Problem: Misalignment of Scaled Items.* Margins and paddings are calculated *before* transforms (like `scaleX` or `scaleY`) are applied. Applying a `1.2x` scale on focus can cause adjacent nodes to overlap.
    *   *Solution:* Account for focus scaling zoom room by adding sufficient margins between grid/list items.

---

## 2. Visual Effects & Styles

### `background_color` & `border_color`
Sets background fill and border stroke colors.
*   **Formats:** HEX color strings (e.g., `#A0101115`, `#FFFFFF`).
*   **Common Pitfalls:**
    *   *Problem: Low-bitrate Pastel Color Banding.* In default Android views, drawing translucent colors over dark regions results in blocky, low-bitrate artifacts.
    *   *Solution:* Force the host window and SurfaceView to use 32-bit `RGBA_8888` pixel formats.

### `backdrop_blur`
Applies a frosted-glass blur filter to the background sub-region behind the node.
*   **Formats:** Float value (e.g., `24`).
*   **Common Pitfalls:**
    *   *Problem: Double-image Leak / Sharp Details Showing Through.* Because the sharp background image is drawn directly on the screen, drawing a translucent blurred bitmap on top results in both details overlapping.
    *   *Solution:* FeatherUI automatically draws a solid black base layer beneath the blurred bitmap to completely mask out sharp background leaks.
    *   *Problem: Blur is Missing/Blank.* The backdrop filter requires a target image view node to crop pixels from. If a node has `backdrop_blur` but no backdrop ID is resolved, the blur will be skipped.
    *   *Solution:* Define a background image with `id: "backdrop"` inside the layout file.

### `glow_color` & `glow_radius`
Renders an external drop-shadow glow around the node bounds (often used for focused buttons).
*   **Common Pitfalls:**
    *   *Problem: Low Frame Rates during Scrolling.* Applying a complex shadow layer via `paint.setShadowLayer(...)` is highly resource-intensive on the CPU.
    *   *Solution:* Only apply `glow_radius` on active, focused elements (e.g., inside button focus states). Avoid applying continuous glows to static background nodes.

---

## 3. Transforms & Compositing

### `alpha`
Specifies the transparency level of the node.
*   **Formats:** Float from `0.0` (fully transparent) to `1.0` (fully opaque).
*   **Common Pitfalls:**
    *   *Problem: Heavy saveLayerAlpha Overhead.* Changing alpha values below `0.99` triggers `canvas.saveLayerAlpha`, creating a temporary offscreen buffer. Having too many layered translucent nodes ruins render speeds.
    *   *Solution:* For simple leaf nodes (like images/text), set the color or tint transparency directly on the node's paint instead of modifying the global node `alpha`.

---

## 4. D-pad & Focus Navigation

### `next_focus_up` / `down` / `left` / `right`
Overrides the automatic spatial navigation engine path.
*   **Formats:** ID string of the target node (e.g., `"btn_play"`).
*   **Common Pitfalls:**
    *   *Problem: D-pad Focus Jumps or Lock.* If the targeted ID is missing, misspelled, or marked `focusable: false`, the D-pad input will be ignored, locking focus in place.
    *   *Solution:* Double-check that all target navigation IDs exist in the JSON layout and have `focusable: true`.

---

## 5. Widget-Specific Properties

### `scroll_alignment` & `scroll_center_offset` (List & Grid)
Controls scrolling viewport offsets during focus changes.
*   **Formats:** `"center"`, `"start"`, `"end"`.
*   **Common Pitfalls:**
    *   *Problem: Focus jumps out of view.* In horizontal lists, setting incorrect alignment values can cause the list to scroll too late, hiding the active card.
    *   *Solution:* Use `"center"` scroll alignment to keep the active card centered in the scrolling viewport.

### `src` & `image_url` (ImageView)
Specifies local asset path or remote URL for decoding.
*   **Common Pitfalls:**
    *   *Problem: Memory Bloat / Out of Memory (OOM).* Loading raw high-resolution images (e.g., 4K backdrops) causes the JVM to run out of memory.
    *   *Solution:* Always use scaled-down, web-optimized image assets (e.g., 1080p maximum) since screen coordinates scale down anyway.
