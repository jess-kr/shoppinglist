package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

import org.w3c.dom.Text;

import java.awt.Shape;

import javax.swing.plaf.synth.SynthTextAreaUI;

public class ScreenRenderer {
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final ScreenFonts fonts;
    private final ScreenState state;
    protected Texture bgTexture;
    protected Texture rowTex;
    protected Texture barTexture;
    private Texture doneTexture;
    private Texture notDoneTexture;
    protected Texture catHeadTex;
    protected  Texture btnTex;
    private NinePatch rowPatch;
    private NinePatch catHeadPatch;

    public ScreenRenderer(SpriteBatch batch, ShapeRenderer shape,
            ScreenFonts fonts, ScreenState state) {
        this.batch = batch;
        this.shape = shape;
        this.fonts = fonts;
        this.state = state;
        bgTexture = new Texture(Gdx.files.internal("media/background.png"));
        bgTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);

        rowTex = new Texture(Gdx.files.internal("media/row.png"));
        catHeadTex = new Texture(Gdx.files.internal("media/category.png"));
        doneTexture = new Texture (Gdx.files.internal("media/check.png"));
        notDoneTexture = new Texture(Gdx.files.internal("media/non.png"));
        barTexture = new Texture(Gdx.files.internal("media/bar.png"));
        btnTex = new Texture(Gdx.files.internal("media/button.png"));

        rowPatch = new NinePatch(rowTex, 20, 20, 10, 10);
        catHeadPatch = new NinePatch(catHeadTex, 20, 20, 10, 10);
        }

    public void render(OrthographicCamera camera, float w, float h) {
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        state.projMatrix.set(camera.combined);

        drawBackground(w, h);
        drawHeader(w, h);
        drawSearchBar(w, h);
        drawRows(w, h);
        drawBottomBar(w, h);
    }

    private void drawBackground(float w, float h) {
        batch.begin();
        batch.draw(bgTexture, 0, 0, w, h);
        batch.end();
    }

    private void drawHeader(float w, float h) {
        batch.begin();
        fonts.title.setColor(ScreenColors.TEXT_PRI);
        fonts.title.draw(batch, "Shopping List", ScreenState.PAD, h - ScreenState.PAD*2);
        fonts.body.setColor(ScreenColors.TEXT_SEC);
        batch.end();



    }

    private void drawSearchBar(float w, float h) {
        float barY = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H+ScreenState.PAD/1.5f;
        float barW = w - ScreenState.PAD *3;

        batch.begin();
        catHeadPatch.draw(batch, ScreenState.PAD, barY-5 , barW, ScreenState.SEARCHBAR_H - 6);
        batch.end();


        batch.begin();
        String display = state.searchQuery.isEmpty() && !state.searchFocused
                ? "search items"
                : state.searchQuery + (state.searchFocused ? "|" : "");
        fonts.body.setColor(state.searchQuery.isEmpty() && !state.searchFocused
                ? Color.WHITE
                : ScreenColors.TEXT_SEC);
        fonts.body.draw(batch, display,
                ScreenState.PAD + 12,
                barY + (ScreenState.SEARCHBAR_H - 6) / 2f + 6);
        batch.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        shape.rect(ScreenState.PAD,h-barY,1,1);
        shape.end();
    }


    private void drawRows(float w, float h) {
        drawSelectRow(w, h);
        float y = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
                - ScreenState.PAD + state.scrollY;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) ScreenState.BOTTOM_BAR_H+10, (int) w,
            (int) (h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
                - ScreenState.SELECT_ROW_H- ScreenState.BOTTOM_BAR_H));

        for (Object row : state.rows) {
            if (row instanceof String) {
                if (y > 0 && y < h + ScreenState.CAT_H) {
                    batch.begin();
                    catHeadPatch.draw(batch, 0, y-ScreenState.PAD, w-ScreenState.PAD, ScreenState.CAT_H);
                    fonts.small.setColor(ScreenColors.TEXT_SEC);
                    fonts.small.draw(batch, formatCategory((String) row), ScreenState.PAD, y-ScreenState.PAD/2+10);
                    batch.end();
                }
                y -= ScreenState.CAT_H+10;
            } else {
                ShoppingItem item = (ShoppingItem) row;
                if (y > -ScreenState.ROW_H && y < h + ScreenState.ROW_H) {
                    drawItemRow(item, ScreenState.PAD, y - ScreenState.ROW_H,
                            w - ScreenState.PAD * 2-10, ScreenState.ROW_H);
                }
                y -= ScreenState.ROW_H;
            }
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        drawScrollbar(w, h);
    }

    private void drawItemRow(ShoppingItem item, float x, float y, float w, float h) {
        batch.begin();
        rowPatch.draw(batch, x, y + 2, w, h - 4);
        batch.end();

        float cx = x + 20 + ScreenState.CHECKBOX_R;
        float cy = y + h / 2f;
        float circleX = cx+30+ScreenState.CHECKBOX_R+ScreenState.PAD/2;

        shape.setProjectionMatrix(state.projMatrix);

         //1. Kreis to color if item is even needed this cycle
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(item.isNeeded()? ScreenColors.BLUE : Color.WHITE);
        shape.circle(cx,cy,ScreenState.CHECKBOX_R+10,32);
        shape.end();


            //Circle-Shape under checkmark
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.WHITE);
            shape.circle(circleX, cy, ScreenState.CHECKBOX_R+10, 32);
            shape.end();

            shape.begin(ShapeRenderer.ShapeType.Line);
            shape.setColor(item.isDone() ? ScreenColors.SURFACE : Color.WHITE);
            shape.circle(circleX, cy, ScreenState.CHECKBOX_R+10, 32);
            shape.end();

            //Checkmark if done
            batch.begin();
            batch.draw(item.isDone()? doneTexture : notDoneTexture, circleX-35, cy-35);
            batch.end();

            //Text in Row
            batch.begin();
            fonts.body.setColor(item.isDone() ? ScreenColors.TEXT_SEC : ScreenColors.TEXT_PRI);
            fonts.body.draw(batch, item.getName(), circleX + ScreenState.CHECKBOX_R + 50, cy + 20);
            batch.end();
        }


    public void drawBottomBar(float w, float h) {
        batch.begin();
        batch.draw(barTexture, 0, 0, w, ScreenState.BOTTOM_BAR_H);
        batch.end();
        drawBarButtons(w,h);
    }

    public void drawBarButtons(float w, float h){
        float dp      = Gdx.graphics.getDensity();
        float btnY    = ScreenState.BOTTOM_BAR_H / 2f + 8f * dp;
        float centerX = w / 2f;

        batch.begin();
        // categories button - left side
        batch.draw(btnTex, centerX-160f*dp, btnY/2-ScreenState.PAD/2f+20,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        fonts.body.draw(batch, "Categories",
            centerX - 150f * dp, btnY);

        // add button - right side
        fonts.body.setColor(ScreenColors.TEXT_PRI);
        batch.draw(btnTex, centerX+60f*dp, btnY/2-ScreenState.PAD/2f+20,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.draw(batch, "Add Item",
            centerX + 80f * dp, btnY);
        batch.end();
    }

    private void drawSelectRow(float w, float h) {
        float dp      = Gdx.graphics.getDensity();
        float rowY    = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
            - ScreenState.SELECT_ROW_H / 2f - 2.5f * dp;
        float cx1     = ScreenState.PAD + 20 + ScreenState.CHECKBOX_R;
        float cx2     = cx1 + 30 + ScreenState.CHECKBOX_R + ScreenState.PAD / 2;

        float r    = ScreenState.CHECKBOX_R + 10;


        boolean allNeeded = state.allNeeded();
        boolean allDone   = state.allDone();

        shape.setProjectionMatrix(state.projMatrix);

        // needed circle — filled blue if all needed, white outline if not
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(allNeeded ? ScreenColors.BLUE : Color.WHITE);
        shape.circle(cx1, rowY, r, 32);
        shape.end();


        // done circle
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.WHITE);
        shape.circle(cx2, rowY, r, 32);
        shape.end();

        // done texture overlay
        batch.begin();
        batch.draw(state.allDone()? doneTexture:notDoneTexture, cx2 - r, rowY - r, r * 2, r * 2);
        batch.end();


        // labels
        batch.begin();
        fonts.small.setColor(ScreenColors.TEXT_PRI);
        fonts.small.draw(batch, "need all", cx1 - (ScreenState.CHECKBOX_R+27f*dp), rowY + 30f*dp);
        fonts.small.draw(batch, "all done",   cx2 - ScreenState.CHECKBOX_R/2, rowY + 30f*dp);
        batch.end();
    }





    // HELPERS

    private String formatCategory(String raw) {
        String[] words = raw.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void drawScrollbar(float w, float h) {
        float totalH   = 0;
        for (Object row : state.rows)
            totalH += (row instanceof String) ? ScreenState.CAT_H + 10 : ScreenState.ROW_H;

        float visibleH  = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
            - ScreenState.SELECT_ROW_H - ScreenState.BOTTOM_BAR_H;

        if (totalH <= visibleH) return; // no scrollbar needed

        float barRatio  = visibleH / totalH;
        float barH      = Math.max(40f * Gdx.graphics.getDensity(), visibleH * barRatio);
        float barTravel = visibleH - barH;
        float scrollRatio = state.maxScroll > 0 ? state.scrollY / state.maxScroll : 0;
        float barY      = ScreenState.BOTTOM_BAR_H + barTravel - scrollRatio * barTravel;
        float barX      = w - ScreenState.SCROLLBAR_W - 4f * Gdx.graphics.getDensity();

        shape.setProjectionMatrix(state.projMatrix);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(ScreenColors.TEXT_PRI);
        shape.rect(barX, barY, ScreenState.SCROLLBAR_W, barH);
        shape.end();
    }

    public void dispose() {
        bgTexture.dispose();
        doneTexture.dispose();
        notDoneTexture.dispose();
        barTexture.dispose();
        catHeadTex.dispose();
        rowTex.dispose();
        btnTex.dispose();

        rowPatch.getTexture().dispose();
        catHeadPatch.getTexture().dispose();


    }

}
