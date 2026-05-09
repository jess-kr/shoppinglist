package at.tool.shoppinglist;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.*;


public class ShoppingListScreen implements Screen {

    private SpriteBatch batch;
    private ShapeRenderer shape;
    private OrthographicCamera camera;
    private Viewport viewport;

    private ShoppingList shoppingList;
    private ScreenFonts fonts;
    private ScreenRenderer renderer;
    private ScreenInput input;
    private ScreenState state;

    private ItemDatabase database;
    private String pendingCategory = null;

    public ShoppingListScreen(ItemDatabase database) {
        this.database = database;
    }

    @Override
    public void show() {
        batch  = new SpriteBatch();
        shape  = new ShapeRenderer();
        camera = new OrthographicCamera();
        viewport = new ScreenViewport(camera);
        shoppingList = new ShoppingList(new Items(database),database);
        fonts    = new ScreenFonts();
        state    = new ScreenState(shoppingList);
        renderer = new ScreenRenderer(batch, shape, fonts, state);
        input    = new ScreenInput(state, shoppingList);

        state.rebuild();
        if (pendingCategory != null) {
            input.jumpToCategory(pendingCategory);
            pendingCategory = null;}
        Gdx.input.setInputProcessor(input);
    }

    @Override
    public void render(float delta) {
        state.update();

        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        Gdx.gl.glClearColor(ScreenColors.BG.r, ScreenColors.BG.g, ScreenColors.BG.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.setToOrtho(false, w, h);
        camera.update();

        renderer.render(camera, w, h);
    }

    @Override
    public void resize(int w, int h) {
        viewport.update(w, h, true);
        state.rebuild();
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        shape.dispose();
        fonts.dispose();
        renderer.dispose();
    }

    public void jumpToCategory(String category) {
        state.rebuild();
        pendingCategory = category;
        input.jumpToCategory(category);
    }
}
