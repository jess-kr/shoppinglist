package at.tool.shoppinglist;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AddItemScreen implements Screen {

    private final Main game;
    private final ShoppingList shoppingList;

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ScreenFonts fonts;
    private Texture bgTexture;
    private Texture barTexture;
    private Texture btnTexture;
    private NinePatch rowPatch;

    private String itemName     = "";
    private String itemCategory = "";
    private boolean nameFocused = false;
    private float screenW, screenH;

    private boolean categoryDropdownOpen = false;
    private float dropdownScrollY = 0f;
    private float dropdownMaxScroll = 0f;
    private float dropdownLastTouchY;
    private float dropdownVelocity = 0f;
    private boolean dropdownDragging = false;
    private List<String> availableCategories = new ArrayList<>();

    public AddItemScreen(Main game, ShoppingList shoppingList) {
        this.game         = game;
        this.shoppingList = shoppingList;
    }

    @Override
    public void show() {
        batch    = new SpriteBatch();
        shape    = new ShapeRenderer();
        camera   = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        fonts    = new ScreenFonts();

        bgTexture = new Texture(Gdx.files.internal("media/background.png"));
        barTexture = new Texture(Gdx.files.internal("media/bar.png"));
        btnTexture = new Texture(Gdx.files.internal("media/button.png"));
        Texture rowTex = new Texture(Gdx.files.internal("media/row.png"));
        rowPatch = new NinePatch(rowTex, 20, 20, 10, 10);

        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

        Set<String> seen = new LinkedHashSet<>();
        for (ShoppingItem item : shoppingList.getAll()) {
            if (item.getCategory() != null) seen.add(item.getCategory());
        }
        availableCategories = new ArrayList<>(seen);
        Collections.sort(availableCategories);

        Gdx.input.setInputProcessor(new InputAdapter() {

            @Override
            public boolean touchUp(int x, int y, int ptr, int btn) {
                float wx = x, wy = screenH - y;
                float dp = Gdx.graphics.getDensity();
                float pad = ScreenState.PAD;
                float barW = screenW - pad * 2;
                float barH = 50f * dp;

                // back button
                if (wy <= ScreenState.BOTTOM_BAR_H && wx <= screenW / 2f) {
                    game.showShoppingList(null);
                    return true;
                }

                // save button
                if (wy <= ScreenState.BOTTOM_BAR_H && wx > screenW / 2f) {
                    if (!itemName.isEmpty()) {
                        shoppingList.addItem(itemName,
                            itemCategory.isEmpty() ? null : itemCategory);
                        game.showShoppingList(null);
                    }
                    return true;
                }

                // dropdown item selection
                if (categoryDropdownOpen) {
                    if (!dropdownDragging) {
                        float itemH   = 50f * dp;
                        float itemGap = 4f * dp;
                        float dropH   = screenH * 0.45f - ScreenState.BOTTOM_BAR_H;
                        float startY  = ScreenState.BOTTOM_BAR_H + dropH - itemH + dropdownScrollY;

                        for (String cat : availableCategories) {
                            if (wy >= startY && wy <= startY + itemH
                                && wx >= pad && wx <= pad + barW) {
                                itemCategory = cat;
                                categoryDropdownOpen = false;
                                return true;
                            }
                            startY -= itemH + itemGap;
                        }
                    }
                    dropdownDragging = false;
                    return true;
                }

                // category field tap — open dropdown
                if (hitField(wx, wy, screenH * 0.45f)) {
                    categoryDropdownOpen = true;
                    dropdownScrollY  = 0f;
                    dropdownVelocity = 0f;
                    float itemH   = 50f * dp;
                    float itemGap = 4f * dp;
                    float totalH  = availableCategories.size() * (itemH + itemGap);
                    float visibleH = screenH * 0.45f - ScreenState.BOTTOM_BAR_H;
                    dropdownMaxScroll = Math.max(0, totalH - visibleH);
                    return true;
                }

                // name field
                if (hitField(wx, wy, screenH * 0.65f)) {
                    nameFocused = true;
                    categoryDropdownOpen = false;
                    Gdx.input.setOnscreenKeyboardVisible(true);
                    return true;
                }

                return true;
            }

            @Override
            public boolean touchDown(int x, int y, int ptr, int btn) {
                float wy = screenH - y;
                if (categoryDropdownOpen && wy > ScreenState.BOTTOM_BAR_H
                    && wy < screenH * 0.45f) {
                    dropdownLastTouchY = wy;
                    dropdownDragging   = false;
                    dropdownVelocity   = 0f;
                }
                return true;
            }
            @Override
            public boolean touchDragged(int x, int y, int ptr) {
                float wy = screenH - y;
                if (categoryDropdownOpen) {
                    float dy = wy - dropdownLastTouchY;
                    if (Math.abs(dy) > 8f) dropdownDragging = true;
                    dropdownScrollY  = clampDropdown(dropdownScrollY + dy);
                    dropdownVelocity = dy;
                    dropdownLastTouchY = wy;
                }
                return true;
            }

            @Override
            public boolean keyTyped(char c) {
                if (!nameFocused) return false;
                if (c == '\b') {
                    if (!itemName.isEmpty())
                        itemName = itemName.substring(0, itemName.length() - 1);
                } else if (c >= 32) {
                    itemName += c;
                }
                return true;
            }
        });

        Gdx.input.setOnscreenKeyboardVisible(false);
    }

    private boolean hitField(float wx, float wy, float fieldY) {
        float dp   = Gdx.graphics.getDensity();
        float padH = ScreenState.PAD;
        float barW = screenW - padH * 2;
        float barH = 50f * dp;
        return wx >= padH && wx <= padH + barW
            && wy >= fieldY && wy <= fieldY + barH;
    }

    @Override
    public void render(float delta) {
        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, screenW, screenH);
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        float dp   = Gdx.graphics.getDensity();
        float pad  = ScreenState.PAD;
        float barW = screenW - pad * 2;
        float barH = 50f * dp;

        // background
        batch.begin();
        batch.draw(bgTexture, 0, 0, screenW, screenH);
        batch.end();

        // title
        batch.begin();
        fonts.title.setColor(ScreenColors.TEXT_PRI);
        fonts.title.draw(batch, "Add Item", pad, screenH - pad * 2);
        batch.end();

        // name field
        batch.begin();
        rowPatch.draw(batch, pad, screenH * 0.65f, barW, barH);
        fonts.small.setColor(ScreenColors.TEXT_PRI);
        fonts.small.draw(batch, "Item name", pad + 12, screenH * 0.65f + barH + 17f * dp);
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        fonts.body.draw(batch,
            itemName + (nameFocused ? "|" : ""),
            pad + 12, screenH * 0.65f + barH / 2f + 8f * dp);
        batch.end();

        // category field
        batch.begin();
        rowPatch.draw(batch, pad, screenH * 0.45f, barW, barH);
        fonts.small.setColor(ScreenColors.TEXT_PRI);
        fonts.small.draw(batch, "Category", pad + 12, screenH * 0.45f + barH + 17f * dp);
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        fonts.body.draw(batch,
            itemCategory.isEmpty() ? "tap to select..." : itemCategory,
            pad + 12, screenH * 0.45f + barH / 2f + 8f * dp);
        batch.end();



        // draw dropdown list if open
        if (categoryDropdownOpen) {
            float dropH = screenH * 0.45f - ScreenState.BOTTOM_BAR_H;
            float dropX = pad;
            float dropY_base = ScreenState.BOTTOM_BAR_H+10;

            // white background
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.WHITE);
            shape.rect(pad, dropY_base, barW, dropH);
            shape.end();

            // scissor clip
            Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
            Gdx.gl.glScissor((int)dropX, (int)dropY_base, (int)barW, (int)dropH);

            // inertia
            dropdownScrollY  = clampDropdown(dropdownScrollY + dropdownVelocity);
            dropdownVelocity *= 0.88f;

            // items — draw from top downward
            float itemH   = 50f * dp;
            float itemGap = 4f * dp;
            float startY  = dropY_base + dropH - itemH + dropdownScrollY;

            for (String cat : availableCategories) {
                float iy = startY;
                if (iy + itemH > dropY_base && iy < dropY_base + dropH) {
                    batch.begin();
                    fonts.body.setColor(cat.equals(itemCategory)
                        ? ScreenColors.BLUE : ScreenColors.TEXT_PRI);
                    fonts.body.draw(batch, formatCategory(cat), dropX + 16, iy + itemH / 2f + 8f * dp);
                    batch.end();
                }
                startY -= itemH + itemGap;
            }

            Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

            // scrollbar
            float totalH   = availableCategories.size() * (itemH + itemGap);
            float barRatio = dropH / totalH;
            float barHgt   = Math.max(40f * dp, dropH * barRatio);
            float barTravel = dropH - barHgt;
            float scrollRatio = dropdownMaxScroll > 0 ? dropdownScrollY / dropdownMaxScroll : 0;
            float barTop   = dropY_base + dropH - barHgt - scrollRatio * barTravel;

            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(ScreenColors.TEXT_PRI);
            shape.rect(dropX + barW - 8f * dp, barTop, 6f * dp, barHgt);
            shape.end();
        }


        //bottom bar
        float centerX = Gdx.graphics.getWidth() / 2f;
        float btnY    = ScreenState.BOTTOM_BAR_H / 2f + 8f * dp;

        batch.begin();
        batch.draw(barTexture, 0, 0, screenW, ScreenState.BOTTOM_BAR_H);


        //Back Button
        batch.draw(btnTexture, centerX-160f*dp, btnY/2-ScreenState.PAD/2f+20,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        fonts.body.draw(batch, "Back to list",
            ScreenState.PAD,
            ScreenState.BOTTOM_BAR_H / 2f + 8f * dp);

        //Add Button
        batch.draw(btnTexture, centerX+60f*dp, btnY/2-ScreenState.PAD/2f+20,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        fonts.body.draw(batch, "Add Item",
            centerX + 80f * dp,
            ScreenState.BOTTOM_BAR_H / 2f + 8f * dp);


        batch.end();

    }

    @Override public void resize(int w, int h) {
        viewport.update(w, h, true);
        screenW = w; screenH = h;
    }

    public String formatCategory(String raw) {
        String[] words = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                    .append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private float clampDropdown(float s) {
        return Math.max(0, Math.min(s, dropdownMaxScroll));
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        fonts.dispose();
        bgTexture.dispose();
        barTexture.dispose();
        rowPatch.getTexture().dispose();
    }
}
