package com.rock.featherui;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rock.featherui.lib.controller.UILayout;
import com.rock.featherui.lib.node.ListViewNode;
import com.rock.featherui.lib.node.VirtualNode;
import com.rock.featherui.lib.parser.JSONLayoutInflater;
import com.rock.featherui.lib.view.FeatherUIView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static class Movie {
        String id;
        String title;
        String poster;

        Movie(String id, String title, String poster) {
            this.id = id;
            this.title = title;
            this.poster = poster;
        }
    }

    private FeatherUIView featherUIView;
    private UILayout uiLayout;
    private final List<Movie> moviesList = new ArrayList<>();
    private boolean isInMyList = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);
        setContentView(R.layout.activity_main);

        featherUIView = findViewById(R.id.feather_ui_view);

        // Load layouts and data from assets
        String mainLayoutJson = loadJSONFromAsset("tv_show_layout.json");
        String cardTemplateJson = loadJSONFromAsset("item_show_card.json");
        loadMoviesList();

        if (mainLayoutJson != null && cardTemplateJson != null) {
            // Inflate virtual node tree
            uiLayout = JSONLayoutInflater.inflate(this, mainLayoutJson, featherUIView);
            featherUIView.setLayout(uiLayout);

            // Set up initial backdrop and details using the first movie
            if (!moviesList.isEmpty()) {
                Movie firstMovie = moviesList.get(0);
                uiLayout.updateProperty("show_title", "text", firstMovie.title.toUpperCase());
                uiLayout.updateProperty("backdrop", "src", firstMovie.poster);
                uiLayout.updateProperty("show_description", "text", "Experience the epic journey of " + firstMovie.title + " streaming now in stunning Ultra HD and spatial audio.");
            }

            // Populate horizontal recommendation list
            setupRecommendationList(cardTemplateJson);

            // Connect button click handlers
            setupButtonListeners();
        } else {
            Toast.makeText(this, "Failed to load layout from assets", Toast.LENGTH_LONG).show();
        }
    }

    private void loadMoviesList() {
        moviesList.clear();
        try {
            String moviesJsonStr = loadJSONFromAsset("movies.json");
            if (moviesJsonStr != null) {
                JSONObject obj = new JSONObject(moviesJsonStr);
                JSONArray arr = obj.getJSONArray("movies");
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject m = arr.getJSONObject(i);
                    String id = m.optString("id");
                    String title = m.optString("title");
                    if (title.isEmpty()) {
                        title = "Movie " + id;
                    }
                    String poster = m.optString("poster");
                    moviesList.add(new Movie(id, title, poster));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupRecommendationList(String cardTemplateJson) {
        ListViewNode listView = (ListViewNode) uiLayout.findViewById("recommendations_list");
        if (listView != null) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (Movie movie : moviesList) {
                Map<String, Object> show = new HashMap<>();
                show.put("card_title:text", movie.title);
                show.put("card_image:src", movie.poster);
                items.add(show);
            }
            listView.setItemsData(items, cardTemplateJson);
        }
    }

    private void setupButtonListeners() {
        // Play button click handler
        VirtualNode playBtn = uiLayout.findViewById("btn_play");
        if (playBtn != null) {
            playBtn.onClickListener = node -> {
                Toast.makeText(MainActivity.this, "Starting Playback...", Toast.LENGTH_SHORT).show();
            };
        }

        // My List button click handler (toggles state)
        VirtualNode myListBtn = uiLayout.findViewById("btn_mylist");
        if (myListBtn != null) {
            myListBtn.onClickListener = node -> {
                isInMyList = !isInMyList;
                String text = isInMyList ? "✓ In My List" : "+ My List";
                uiLayout.updateProperty("txt_mylist", "text", text);
                Toast.makeText(MainActivity.this, isInMyList ? "Added to list" : "Removed from list", Toast.LENGTH_SHORT).show();
            };
        }

        // Trailer button click handler
        VirtualNode episodesBtn = uiLayout.findViewById("btn_episodes");
        if (episodesBtn != null) {
            episodesBtn.onClickListener = node -> {
                Toast.makeText(MainActivity.this, "Trailer loading...", Toast.LENGTH_SHORT).show();
            };
        }

        // Horizontal List Card click and focus listeners: load the selected movie details dynamically!
        ListViewNode listView = (ListViewNode) uiLayout.findViewById("recommendations_list");
        if (listView != null) {
            listView.setOnItemFocusChangeListener((index, itemData, focused, node) -> {
                if (focused && index >= 0 && index < moviesList.size()) {
                    Movie selectedMovie = moviesList.get(index);
                    
                    VirtualNode backdrop = uiLayout.findViewById("backdrop");
                    VirtualNode title = uiLayout.findViewById("show_title");
                    VirtualNode desc = uiLayout.findViewById("show_description");

                    // Fade out
                    if (backdrop != null) {
                        com.rock.featherui.lib.animation.AnimationEngine.animate(backdrop, "alpha", backdrop.alpha, 0f, 150, "ease_out");
                    }
                    if (title != null) {
                        com.rock.featherui.lib.animation.AnimationEngine.animate(title, "alpha", title.alpha, 0f, 150, "ease_out");
                    }
                    if (desc != null) {
                        com.rock.featherui.lib.animation.AnimationEngine.animate(desc, "alpha", desc.alpha, 0f, 150, "ease_out");
                    }

                    // After fade out completes, update properties and fade back in
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        uiLayout.updateProperty("show_title", "text", selectedMovie.title.toUpperCase());
                        uiLayout.updateProperty("backdrop", "src", selectedMovie.poster);
                        uiLayout.updateProperty("show_description", "text", "Experience the epic journey of " + selectedMovie.title + " streaming now in stunning Ultra HD and spatial audio. Check out related titles in Featured Movies.");
                        
                        if (backdrop != null) {
                            com.rock.featherui.lib.animation.AnimationEngine.animate(backdrop, "alpha", 0f, 0.2f, 250, "ease_out");
                        }
                        if (title != null) {
                            com.rock.featherui.lib.animation.AnimationEngine.animate(title, "alpha", 0f, 1.0f, 250, "ease_out");
                        }
                        if (desc != null) {
                            com.rock.featherui.lib.animation.AnimationEngine.animate(desc, "alpha", 0f, 1.0f, 250, "ease_out");
                        }
                        featherUIView.requestLayoutOrPaint(null); // repaint to reflect new bounds and values
                    }, 150);
                }
            });

            listView.setOnItemClickListener((index, itemData, node) -> {
                if (index >= 0 && index < moviesList.size()) {
                    Movie selectedMovie = moviesList.get(index);
                    Toast.makeText(MainActivity.this, "Selected: " + selectedMovie.title, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private String loadJSONFromAsset(String fileName) {
        try {
            InputStream is = getAssets().open(fileName);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer, "UTF-8");
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }
}