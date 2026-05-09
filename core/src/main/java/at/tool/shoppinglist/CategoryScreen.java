package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.*;
import com.badlogic.gdx.InputAdapter;

import java.util.*;

public class CategoryScreen implements Screen {

    private final Main game;
    private final ShoppingList shoppingList;

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

    public CategoryScreen(Main game, ShoppingList shoppingList) {
        this.game = game;
        this.shoppingList = shoppingList;
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
        categories = new ArrayList<>(seen);
        Collections.sort(categories);

        calculateMaxScroll();

        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean touchDown(int x, int y, int ptr, int btn) {
                float wy = screenH - y;
                touchStartY  = wy;
                lastTouchY   = wy;
                dragging     = false;
                velocity     = 0f;
                return true;
            }

            @Override
            public boolean touchDragged(int x, int y, int ptr) {
                float wy = screenH - y;
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
                if (!dragging) handleTap(wx, wy);
                dragging = false;
                return true;
            }
        });
    }

    private void calculateMaxScroll() {
        float dp    = Gdx.graphics.getDensity();
        float tileH = 70f * dp;
        float rows  = (float) Math.ceil(categories.size() / COLS);
        float total = HEADER_H * dp + PAD * dp + rows * (tileH + PAD * dp);
        maxScroll   = Math.max(0, total - (screenH-ScreenState.BOTTOM_BAR_H));
    }

    private void handleTap(float wx, float wy) {
        // bottom bar - back button
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
            return;
        }



        // category tiles
        float pad    = PAD * dp;
        float tileW  = (screenW - pad * 3) / COLS;
        float tileH  = 70f * dp;
        float startY = screenH - HEADER_H * dp - pad;

        float contentWy = wy - scrollY;


        for (int i = 0; i < categories.size(); i++) {
            int col = i % 2;
            int row = i / 2;
            float x = pad + col * (tileW + pad);
            float y = startY - row * (tileH + pad) - tileH;
            Gdx.app.log("GRID", "i=" + i + " cat=" + categories.get(i) + " x=" + x + " y=" + y + " wy=" + contentWy);

            if (wx >= x && wx <= x + tileW && contentWy >= y && contentWy <= y + tileH) {
                game.showShoppingList(categories.get(i));
                return;
            }
        }
    }

    @Override
    public void render(float delta) {
        if (!dragging) {
            scrollY  = clamp(scrollY + velocity);
            velocity *= 0.88f;
        }

        screenW = Gdx.graphics.getWidth();
        screenH = Gdx.graphics.getHeight();

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
        fonts.title.setColor(ScreenColors.TEXT_PRI);
        fonts.title.draw(batch, "Categories", PAD * Gdx.graphics.getDensity(),
            screenH - PAD * 2 * Gdx.graphics.getDensity());
        batch.end();

        // grid
        float dp     = Gdx.graphics.getDensity();
        float pad    = PAD * dp;
        float tileW  = (screenW - pad * 2) / COLS-20;
        float tileH  = 70f * dp;
        float startY = screenH - HEADER_H * dp - pad + scrollY;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, 0, (int) screenW, (int) (screenH - HEADER_H * dp));

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
                    x + pad/2, y + tileH / 2f + 8f * dp);

                batch.end();
            }
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);

        //bottom bar
        float centerX = Gdx.graphics.getWidth() / 2f;
        batch.begin();
        batch.draw(barTexture, 0, 0, screenW, ScreenState.BOTTOM_BAR_H);
        batch.draw(btnTexture, centerX-160f*dp, ScreenState.BOTTOM_BAR_H/2f - 20f * dp,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        fonts.body.draw(batch, "Back to list",
            ScreenState.PAD,
            ScreenState.BOTTOM_BAR_H / 2f + 8f * dp);
        batch.end();
    }

    private float clamp(float s) {
        return Math.max(0, Math.min(s, maxScroll));
    }

    private String formatCategory(String raw) {
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
