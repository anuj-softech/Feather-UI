# Element Reference Guide

FeatherUI layouts are built using a hierarchy of virtual nodes. Each node translates directly to a specific drawing and layout instruction on the render pipeline. Below is the documentation for all available element types.

---

## 1. Base Element: VirtualNode
The root parent class of all UI elements. It manages position, layout constraints, transform animations, visibility, backgrounds, borders, and shadows.

*   **Render Behavior:** Every `VirtualNode` computes its absolute screen-space coordinates using its relative `left`, `top`, `width`, and `height` properties. It automatically applies transforms (scaling, rotation, translation) using the canvas matrix, and handles alpha transparency.
*   **Occlusion Culling:** During each draw pass, if a `VirtualNode`'s bounds (after scaling) are entirely outside the device screen viewport, it is culled automatically, skipping all render passes for itself and its children.

---

## 2. Layout Elements

### LinearLayoutNode (`linear_layout`)
Lays out its children sequentially in a single direction (horizontal or vertical).

*   **Render Behavior:** Computes the size of all children and positions them one after another. If the child's width/height is `"match_parent"`, it allocates the remaining space in that direction.
*   **Gap Spacing:** Automatically inserts spacing (gaps) between adjacent children without requiring empty spacer nodes.

### FrameLayoutNode (`frame_layout`)
Stacks children on top of each other in the z-direction.

*   **Render Behavior:** Children are positioned relative to the top-left of the container by default. If a child defines a `gravity` property, it is aligned accordingly (e.g., centered, aligned to the bottom-right).

### ListViewNode (`list_view`)
A high-performance scrolling layout designed to render lists of hundreds of items.

*   **Render Behavior:** Instead of drawing all children at once, it monitors its scrolling offset and coordinates layout/drawing only for children currently intersecting the visible window.
*   **Recycling mechanism:** Used for grids and menus to maintain extremely low memory usage.

### GridViewNode (`grid_view`)
Lays out children in a multi-column, scrollable grid format.

*   **Render Behavior:** Automatically calculates column widths based on its own dimensions, the column count, and the gap property. Supports vertical D-pad navigation between rows and columns.

---

## 3. Visual & Text Widgets

### TextViewNode (`text_view`)
Renders text strings.

*   **Render Behavior:** Measures text bounds using the system text paint to report accurate width/height measurements during layout passes.
*   **Gravity:** Automatically aligns text inside its own bounding box (left, right, center, vertical alignment).

### ImageViewNode (`image_view`)
Loads, decodes, and draws images.

*   **Render Behavior:** Decodes image paths or remote URLs asynchronously on a background executor. When decoding completes, it triggers a layout request.
*   **Bilinear Filtering:** Always uses bilinear filtering (`paint.setFilterBitmap(true)`) and dithering (`paint.setDither(true)`) to output high-fidelity scaled images without color banding.

### ButtonNode (`button`)
An interactive node that accepts focus and responds to click/D-pad select inputs.

*   **Render Behavior:** Automatically manages transition states. When focused, it applies zoom transforms (`scale`), changes borders, updates text colors, and displays optional glow shadows.
*   **State Machine:** Interacts with the `SpatialFocusEngine` to smoothly animate focus changes using ease-out transitions.
