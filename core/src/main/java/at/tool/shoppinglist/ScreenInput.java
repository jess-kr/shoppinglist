package at.tool.shoppinglist;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Gdx;

import java.util.List;

public class ScreenInput extends InputAdapter {
    private final ScreenState state;
    private final ShoppingList shoppingList;
    private final ScreenRenderer renderer;

    public ScreenInput(ScreenState state, ShoppingList shoppingList, ScreenRenderer renderer) {
        this.state = state;
        this.shoppingList = shoppingList;
        this.renderer = renderer;
    }

    @Override
    public boolean touchDown(int x, int y, int ptr, int btn) {
//unfocus the input field if tapped somewhere else
        if (state.focusedAddRow != null) {
            state.focusedAddRow = null;
            state.addRowInput = "";
            Gdx.input.setOnscreenKeyboardVisible(false);
            state.rebuild();
        }
        float wy = state.screenH - y;
        state.touchStartY = wy;
        state.lastTouchY = wy;
        state.dragging = false;
        state.velocity = 0f;

        //scrollbar taps
        float barX = renderer.getScrollbarX(state.screenW);

        float visibleH = state.getVisibleHeight(state.screenH);

        float trackY = ScreenState.BOTTOM_BAR_H;

        float hitPadding = 20f * Gdx.graphics.getDensity();

        if (x >= barX - hitPadding &&
            x <= barX + ScreenState.SCROLLBAR_W + hitPadding &&
            wy >= trackY - hitPadding &&
            wy <= trackY + visibleH + hitPadding) {

            state.draggingScrollBar = true;

            float barH =
                renderer.getScrollbarHeight(state.screenH);

            float travel = visibleH - barH;

            float minY = ScreenState.BOTTOM_BAR_H;
            float maxY = minY + travel;

            // place thumb centered on tap
            float newBarY = wy - barH / 2f;

            newBarY = Math.max(minY,
                Math.min(maxY, newBarY));

            float ratio =
                1f - ((newBarY - minY) / travel);

            state.scrollY = ratio * state.maxScroll;

            // drag from thumb center
            state.scrollbarGrabOffset = barH / 2f;

            return true;
        }

        if (hitSearchBar(x, wy)) {
            state.searchFocused = true;
            Gdx.input.setOnscreenKeyboardVisible(true);
        } else {
            state.searchFocused = false;
            Gdx.input.setOnscreenKeyboardVisible(false);
        }
        return true;
    }

    @Override
    public boolean touchDragged(int x, int y, int ptr) {
        float scrollSpeed = 1.2f;
        float wy = state.screenH - y;
        float dy;

        //Scrollbar Dragging
        if (state.draggingScrollBar) {

            float visibleH =
                state.getVisibleHeight(state.screenH);

            float barH =
                renderer.getScrollbarHeight(state.screenH);

            float travel = visibleH - barH;

            float newBarY =
                wy - state.scrollbarGrabOffset;

            float minY = ScreenState.BOTTOM_BAR_H;
            float maxY = minY + travel;

            newBarY = Math.max(minY,
                Math.min(maxY, newBarY));

            float ratio =
                1f - ((newBarY - minY) / travel);

            state.scrollY = ratio * state.maxScroll;

            return true;
        }

        //Normal Content Dragging
        dy = wy - state.lastTouchY;
        float minY = Gdx.graphics.getHeight()-(ScreenState.SEARCHBAR_H+ScreenState.SELECT_ROW_H+ScreenState.HEADER_H);
        if (wy<minY){
            state.dragging=false;
        }
        if (Math.abs(wy - state.touchStartY) > 10f * Gdx.graphics.getDensity())
            state.dragging = true;

        if (state.touchStartY >= minY) {
            state.lastTouchY = wy;
            return true;
        }

        state.scrollY = state.clampScroll(state.scrollY + dy*scrollSpeed);
        state.velocity = dy*scrollSpeed;


        state.lastTouchY = wy;
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int ptr, int btn) {
        boolean wasDraggingScrollBar = state.draggingScrollBar;
        state.draggingScrollBar = false;

        Gdx.app.log("ShoppingList", "Touching up: " + x + ", " + y);

        float wy = state.screenH - y;
        if (!state.dragging && !wasDraggingScrollBar) handleTap(x, wy);
        state.dragging = false;
        return true;
    }

    @Override
    public boolean keyTyped(char c) {
        // ── Add row input ─────────────────────────────────────────────────────
        if (state.focusedAddRow != null) {
            if (c == '\n' || c == '\r') {
                // Enter — save the item
                String name = state.addRowInput.trim();
                if (!name.isEmpty()) {
                    shoppingList.getDatabase().saveNewItem(name, state.focusedAddRow.category);
                    shoppingList.reload();
                }
                state.focusedAddRow = null;
                state.addRowInput   = "";
                Gdx.input.setOnscreenKeyboardVisible(false);
                state.rebuild();
                return true;
            } else if (c == '\b') {
                if (!state.addRowInput.isEmpty())
                    state.addRowInput = state.addRowInput
                        .substring(0, state.addRowInput.length() - 1);
                state.rebuild();
            } else if (c >= 32) {
                state.addRowInput += c;
                state.rebuild();
            }
            return true;
        }

    //Search Bar Input
        if (!state.searchFocused) return false;
        if (c == '\b') {
            if (!state.searchQuery.isEmpty()) {
                state.searchQuery = state.searchQuery.substring(0, state.searchQuery.length() - 1);
            }
        } else if (c == '\r' || c == '\n') {
            state.searchFocused = false;
            Gdx.input.setOnscreenKeyboardVisible(false);
        } else if (c >= 32) {
            state.searchQuery += c;
        }
        state.rebuild();
        return true;
    }

    private void handleTap(float wx, float wy) {
        //Delete popup above all so other taps are blocked during it
        if (state.pendingRemove != null) {
            if (hitConfirm(wx, wy)) {
                ShoppingItem item = state.pendingRemove;
                shoppingList.removeItem(item.getName());
                shoppingList.getDatabase().removeItem(item.getName());
                state.pendingRemove = null;
                state.rebuild();
            } else if (hitCancel(wx, wy)) {
                state.pendingRemove = null;
            }
            return;
        }

            if (wy <= ScreenState.BOTTOM_BAR_H) {
                float centerX = state.screenW / 2f;

                // categories button
                if (wx <= centerX) {
                    ((Main) Gdx.app.getApplicationListener()).showCategories();
                    return;
                }
            }

            // select row
            float h = Gdx.graphics.getHeight();
            float selectRowY = h - ScreenState.HEADER_H/1.3f - ScreenState.SEARCHBAR_H;

            float cx1 = ScreenState.PAD + 20 + ScreenState.CHECKBOX_R;
            float cx3 = Gdx.graphics.getWidth()-ScreenState.SCROLLBAR_W-ScreenState.PAD*1.5f;
            float r = ScreenState.CHECKBOX_R + 10;

            if (Math.abs(wy - selectRowY - ScreenState.PAD / 3) < ScreenState.CHECKBOX_R + 10) {
                if (dist(wx, wy, cx1, selectRowY) < r) {
                    // toggle all needed
                    boolean allNeeded = shoppingList.getAll().stream().allMatch(ShoppingItem::isNeeded);
                    boolean target = !allNeeded;
                    List<ShoppingItem> allItems = shoppingList.getAll();
                    for (ShoppingItem item : allItems) {
                        item.setNeeded(target);
                    }
                    new Thread(() -> {
                        for (ShoppingItem item : allItems) {
                            shoppingList.getDatabase().saveNeededStatus(item.getName(), target);
                        }
                    }).start();
                    state.rebuild();
                    return;
                }

                //all shown
                if (dist(wx, wy, cx3, selectRowY) < r) {
                    toggleShowAll();
                    return;
                }
            }

            // item rows
        float y = state.screenH - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
            + ScreenState.CHECKBOX_R*1.45f - ScreenState.PAD/1.7f + state.scrollY;
        if (wy >= state.screenH - (ScreenState.HEADER_H+ScreenState.SEARCHBAR_H)) {
            return;}
            //start from the bottom - first see if tap in bottombar
            for (Object row : state.rows) {
                if (wy <= ScreenState.BOTTOM_BAR_H) break;
                if (row instanceof String) {
                    y -= ScreenState.CAT_H+10;
                } //then see if tap in Item-Add-Row
                    else if (row instanceof ScreenState.AddRow) {
                        ScreenState.AddRow ar = (ScreenState.AddRow) row;
                        float rowTop = y;
                        float rowBot = y - ScreenState.ROW_H;
                        if (wy >= rowBot && wy <= rowTop
                            && wx >= ScreenState.PAD && wx <= state.screenW - ScreenState.PAD) {
                            state.focusedAddRow = ar;
                            state.addRowInput   = "";
                            Gdx.input.setOnscreenKeyboardVisible(true);
                            state.rebuild();
                            return;
                        }
                        y -= ScreenState.ROW_H;
                } else {
                    ShoppingItem item = (ShoppingItem) row;
                    float rowTop = y;
                    float rowBot = y - ScreenState.ROW_H;



                    //now check different circles at the item-rows
                    if (wy >= rowBot && wy <= rowTop && wx >= ScreenState.PAD
                        && wx <= state.screenW - ScreenState.PAD) {

                        cx1 = ScreenState.PAD + 20 + ScreenState.CHECKBOX_R;
                        float cy = rowBot + ScreenState.ROW_H / 2f;
                        float itemR = ScreenState.CHECKBOX_R + 5;

                        // Red X hit — right edge of row
                        float xSize = 28f * Gdx.graphics.getDensity();
                        float xRight = state.screenW - ScreenState.PAD - 8f * Gdx.graphics.getDensity();
                        float xCenterX = xRight - xSize / 2f;

                        //done circle - remove item
                        if (Math.abs(wx - xCenterX) < xSize / 2f + 8f
                            && Math.abs(wy - cy) < xSize / 2f + 8f) {
                            state.pendingRemove = item;
                            return;
                        }

                        //needed circle
                        if (dist(wx, wy, cx1, cy) < itemR) {
                            shoppingList.toggleNeeded(item.getName());
                            state.rebuild();
                            return;

                        //done circle set item to done
                        } else if (dist(wx, wy, cx1 + 30 + ScreenState.CHECKBOX_R + ScreenState.PAD / 2, cy) < itemR) {
                            toggleDone(item);
                            state.rebuild();
                            return;
                        }

                        return;
                    }
                    y -= ScreenState.ROW_H;
                }
            }


    }

    public void jumpToCategory(String category) {
                float pos = ScreenState.HEADER_H + ScreenState.SEARCHBAR_H + ScreenState.PAD;
           for (Object row : state.rows) {
            if (row instanceof String) {
                if ((row).equals(category)) {
                    state.scrollY = state.clampScroll(pos - ScreenState.HEADER_H - ScreenState.SELECT_ROW_H-ScreenState.SEARCHBAR_H-ScreenState.PAD);
                    state.velocity = 0f;
                    return;
                }
                pos += ScreenState.CAT_H;
            } else {
                pos += ScreenState.ROW_H;
            }
        }
        Gdx.app.log("DEBUG", "category not found!");

    }

    private boolean hitSearchBar(float wx, float wy) {
        float h = Gdx.graphics.getHeight();
        float barX = ScreenState.PAD + ScreenState.PAD*3f;
        float barY =  h - ScreenState.HEADER_H/2f - ScreenState.SEARCHBAR_H+ScreenState.PAD/1.5f;
        float barW = state.screenW - barX - ScreenState.PAD;

        return wx >= barX &&
            wx <= barX+ barW &&
            wy >= barY &&
            wy <= barY + ScreenState.SEARCHBAR_H;

    }

    private void toggleDone(ShoppingItem item) {
        item.setDone(!item.isDone());
        shoppingList.getDatabase().saveDoneStatus(item.getName(), item.isDone());
        state.rebuild();
    }

    private void toggleShowAll() {
        if (state.allVisible()) {
            // Currently showing everything -> switch to showing only needed items
            List<ShoppingItem> allItems = shoppingList.getAll();
            for (ShoppingItem item : allItems) {
                // If an item is NOT needed, hide it from the current view
                item.setVisible(item.isNeeded());
            }
            new Thread(() -> {
                for (ShoppingItem item : allItems) {
                    shoppingList.getDatabase().saveVisibilityStatus(item.getName(), item.isVisible());
                }
            }).start();
        } else {
            // Currently filtered -> switch to showing everything in the "shopping list"
            state.setAllVisible(true);
        }
        state.rebuild();
    }

    private boolean hitConfirm(float wx, float wy) {
        float pw = 300f * Gdx.graphics.getDensity();
        float ph = 160f * Gdx.graphics.getDensity();
        float px = (state.screenW - pw) / 2f;
        float py = (state.screenH - ph) / 2f;
        float btnW = (pw - 30f * Gdx.graphics.getDensity()) / 2f;
        float btnH = 44f * Gdx.graphics.getDensity();
        float btnY = py + 16f * Gdx.graphics.getDensity();
        float confirmX = px + pw / 2f + 8f * Gdx.graphics.getDensity();
        return wx >= confirmX && wx <= confirmX + btnW && wy >= btnY && wy <= btnY + btnH;
    }

    private boolean hitCancel(float wx, float wy) {
        float pw = 300f * Gdx.graphics.getDensity();
        float ph = 160f * Gdx.graphics.getDensity();
        float px = (state.screenW - pw) / 2f;
        float py = (state.screenH - ph) / 2f;
        float btnW = (pw - 30f * Gdx.graphics.getDensity()) / 2f;
        float btnH = 44f * Gdx.graphics.getDensity();
        float btnY = py + 16f * Gdx.graphics.getDensity();
        float cancelX = px + 8f * Gdx.graphics.getDensity();
        return wx >= cancelX && wx <= cancelX + btnW && wy >= btnY && wy <= btnY + btnH;
    }

    private float dist(float x1, float y1, float x2, float y2) {
        float dx = x1 - x2, dy = y1 - y2;
        return (float) Math.sqrt(dx*dx + dy*dy);
    }
}
