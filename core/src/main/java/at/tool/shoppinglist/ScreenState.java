package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;

import java.util.*;

public class ScreenState {
    public com.badlogic.gdx.math.Matrix4 projMatrix = new com.badlogic.gdx.math.Matrix4();
    public static float PAD;
    public static float ROW_H;
    public static float CAT_H;
    public static float HEADER_H;
    public static float SEARCHBAR_H;
    public static float CHECKBOX_R;
    public static float BOTTOM_BAR_H = 60f;

    public static float SELECT_ROW_H = 40f;

    public static float SCROLLBAR_W = 6f;


    public float screenW, screenH;
    public float scrollY = 0f;
    public float maxScroll = 0f;
    public float velocity = 0f;
    public boolean dragging = false;
    public float lastTouchY;
    public float touchStartY;

    public String searchQuery = "";
    public boolean searchFocused = false;

    public List<Object> rows = new ArrayList<>();
    public List<String> categoryOrder = new ArrayList<>();

    private final ShoppingList shoppingList;

    public ScreenState(ShoppingList shoppingList) {
        ScreenState.initDensity();
        this.shoppingList = shoppingList;
    }

    public void rebuild() {
        ScreenState.initDensity();
        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();
        rows.clear();

        String q = searchQuery.toLowerCase().trim();
        Map<String, List<ShoppingItem>> byCat = new LinkedHashMap<>();

        for (ShoppingItem item : shoppingList.getAll()) {
            if (!q.isEmpty() && !item.getName().toLowerCase().contains(q)) continue;
            String cat = item.getCategory() != null ? item.getCategory() : "Other";
            byCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
        }

        List<String> cats = new ArrayList<>(byCat.keySet());
        Collections.sort(cats);
        categoryOrder = new ArrayList<>(cats);

        for (String cat : cats) {
            rows.add(cat);
            rows.addAll(byCat.get(cat));
        }

        float total = HEADER_H + SEARCHBAR_H + PAD;
        for (Object r : rows) total += (r instanceof String) ? CAT_H : ROW_H;
        maxScroll = Math.max(0, total - HEADER_H- screenH + PAD);
        scrollY = clampScroll(scrollY);
    }

    public void update() {
        if (!dragging) {
            scrollY = clampScroll(scrollY + velocity);
            velocity *= 0.88f;
        }
        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();
    }

    public float clampScroll(float s) {
        return Math.max(0, Math.min(s, maxScroll));
    }

    public static void initDensity() {
        float dp = Gdx.graphics.getDensity();
        PAD          = 30f  * dp;
        ROW_H        = 56f  * dp;
        CAT_H        = 28f  * dp;
        HEADER_H     = 140f * dp;
        SEARCHBAR_H  = 44f  * dp;
        CHECKBOX_R   = 10f  * dp;
        BOTTOM_BAR_H = 60f * dp;
        SELECT_ROW_H = 40f * dp;
        SCROLLBAR_W = 6f * dp;
    }

    public boolean allNeeded() {
        for (Object row : rows) {
            if (row instanceof ShoppingItem && !((ShoppingItem) row).isNeeded()) return false;
        }
        return true;
    }

    public boolean allDone() {
        for (Object row : rows) {
            if (row instanceof ShoppingItem && !((ShoppingItem) row).isDone()) return false;
        }
        return true;
    }
}
