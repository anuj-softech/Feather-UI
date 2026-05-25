# Getting Started with FeatherUI

FeatherUI is a high-performance, ultra-lightweight UI framework for Android. Instead of using traditional heavy Android View trees or Jetpack Compose hierarchies, FeatherUI inflates a lightweight tree of virtual nodes (`VirtualNode`) and renders them onto a single 32-bit hardware-accelerated `SurfaceView` using a dedicated asynchronous rendering pipeline. This framework is highly optimized for memory-constrained environments such as Android TV, set-top boxes, and legacy devices.

---

## Installation & Setup

FeatherUI is organized as a library module (`:featherui`) that can be integrated directly into your Android application project.

### 1. Declare FeatherUIView in XML Layout
Place the `FeatherUIView` in your activity or fragment XML layout where you want the virtual UI to render:

```xml
<?xml version="1.0" encoding="utf-8"?>
<FrameLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="#000000">

    <com.rock.featherui.lib.view.FeatherUIView
        android:id="@+id/feather_ui_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:focusable="true"
        android:focusableInTouchMode="true" />

</FrameLayout>
```

### 2. Prepare 32-bit Screen Depth
To prevent color banding and low-bitrate artifacts (especially during gradients and blurs), force 32-bit color precision on the Activity Window prior to setting the content view:

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Request 32-bit RGBA_8888 color depth before setContentView
    getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
    setContentView(R.layout.activity_main);
    
    FeatherUIView featherUIView = findViewById(R.id.feather_ui_view);
    ...
}
```

---

## Layout Inflation

FeatherUI templates are structured in JSON format. Place your template files (e.g., `tv_show_layout.json` and `item_show_card.json`) in your project's `assets/` directory.

### 1. Simple JSON Layout Template (`assets/tv_show_layout.json`)
```json
{
  "type": "frame_layout",
  "width": "match_parent",
  "height": "match_parent",
  "children": [
    {
      "id": "backdrop",
      "type": "image_view",
      "width": "match_parent",
      "height": "match_parent",
      "scale_type": "center_crop",
      "alpha": 0.2
    },
    {
      "id": "main_content",
      "type": "linear_layout",
      "orientation": "vertical",
      "width": 600,
      "height": "match_parent",
      "padding": 40,
      "children": [
        {
          "id": "show_title",
          "type": "text_view",
          "width": "match_parent",
          "height": 80,
          "text": "Loading Title...",
          "text_size": 36,
          "text_color": "#FFFFFF"
        }
      ]
    }
  ]
}
```

### 2. Inflating the Virtual Node Tree in Java
Use `JSONLayoutInflater` to parse the layout and set it on the `FeatherUIView`:

```java
// Load JSON string from assets
String layoutJson = loadJSONFromAsset("tv_show_layout.json");

// Inflate virtual layout
UILayout uiLayout = JSONLayoutInflater.inflate(this, layoutJson, featherUIView);

// Attach layout to the host view
featherUIView.setLayout(uiLayout);
```

---

## Interacting with the UI

### 1. Dynamic Property Updates
To update elements dynamically without re-inflating the layout, use `uiLayout.updateProperty(...)`:

```java
// Updates the text of a TextViewNode
uiLayout.updateProperty("show_title", "text", "PROJECT HAIL MARY");

// Dynamically sets the source of an ImageViewNode
uiLayout.updateProperty("backdrop", "src", "https://example.com/backdrop.jpg");
```

### 2. Handling Focus and Remote Input (D-pad)
FeatherUI features a built-in spatial focus engine. It resolves focus movement based on D-pad input (Up, Down, Left, Right). 
To register a focus change listener or a click listener on interactive buttons:

```java
VirtualNode playButton = uiLayout.findNodeById("play_button");
if (playButton instanceof ButtonNode) {
    ButtonNode btn = (ButtonNode) playButton;
    
    // Set click listener
    btn.setOnClickListener(node -> {
        Log.d("FeatherUI", "Play Button Clicked!");
    });
}
```

---

## Essential Performance Guidelines

To maintain FeatherUI's lightweight, jank-free nature, follow these rules:

1. **Keep Custom Nodes Allocation-Free:** If subclassing or overriding draw steps, never instantiate objects (like `new Paint()`, `new Rect()`, or arrays) inside `onDraw()`. Instead, use the `ObjectPool` to acquire temporary objects:
   ```java
   Paint paint = ObjectPool.acquirePaint();
   // Do draw operations
   ObjectPool.release(paint);
   ```
2. **Execute Layout Updates on the Main Looper:** Although FeatherUI uses a background thread for drawing, modifying node hierarchies or adding children must be dispatched via `MainLooper` to prevent race conditions with the draw loop.
