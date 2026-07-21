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
    public float maxScroll = 4f;
    public float velocity = 0f;
    public boolean dragging = false;
    public boolean draggingScrollBar = false;
    public float scrollbarGrabOffset = 0f;
    public float lastTouchY;
    public float touchStartY;

    public String searchQuery = "";
    public boolean searchFocused = false;
    public ShoppingItem pendingRemove = null;

    public List<Object> rows = new ArrayList<>();
    public List<String> categoryOrder = new ArrayList<>();

    private final ShoppingList shoppingList;
    public AddRow focusedAddRow = null;
    public String addRowInput   = "";


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
            if (!item.isVisible()) continue;
            String cat = item.getCategory() != null ? item.getCategory() : "Other";
            byCat.computeIfAbsent(cat, k -> new ArrayList<>()).add(item);
        }


        List<String> cats = CategoryOrderStore.applyOrder(byCat.keySet());
        categoryOrder = new ArrayList<>(cats);

        for (String cat : cats) {
            rows.add(cat);
            rows.add(new AddRow(cat));
            rows.addAll(byCat.get(cat));

        }

        float total = HEADER_H + SEARCHBAR_H + PAD;
        for (Object r : rows) {
            if (r instanceof String) total += CAT_H;
            else if (r instanceof AddRow) total += ROW_H;
            else total += ROW_H;
        }
        maxScroll = Math.max(0, total - screenH + HEADER_H + SEARCHBAR_H + SELECT_ROW_H + BOTTOM_BAR_H);
        scrollY = clampScroll(scrollY);
    }

    public void update() {
        if (!dragging) {
            scrollY = clampScroll(scrollY + velocity);
            velocity *= 0.96f;
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
        HEADER_H     = 120f * dp;
        SEARCHBAR_H  = 44f  * dp;
        CHECKBOX_R   = 10f  * dp;
        BOTTOM_BAR_H = 60f * dp;
        SELECT_ROW_H = 40f * dp;
        SCROLLBAR_W = 10f * dp;
    }


    public boolean allVisible() {
        for (ShoppingItem item : shoppingList.getAll()) {
            if (!item.isVisible()) return false;
        }
        return true;
    }


    public void setAllVisible(boolean n){
        for (ShoppingItem item : shoppingList.getAll()) {
            item.setVisible(n);
            }
        }


public boolean allNeeded(){
    for (Object row : rows) {
        if (row instanceof ShoppingItem && !((ShoppingItem) row).isNeeded()){
            return false; }
    }
    return true;
}
    public boolean atLeastOneNeeded() {
        boolean oneNeeded = false;
        for (Object row : rows) {
            if (row instanceof ShoppingItem && ((ShoppingItem) row).isNeeded()){
                oneNeeded = true; }
        }
        return oneNeeded;
    }

    public boolean allDone() {
        for (Object row : rows) {
            if (row instanceof ShoppingItem && !((ShoppingItem) row).isDone()) return false;
        }
        return true;
    }

    public float getVisibleHeight(float h) {
        return h - HEADER_H - SEARCHBAR_H
            - SELECT_ROW_H - BOTTOM_BAR_H;
    }

    public float getTotalContentHeight() {
        float total = 0;

        for (Object row : rows) {
            total += (row instanceof String)
                ? CAT_H + 10
                : ROW_H;
        }

        return total;
    }

    public static class AddRow {
        public final String category;
        public AddRow(String category) { this.category = category; }
    }
}
