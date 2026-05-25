# FeatherUI Documentation Center

Welcome to the FeatherUI documentation center. This directory contains detailed guides, technical specifications, and API/layout reference materials to help you build and maintain applications using the FeatherUI virtual layout rendering engine.

## Documentation Index

### 1. [Getting Started Guide](getting_started.md)
*Learn how to integrate FeatherUI into your Android applications.*
- Host view configuration (`FeatherUIView` in XML).
- Requesting 32-bit window format for high-precision rendering and blurs.
- Layout inflation from JSON templates using `JSONLayoutInflater`.
- Binding dynamic properties and listening to user inputs (click, focus, D-pad events).
- Core performance rules (e.g. allocation-free rendering).

### 2. [Element Reference Guide](element_reference.md)
*Understand the layout and widget building blocks of FeatherUI.*
- The base `VirtualNode` structure, transforms, and occlusion culling.
- Layout nodes: `LinearLayoutNode`, `FrameLayoutNode`.
- Scrolling containers: High-performance recycling `ListViewNode` and `GridViewNode`.
- Widgets: `TextViewNode`, `ImageViewNode`, and interactive `ButtonNode`.

### 3. [Properties & Troubleshooting Guide](properties_guide.md)
*Reference documentation for configuring layout templates.*
- Properties overview: sizes, dimensions, margins, padding, backgrounds, borders, inner shadows, and focus styles.
- Custom button focus behaviors (glow, zoom, text/tint transitions).
- Troubleshooting common bugs: resolving infinite size calculation loops, eliminating double blurs or blank blurs, avoiding OOM decoding large backdrop images, and optimizing shadows.
