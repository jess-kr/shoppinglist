package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.TimeUtils;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.InputAdapter;

import java.util.*;

public class CategoryScreen implements Screen {

    private final Main game;
    private final ShoppingList shoppingList;

    private final ScreenState state;
    private SpriteBatch batch;
    private ShapeRenderer shape;
    private OrthographicCamera camera;
    private Viewport viewport;
    private ScreenFonts fonts;

    private NinePatch catPatch;

    private List<String> categories = new ArrayList<>();
    private float scrollY = 0f;
    private float maxScroll = 0f;
    private float velocity = 0f;
    private float lastTouchY;
    private float touchStartY;
    private boolean dragging = false;

    private float screenW, screenH;

    private static final float COLS     = 2f;
    private static final float PAD      = 24f;
    private static final float HEADER_H = 120f;

    protected Texture barTexture;
    protected Texture bgTexture;

    protected Texture btnTexture;

    // ── Reorder mode ────────────────────────────────────────────────────────
    private boolean reorderMode = false;

    // a touch on a row that hasn't been held long enough to count as a drag yet
    private int pendingDragIndex = -1;
    private long pendingDragStartTime = 0L;
    private float pendingDragStartX = 0f, pendingDragStartY = 0f;

    // an actively-promoted drag (long press fired)
    private int draggedIndex = -1;
    private float dragScreenY = 0f; // current finger y, screen-space (y-up)

    private static final long LONG_PRESS_MS = 450L;
    private static final float LONG_PRESS_SLOP_DP = 10f;
    private static final float AUTO_SCROLL_EDGE_DP = 70f;
    private static final float AUTO_SCROLL_SPEED_DP = 7f;
    private static final float LIST_ROW_H = 70f; // dp, same visual size as grid tile

    public CategoryScreen(Main game, ShoppingList shoppingList, ScreenState state) {
        this.game = game;
        this.shoppingList = shoppingList;
        this.state = state;

        barTexture = new Texture(Gdx.files.internal("media/bar.png"));
        bgTexture = new Texture(Gdx.files.internal("media/background.png"));
        btnTexture = new Texture(Gdx.files.internal("media/button.png"));
    }

    @Override
    public void show() {
        batch    = new SpriteBatch();
        shape    = new ShapeRenderer();
        camera   = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        fonts    = new ScreenFonts();

        Texture catTex = new Texture(Gdx.files.internal("media/category.png"));
        catPatch = new NinePatch(catTex, 20, 20, 10, 10);

        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

        // collect unique categories
        Set<String> seen = new LinkedHashSet<>();
        for (ShoppingItem item : shoppingList.getAll()) {
            if (item.getCategory() != null) seen.add(item.getCategory());
        }
        categories = CategoryOrderStore.applyOrder(seen);

        calculateMaxScroll();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int ptr, int btn) {
                float wy = screenH - y;
                touchStartY  = wy;
                lastTouchY   = wy;
                dragging     = false;
                velocity     = 0f;

                if (reorderMode) {
                    int idx = rowIndexAt(x, wy);
                    if (idx >= 0) {
                        pendingDragIndex = idx;
                        pendingDragStartTime = TimeUtils.millis();
                        pendingDragStartX = x;
                        pendingDragStartY = wy;
                    }
                }
                return true;
            }

            @Override
            public boolean touchDragged(int x, int y, int ptr) {
                float wy = screenH - y;

                if (draggedIndex >= 0) {
                    // active drag — position is tracked every frame in render()
                    dragScreenY = wy;
                    return true;
                }

                if (pendingDragIndex >= 0) {
                    float dp = Gdx.graphics.getDensity();
                    float slop = LONG_PRESS_SLOP_DP * dp;
                    if (Math.abs(x - pendingDragStartX) > slop
                        || Math.abs(wy - pendingDragStartY) > slop) {
                        // moved too far before the long press fired — treat as a scroll instead
                        pendingDragIndex = -1;
                    } else {
                        return true; // small jitter while waiting for long press, ignore
                    }
                }

                float dy = wy - lastTouchY;
                if (Math.abs(wy - touchStartY) > 20f * Gdx.graphics.getDensity())
                    dragging = true;
                scrollY   = clamp(scrollY + dy);
                velocity  = dy;
                lastTouchY = wy;
                return true;
            }

            @Override
            public boolean touchUp(int x, int y, int ptr, int btn) {
                float wx = x, wy = screenH - y;

                if (draggedIndex >= 0) {
                    draggedIndex = -1;
                    CategoryOrderStore.save(categories);
                    return true;
                }

                pendingDragIndex = -1;

                if (!dragging) handleTap(wx, wy);
                dragging = false;
                return true;
            }
        });
    }

    private void calculateMaxScroll() {
        float dp    = Gdx.graphics.getDensity();
        float pad   = PAD * dp;
        float total;
        if (reorderMode) {
            float rowH = LIST_ROW_H * dp;
            total = HEADER_H * dp + pad + categories.size() * (rowH + pad);
        } else {
            float tileH = 70f * dp;
            float rows  = (float) Math.ceil(categories.size() / COLS);
            total = HEADER_H * dp + pad + rows * (tileH + pad);
        }
        maxScroll = Math.max(0, total - (screenH - ScreenState.BOTTOM_BAR_H + 10));
    }

    /** Content-space (no scrollY applied) top Y of row i in reorder-list layout. */
    private float listRowTop(int i) {
        float dp   = Gdx.graphics.getDensity();
        float pad  = (PAD / 2f) * dp;
        float rowH = LIST_ROW_H * dp;
        float startY = screenH - HEADER_H * dp - pad;
        return startY - i * (rowH + pad);
    }

    /** Hit-tests a screen-space touch point against the reorder list, returns index or -1. */
    private int rowIndexAt(float wx, float wy) {
        float dp   = Gdx.graphics.getDensity();
        float pad  = (PAD / 2f) * dp;
        float rowH = LIST_ROW_H * dp;
        float rowW = screenW - pad * 2f - ScreenState.SCROLLBAR_W;
        float contentWy = wy - scrollY;

        if (wx < pad || wx > pad + rowW) return -1;

        for (int i = 0; i < categories.size(); i++) {
            float top = listRowTop(i);
            float bottom = top - rowH;
            if (contentWy >= bottom && contentWy <= top) return i;
        }
        return -1;
    }

    /**
     * Called every frame while a drag is active. Auto-scrolls the list when the
     * finger is near the top/bottom edge, then moves the dragged category to
     * wherever the finger currently points.
     */
    private void handleAutoScrollAndTarget() {
        float dp = Gdx.graphics.getDensity();

        float topEdge    = screenH - HEADER_H * dp;
        float bottomEdge = ScreenState.BOTTOM_BAR_H + 10;
        float edgeZone   = AUTO_SCROLL_EDGE_DP * dp;

        if (dragScreenY > topEdge - edgeZone) {
            float t = Math.min(1f, (dragScreenY - (topEdge - edgeZone)) / edgeZone);
            scrollY = clamp(scrollY - AUTO_SCROLL_SPEED_DP * dp * t);
        } else if (dragScreenY < bottomEdge + edgeZone) {
            float t = Math.min(1f, ((bottomEdge + edgeZone) - dragScreenY) / edgeZone);
            scrollY = clamp(scrollY + AUTO_SCROLL_SPEED_DP * dp * t);
        }

        float pad  = (PAD / 2f) * dp;
        float rowH = LIST_ROW_H * dp;
        float startY = screenH - HEADER_H * dp - pad;
        float contentY = dragScreenY - scrollY;

        int targetIndex = Math.round((startY - contentY - rowH / 2f) / (rowH + pad));
        targetIndex = Math.max(0, Math.min(categories.size() - 1, targetIndex));

        if (targetIndex != draggedIndex) {
            String moved = categories.remove(draggedIndex);
            categories.add(targetIndex, moved);
            draggedIndex = targetIndex;
        }
    }

    private void handleTap(float wx, float wy) {
        float dp      = Gdx.graphics.getDensity();
        float centerX = screenW / 2f;

        // bottom bar
        if (wy <= ScreenState.BOTTOM_BAR_H) {
            float btnX = centerX - 160f * dp;
            float btnY = ScreenState.BOTTOM_BAR_H / 2f - 20f * dp;
            float btnW = 100f * dp;
            float btnH = ScreenState.BOTTOM_BAR_H - 50f;

            if (wx >= btnX && wx <= btnX + btnW
                && wy >= btnY && wy <= btnY + btnH) {
                game.showShoppingList(null);
                return;
            }

            // reorder toggle button (right side, mirrors the back button)
            float reorderBtnX = centerX + 60f * dp;
            if (wx >= reorderBtnX && wx <= reorderBtnX + btnW
                && wy >= btnY && wy <= btnY + btnH) {
                reorderMode = !reorderMode;
                if (!reorderMode) {
                    CategoryOrderStore.save(categories);
                }
                pendingDragIndex = -1;
                draggedIndex = -1;
                scrollY = 0f;
                velocity = 0f;
                calculateMaxScroll();
                return;
            }
            return;
        }

        if (reorderMode) return; // taps on rows do nothing but drag in reorder mode

        // category tiles (grid mode)
        float pad    = (PAD/2f) * dp;
        float tileW  = (screenW - pad * 1.3f) / COLS-20-ScreenState.SCROLLBAR_W;
        float tileH  = 70f * dp;
        float startY = screenH - HEADER_H * dp - pad;

        float contentWy = wy - scrollY;

        for (int i = 0; i < categories.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float x = pad + col * (tileW + pad);
            float y = startY - row * (tileH + pad) - tileH;

            if (wx >= x && wx <= x + tileW && contentWy >= y && contentWy <= y + tileH) {
                game.showShoppingList(categories.get(i));
                return;
            }
        }
    }

    @Override
    public void render(float delta) {
        state.projMatrix.set(camera.combined);

        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

        // promote a pending long-press touch into an active drag
        if (reorderMode && pendingDragIndex >= 0 && draggedIndex < 0) {
            if (Gdx.input.isTouched()) {
                long elapsed = TimeUtils.millis() - pendingDragStartTime;
                if (elapsed >= LONG_PRESS_MS) {
                    draggedIndex = pendingDragIndex;
                    pendingDragIndex = -1;
                    dragScreenY = screenH - Gdx.input.getY();
                    try {
                        Gdx.input.vibrate(30);
                    } catch (Exception e) {
                        Gdx.app.log("CategoryScreen", "Vibration unavailable: " + e.getMessage());
                    }
                }
            } else {
                pendingDragIndex = -1; // released before the long press fired
            }
        }

        if (draggedIndex >= 0) {
            dragScreenY = screenH - Gdx.input.getY();
            handleAutoScrollAndTarget();
        } else if (!dragging) {
            scrollY  = clamp(scrollY + velocity);
            velocity *= 0.88f;
        }

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, screenW, screenH);
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shape.setProjectionMatrix(camera.combined);

        // background
        batch.begin();
        batch.draw(bgTexture, 0, 0, screenW, screenH);
        batch.end();

        // header
        batch.begin();
        fonts.title.setColor(ScreenColors.TEXT_DARK);
        fonts.title.draw(batch, reorderMode ? "Reorder Categories" : "Categories",
            screenW/2-ScreenState.PAD*4f,
            screenH - PAD * 2f * Gdx.graphics.getDensity());
        batch.end();

        if (reorderMode) {
            renderReorderList();
        } else {
            renderGrid();
        }

        drawScrollbar();

        // bottom bar
        float dp = Gdx.graphics.getDensity();
        float centerX = Gdx.graphics.getWidth() / 2f;
        batch.begin();
        batch.draw(barTexture, 0, 0, screenW, ScreenState.BOTTOM_BAR_H);

        batch.draw(btnTexture, centerX-160f*dp, ScreenState.BOTTOM_BAR_H/2f - 20f * dp,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_DARK);
        fonts.body.draw(batch, "Back to list",
            ScreenState.PAD,
            ScreenState.BOTTOM_BAR_H / 2f + 8f * dp);

        batch.draw(btnTexture, centerX+60f*dp, ScreenState.BOTTOM_BAR_H/2f - 20f * dp,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_DARK);
        fonts.body.draw(batch, reorderMode ? "Done" : "Reorder",
            centerX + 70f * dp,
            ScreenState.BOTTOM_BAR_H / 2f + 8f * dp);
        batch.end();
    }

    private void renderGrid() {
        float dp     = Gdx.graphics.getDensity();
        float pad    = (PAD/2f) * dp;
        float tileW  = (screenW - pad * 1.3f) / COLS-20-ScreenState.SCROLLBAR_W;
        float tileH  = 70f * dp;
        float startY = screenH - HEADER_H * dp - pad + scrollY;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) ScreenState.BOTTOM_BAR_H+10, (int) screenW, (int) (screenH - ScreenState.BOTTOM_BAR_H- HEADER_H * dp));

        for (int i = 0; i < categories.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float x = pad + col * (tileW + pad);
            float y = startY - row * (tileH + pad) - tileH;

            if (y > -tileH && y < screenH) {
                batch.begin();
                catPatch.draw(batch, x, y, tileW, tileH);
                fonts.body.setColor(Color.WHITE);
                fonts.body.draw(batch, formatCategory(categories.get(i)),
                    x + pad/2f, y + tileH / 2f + 6f * dp);
                batch.end();
            }
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private void renderReorderList() {
        float dp   = Gdx.graphics.getDensity();
        float pad  = (PAD / 2f) * dp;
        float rowH = LIST_ROW_H * dp;
        float rowW = screenW - pad * 2f - ScreenState.SCROLLBAR_W;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) ScreenState.BOTTOM_BAR_H+10, (int) screenW, (int) (screenH - ScreenState.BOTTOM_BAR_H- HEADER_H * dp));

        for (int i = 0; i < categories.size(); i++) {
            boolean isDragged = (i == draggedIndex);
            boolean isPending = (i == pendingDragIndex);

            float y; // screen-space bottom of the row
            if (isDragged) {
                y = dragScreenY - rowH / 2f; // follow the finger directly
            } else {
                float top = listRowTop(i) + scrollY;
                y = top - rowH;
            }

            if (y > -rowH && y < screenH) {
                batch.begin();
                catPatch.draw(batch, pad, y, rowW, rowH);
                fonts.body.setColor(isDragged ? ScreenColors.TEXT_WHITE: Color.WHITE);
                fonts.body.draw(batch, formatCategory(categories.get(i)),
                    pad + 20f * dp, y + rowH / 2f + 6f * dp);
                // drag handle hint (dims briefly while a long-press is pending, to hint "hold on")
                fonts.small.setColor(isPending ? new Color(1f,1f,1f,0.5f) : Color.WHITE);
                fonts.small.draw(batch, "", pad + rowW - 40f * dp, y + rowH / 2f + 6f * dp);
                batch.end();
            }
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
    }

    private float clamp(float s) {
        return Math.max(0, Math.min(s, maxScroll));
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

    private void drawScrollbar() {
        float dp      = Gdx.graphics.getDensity();
        float pad     = PAD * dp;
        float totalH;
        if (reorderMode) {
            float rowH = LIST_ROW_H * dp;
            totalH = HEADER_H * dp + pad + categories.size() * (rowH + pad);
        } else {
            float tileH   = 70f * dp;
            float rows    = (float) Math.ceil(categories.size() / COLS);
            totalH  = HEADER_H * dp + pad + rows * (tileH + pad);
        }
        float visibleH = screenH - ScreenState.BOTTOM_BAR_H - HEADER_H * dp;

        if (totalH <= visibleH) return;

        float barRatio  = visibleH / totalH;
        float barH      = Math.max(40f * dp, visibleH * barRatio);
        float barTravel = visibleH - barH;
        float scrollRatio = maxScroll > 0 ? scrollY / maxScroll : 0;
        float barY      = ScreenState.BOTTOM_BAR_H + barTravel - scrollRatio * barTravel;
        float barX      = screenW - ScreenState.SCROLLBAR_W - 4f * dp;

        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(ScreenColors.TEXT_DARK);
        shape.rect(barX, barY, ScreenState.SCROLLBAR_W, barH);
        shape.end();
    }

    @Override public void resize(int w, int h) {
        viewport.update(w, h, true);
        screenW = w; screenH = h;
        calculateMaxScroll();
    }
    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
    @Override public void dispose() {
        batch.dispose();
        shape.dispose();
        fonts.dispose();
        bgTexture.dispose();
        barTexture.dispose();
        btnTexture.dispose();
        catPatch.getTexture().dispose();
    }
}
