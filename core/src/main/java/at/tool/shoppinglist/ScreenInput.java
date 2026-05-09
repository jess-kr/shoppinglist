package at.tool.shoppinglist;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Gdx;

public class ScreenInput extends InputAdapter {
    private final ScreenState state;
    private final ShoppingList shoppingList;

    public ScreenInput(ScreenState state, ShoppingList shoppingList) {
        this.state = state;
        this.shoppingList = shoppingList;
    }

    @Override
    public boolean touchDown(int x, int y, int ptr, int btn) {
        float wx = x, wy = state.screenH - y;
        state.touchStartY = wy;
        state.lastTouchY  = wy;
        state.dragging    = false;
        state.velocity    = 0f;

        if (hitSearchBar(wx, wy)) {
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
        float wy = state.screenH - y;
        float dy = wy - state.lastTouchY;
        if (Math.abs(wy - state.touchStartY) > 10f* Gdx.graphics.getDensity()) state.dragging = true;
        state.scrollY  = state.clampScroll(state.scrollY + dy);
        state.velocity = dy;
        state.lastTouchY = wy;
        return true;
    }

    @Override
    public boolean touchUp(int x, int y, int ptr, int btn) {
        Gdx.app.log("ShoppingList", "Touching up: " + x + ", " + y);
        float wx = x, wy = state.screenH - y;
        if (!state.dragging) handleTap(wx, wy);
        state.dragging = false;
        return true;
    }

    @Override
    public boolean keyTyped(char c) {
        if (!state.searchFocused) return false;
        if (c == '\b') {
            if (!state.searchQuery.isEmpty())
                state.searchQuery = state.searchQuery.substring(0, state.searchQuery.length() - 1);
        } else if (c >= 32) {
            state.searchQuery += c;
        }
        state.rebuild();
        return true;
    }

    private void handleTap(float wx, float wy) {
        if (wy <= ScreenState.BOTTOM_BAR_H) {
            float dp = Gdx.graphics.getDensity();
            float centerX = state.screenW / 2f;

            // categories button
            if (wx <= centerX) {
                ((Main) Gdx.app.getApplicationListener()).showCategories();
                return;
            }

            // add button
            if (wx > centerX) {
                ((Main) Gdx.app.getApplicationListener()).showAddItem();
                return;
            }
        }

        // select row
        float selectRowY = state.screenH - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
            - ScreenState.SELECT_ROW_H / 2f - 4f * Gdx.graphics.getDensity();
        float cx1 = ScreenState.PAD + 20 + ScreenState.CHECKBOX_R;
        float cx2 = cx1 + 30 + ScreenState.CHECKBOX_R + ScreenState.PAD / 2;

        if (Math.abs(wy - selectRowY) < ScreenState.CHECKBOX_R + 10) {
            if (Math.abs(wx - cx1) < ScreenState.CHECKBOX_R + 10) {
                // toggle all needed
                boolean allNeeded = shoppingList.getAll().stream().allMatch(ShoppingItem::isNeeded);
                for (ShoppingItem item : shoppingList.getAll()) {
                    item.setNeeded(!allNeeded);
                }
                state.rebuild();
                return;
            }
            if (Math.abs(wx - cx2) < ScreenState.CHECKBOX_R + 10) {
                // toggle all done
                boolean allDone = shoppingList.getAll().stream().allMatch(ShoppingItem::isDone);
                for (ShoppingItem item : shoppingList.getAll()) {
                    item.setDone(!allDone);
                }
                state.rebuild();
                return;
            }
        }

         // item rows
        float y = state.screenH - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
            - ScreenState.PAD + state.scrollY;
        for (Object row : state.rows) {
            if (row instanceof String) {
                y -= ScreenState.CAT_H;
            } else {
                ShoppingItem item = (ShoppingItem) row;
                float rowTop = y;
                float rowBot = y - ScreenState.ROW_H;

                if (wy >= rowBot && wy <= rowTop && wx >= ScreenState.PAD
                    && wx <= state.screenW - ScreenState.PAD) {


                    cx1 = ScreenState.PAD + 20 + ScreenState.CHECKBOX_R;
                    cx2 = cx1 + 30 + ScreenState.CHECKBOX_R + ScreenState.PAD / 2;
                    float cy  = rowBot + ScreenState.ROW_H / 2f;

                    float distToNeeded = Math.abs(wx - cx1);
                    float distToDone   = Math.abs(wx - cx2);
                    Gdx.app.log("DEBUG", "row hit at "+wx +" "+wy );

                    if (distToNeeded < ScreenState.CHECKBOX_R +5) {
                        shoppingList.toggleNeeded(item.getName());
                        Gdx.app.log("DEBUG","Needed toggled: " + item.getName());
                    } else if (distToDone < ScreenState.CHECKBOX_R +5) {
                        shoppingList.toggle(item.getName());
                        Gdx.app.log("DEBUG","Done toggled: " + item.getName());
                    }
                    state.rebuild();
                    return;
                }
                y -= ScreenState.ROW_H;
            }
        }


    }

    public void jumpToCategory(String category) {
        float dp = Gdx.graphics.getDensity();
        Gdx.app.log("DEBUG", "jumping to: " + category);
        float pos = ScreenState.HEADER_H + ScreenState.SEARCHBAR_H + ScreenState.PAD;
        Gdx.app.log("JUMP", "looking for: '" + category + "'");
        for (Object row : state.rows) {
            if (row instanceof String) {
                if (((String) row).equals(category)) {
                    state.scrollY  = state.clampScroll(pos - ScreenState.HEADER_H-67f*dp);
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
        float barY = state.screenH - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H;
        return wx >= ScreenState.PAD && wx <= state.screenW - ScreenState.PAD
                && wy >= barY && wy <= barY + ScreenState.SEARCHBAR_H;
    }
}
