package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class ScreenRenderer {
    private final SpriteBatch batch;
    private final ShapeRenderer shape;
    private final ScreenFonts fonts;
    private final ScreenState state;
    protected Texture bgTexture;
    protected Texture rowTex;
    protected Texture barTexture;
    protected  Texture ghostRowTex;
    private final Texture doneTexture;
    private final Texture notDoneTexture;
    protected Texture catHeadTex;
    protected  Texture btnTex;
    private final Texture searchIcon;
    private final Texture visibleTexture;
    private final Texture notAllVisibleTexture;
    private final NinePatch rowPatch;
    private final NinePatch catHeadPatch;
    private final NinePatch ghostRowPatch;


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
        ghostRowTex = new Texture(Gdx.files.internal("media/ghostrow.png"));

        doneTexture = new Texture (Gdx.files.internal("media/check.png"));
        notDoneTexture = new Texture(Gdx.files.internal("media/non.png"));
        barTexture = new Texture(Gdx.files.internal("media/bar.png"));
        btnTex = new Texture(Gdx.files.internal("media/button.png"));
        searchIcon = new Texture(Gdx.files.internal("media/search.png"));
        visibleTexture = new Texture(Gdx.files.internal("media/visible.png"));
        notAllVisibleTexture = new Texture(Gdx.files.internal("media/notallvisible.png"));

        rowPatch = new NinePatch(rowTex, 20, 20, 10, 10);
        catHeadPatch = new NinePatch(catHeadTex, 20, 20, 10, 10);
        ghostRowPatch = new NinePatch(ghostRowTex,20, 20, 10, 10);

        }

    public void render(OrthographicCamera camera, float w, float h) {
        shape.setProjectionMatrix(camera.combined);
        batch.setProjectionMatrix(camera.combined);
        state.projMatrix.set(camera.combined);

        drawBackground(w, h);
        drawHeader(h);
        drawSearchBar(w, h);
        drawBorder(h+15);

        drawRows(w, h);
        drawStickyHeaders(w,h);
        drawGhostRows(w,h);
        drawBottomBar(w);
        if (state.pendingRemove != null) drawConfirmPopup(w, h);
    }

    private void drawBackground(float w, float h) {
        batch.begin();
        batch.draw(bgTexture, 0, 0, w, h);
        batch.end();
    }

    private void drawHeader(float h) {
        batch.begin();
        fonts.title.setColor(ScreenColors.TEXT_DARK);
        fonts.title.draw(batch, "", ScreenState.PAD, h - ScreenState.PAD*2);
        fonts.body.setColor(ScreenColors.TEXT_WHITE);
        batch.end();
    }

    private void drawSearchBar(float w, float h) {
        float barY = h - ScreenState.HEADER_H/2f - ScreenState.SEARCHBAR_H;
        float barW = w - ScreenState.PAD *3;

        batch.begin();
        catHeadPatch.draw(batch, ScreenState.PAD, barY, barW, ScreenState.SEARCHBAR_H - 6);
        batch.end();

        batch.begin();
        batch.draw(searchIcon, ScreenState.PAD+ScreenState.PAD/1.5f,barY+20,ScreenState.PAD,ScreenState.PAD);
        batch.end();


        batch.begin();
        String display = state.searchQuery.isEmpty() && !state.searchFocused
                ? "search items"
                : state.searchQuery + (state.searchFocused ? "|" : "");
        fonts.body.setColor(state.searchQuery.isEmpty() && !state.searchFocused
                ? Color.WHITE
                : ScreenColors.TEXT_WHITE);
        fonts.body.draw(batch, display,
                ScreenState.PAD + ScreenState.PAD*3f,
                barY +ScreenState.PAD/1.2f);
        batch.end();

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.BLACK);
        shape.rect(ScreenState.PAD,h-barY,1,1);
        shape.end();
    }

    private void drawSelectRow(float h) {
        float dp      = Gdx.graphics.getDensity();
        float rowY    = h - ScreenState.HEADER_H/1.3f - ScreenState.SEARCHBAR_H;
        float cx1     = ScreenState.PAD + 20 + ScreenState.CHECKBOX_R;
        float cx3 = Gdx.graphics.getWidth()-ScreenState.SCROLLBAR_W-ScreenState.PAD*1.5f;

        float r    = ScreenState.CHECKBOX_R + 10;


        boolean oneNeeded = state.atLeastOneNeeded();
        boolean allVisible = state.allVisible();

        shape.setProjectionMatrix(state.projMatrix);

        // needed circle — filled red if one is needed, white  if not
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.WHITE);
        shape.circle(cx1,rowY,r+5f, 32);
        shape.setColor(oneNeeded ? ScreenColors.RED : Color.WHITE);
        shape.circle(cx1, rowY, r, 32);
        shape.end();


        //show all circle
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.WHITE);
            shape.circle(cx3, rowY, r, 32);
            shape.end();

       //Icon Overlay
        float scale = 0.15f;
        float visibleIconWidth = visibleTexture.getWidth()*scale;
        float visibleIconHeight = visibleTexture.getHeight()*scale;

        float notVisibleIconHeight = notAllVisibleTexture.getHeight()*scale;
        float notVisibleIconWidth = notAllVisibleTexture.getWidth()*scale;
            batch.begin();
            batch.draw(allVisible? visibleTexture: notAllVisibleTexture , cx3-ScreenState.PAD/2.3f, rowY+ScreenState.CHECKBOX_R/2f-17f*dp, allVisible? visibleIconWidth: notVisibleIconWidth, allVisible? visibleIconHeight : notVisibleIconHeight);
            batch.end();
    }
private void drawBorder(float h){
    float rowY    = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H;
    float dp = Gdx.graphics.getDensity();
    float y = rowY+ScreenState.CHECKBOX_R*1.45f-ScreenState.PAD/1.1f;
    float width = Gdx.graphics.getWidth()*dp;

    shape.begin(ShapeRenderer.ShapeType.Filled);
    shape.setColor(ScreenColors.BLUE);
    shape.rect(0,y,width,ScreenState.PAD/3);
    shape.end();
}

    private void drawStickyHeaders(float w, float h){
        float listTop = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H+ScreenState.CHECKBOX_R*1.5f-ScreenState.PAD/1.1f;
        float currentY = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H - ScreenState.PAD/2f + state.scrollY;

        String currentCategory = null;

        for (Object row : state.rows) {
            if (row instanceof String) {
                if (currentY > listTop) {
                    currentCategory = (String) row;
                }
                currentY -= ScreenState.CAT_H + 10;
            } else {
                currentY -= ScreenState.ROW_H;
            }
        }

        float labelPosition = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H -ScreenState.CHECKBOX_R-ScreenState.PAD/1.1f;

        if (currentCategory != null) {
            batch.begin();
            catHeadPatch.draw(batch, 0, labelPosition+15,
                w - ScreenState.PAD, ScreenState.CAT_H);
            fonts.small.setColor(ScreenColors.TEXT_WHITE);
            fonts.small.draw(batch, formatCategory(currentCategory),
                ScreenState.PAD, labelPosition+ScreenState.PAD-10 );
            batch.end();
        }
    }

    private void drawGhostRows(float w, float h) {

        float y = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H+ScreenState.CHECKBOX_R*1.45f-ScreenState.PAD/1.7f+state.scrollY;
        float upperScissors = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H -ScreenState.CHECKBOX_R/3f-ScreenState.PAD*2f;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) ScreenState.BOTTOM_BAR_H+10, (int) w,
            (int) (upperScissors));

        for (Object row : state.rows) {
            if (row instanceof String) {
                if (y > 0 && y < h + ScreenState.CAT_H) {
                    float stickyTop = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H
                        - ScreenState.CHECKBOX_R-ScreenState.PAD/2f;
                    boolean inStickyZone = (y - ScreenState.PAD+15) >= stickyTop;


                    batch.begin();
                    if(!inStickyZone) {
                    catHeadPatch.draw(batch, 0, y-ScreenState.PAD, w-ScreenState.PAD, ScreenState.CAT_H);
                    fonts.small.setColor(ScreenColors.TEXT_WHITE);
                    fonts.small.draw(batch, formatCategory((String) row), ScreenState.PAD, y - ScreenState.CAT_H/3.3f);
                    }
                    if(inStickyZone)
                    {
                        ghostRowPatch.draw(batch, 0, y-ScreenState.PAD, w-ScreenState.PAD, ScreenState.CAT_H);
                    }
                        batch.end();

                }
                y -= ScreenState.CAT_H+10;
            }   else if (row instanceof ScreenState.AddRow) {
                if (y > -ScreenState.ROW_H && y < h + ScreenState.ROW_H) {
                    drawGhostAddRow(ScreenState.PAD, y - ScreenState.ROW_H,
                        w - ScreenState.PAD * 2 - 10, ScreenState.ROW_H);
                }
                y -= ScreenState.ROW_H;
            } else {
                if (y > -ScreenState.ROW_H && y < h + ScreenState.ROW_H) {
                    drawGhostItemRow(ScreenState.PAD, y - ScreenState.ROW_H,
                        w - ScreenState.PAD * 2-10, ScreenState.ROW_H);
                }
                y -= ScreenState.ROW_H;
            }
        }

        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST);
        drawScrollbar(w, h);
        drawSelectRow(h);

    }

    private void drawRows(float w, float h) {
        float y = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H+ScreenState.CHECKBOX_R*1.45f-ScreenState.PAD/1.7f+state.scrollY;
        float upperScissors = h - ScreenState.HEADER_H - ScreenState.SEARCHBAR_H -ScreenState.CHECKBOX_R/3f-ScreenState.PAD*2f;

        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST);
        Gdx.gl.glScissor(0, (int) ScreenState.BOTTOM_BAR_H+10, (int) w,
            (int) (upperScissors-ScreenState.PAD/2f));

        for (Object row : state.rows) {
            if (row instanceof String) {
                if (y > 0 && y < h + ScreenState.CAT_H) {
                    batch.begin();
                    catHeadPatch.draw(batch, 0, y-ScreenState.PAD, w-ScreenState.PAD, ScreenState.CAT_H);
                    fonts.small.setColor(ScreenColors.TEXT_WHITE);
                    fonts.small.draw(batch, formatCategory((String) row), ScreenState.PAD, y-ScreenState.PAD/2+10);
                    batch.end();
                }
                y -= ScreenState.CAT_H+10;
            }   else if (row instanceof ScreenState.AddRow) {
            ScreenState.AddRow ar = (ScreenState.AddRow) row;
            if (y > -ScreenState.ROW_H && y < h + ScreenState.ROW_H) {
                drawAddRow(ar, ScreenState.PAD, y - ScreenState.ROW_H,
                    w - ScreenState.PAD * 2 - 10, ScreenState.ROW_H);
            }
            y -= ScreenState.ROW_H;
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
        drawSelectRow(h);

    }

    private void drawGhostAddRow( float x, float y, float w, float h) {
        shape.setProjectionMatrix(state.projMatrix);
        batch.setProjectionMatrix(state.projMatrix);


        batch.begin();
        ghostRowPatch.draw(batch, x, y + 2, w, h - 4);
        batch.end();
    }

    private void drawAddRow(ScreenState.AddRow ar, float x, float y, float w, float h) {
        shape.setProjectionMatrix(state.projMatrix);
        batch.setProjectionMatrix(state.projMatrix);
        boolean focused = state.focusedAddRow != null
            && ar.category.equals(state.focusedAddRow.category);
        float dp = Gdx.graphics.getDensity();

        // row background — slightly different tint from normal rows
        batch.begin();
        rowPatch.draw(batch, x, y + 2, w, h - 4);
        batch.end();

        // + circle
        float cx = x + 20 + ScreenState.CHECKBOX_R + 30 + ScreenState.CHECKBOX_R + ScreenState.PAD / 2;
        float cy = y + h / 2f;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(focused ? ScreenColors.BLUE : new Color(0.4f, 0.4f, 0.4f, 1f));
        shape.circle(cx, cy, ScreenState.CHECKBOX_R + 10, 32);
        shape.end();

        // + lines
        float arm = 8f * dp;
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.WHITE);
        shape.rect(cx - arm, cy - 2f * dp, arm * 2, 4f * dp); // horizontal
        shape.rect(cx - 2f * dp, cy - arm, 4f * dp, arm * 2); // vertical
        shape.end();

        // placeholder or typed text
        String display = focused
            ? state.addRowInput + "|"
            : "Add item...";
        batch.begin();
        fonts.body.setColor(focused ? ScreenColors.TEXT_WHITE : new Color(1f, 1f, 1f, 0.4f));
        fonts.body.draw(batch, display,
            cx + ScreenState.CHECKBOX_R + 50, cy + 20);
        batch.end();
    }

    private void drawGhostItemRow(float x, float y, float w, float h){
        batch.begin();
        ghostRowPatch.draw(batch, x, y + 2, w, h - 4);
        batch.end();
    }


    private void drawItemRow(ShoppingItem item, float x, float y, float w, float h) {
        batch.begin();
        rowPatch.draw(batch, x, y + 2, w, h - 4);
        batch.end();

        float cx = x + 20 + ScreenState.CHECKBOX_R;
        float cy = y + h / 2f;
        float circleX = cx+30+ScreenState.CHECKBOX_R+ScreenState.PAD/1.7f;

        shape.setProjectionMatrix(state.projMatrix);

         //1. Kreis to color if item is even needed this cycle
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(Color.WHITE);
        shape.circle(cx,cy,ScreenState.CHECKBOX_R+15, 32);
        shape.setColor(item.isNeeded()? ScreenColors.RED : Color.WHITE);
        shape.circle(cx,cy,ScreenState.CHECKBOX_R+10,32);
        shape.end();


            //Circle-Shape under checkmark
            shape.begin(ShapeRenderer.ShapeType.Filled);
            shape.setColor(Color.WHITE);
            shape.circle(circleX, cy, ScreenState.CHECKBOX_R+15, 32);
            shape.end();


            //Checkmark if done
            boolean isDone = item.isDone();
            batch.begin();
            batch.draw(isDone ? doneTexture : notDoneTexture, circleX-35, cy-35);
            batch.end();

            //Text in Row
            batch.begin();
            fonts.body.setColor(isDone ? new Color(1, 1, 1, 0.4f) : ScreenColors.TEXT_WHITE);
            fonts.body.draw(batch, item.getName(), circleX + ScreenState.CHECKBOX_R + 50, cy + 20);
            batch.end();

        //Remove-Feature
        float xSize = 28f * Gdx.graphics.getDensity();
        float xRight = x + w - 8f * Gdx.graphics.getDensity();
        float xCenterX = xRight - xSize / 2f;
        float xCenterY = y + h / 2f;

        shape.begin(ShapeRenderer.ShapeType.Line);
        shape.setColor(Color.WHITE);
        float half = xSize * 0.28f;
        shape.line(xCenterX - half, xCenterY - half, xCenterX + half, xCenterY + half);
        shape.line(xCenterX + half, xCenterY - half, xCenterX - half, xCenterY + half);
        shape.end();
        }



    public void drawBottomBar(float w) {
        batch.begin();
        batch.draw(barTexture, 0, 0, w, ScreenState.BOTTOM_BAR_H);
        batch.end();
        drawBarButtons(w);
    }

    public void drawBarButtons(float w){
        float dp      = Gdx.graphics.getDensity();
        float btnY    = ScreenState.BOTTOM_BAR_H / 2f + 8f * dp;
        float centerX = w / 2f;

        batch.begin();
        // categories button
        batch.draw(btnTex, centerX-160f*dp, btnY/2-ScreenState.PAD/2f+20,100*dp,ScreenState.BOTTOM_BAR_H-50);
        fonts.body.setColor(ScreenColors.TEXT_DARK);
        fonts.body.draw(batch, "Categories",
            centerX - 150f * dp, btnY);
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

        float totalH = state.getTotalContentHeight();
        float visibleH = state.getVisibleHeight(h);

        if (totalH <= visibleH) return;

        float barH = getScrollbarHeight(h);
        float barY = getScrollbarY(h);
        float barX = getScrollbarX(w);

        //translucent thingy under scrollbar
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(new Color(1f,1f,1f,0f));
        shape.rect(barX,ScreenState.BOTTOM_BAR_H+10,ScreenState.SCROLLBAR_W,visibleH);
        shape.end();

        //Scrollbar
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(ScreenColors.TEXT_DARK);
        shape.rect(barX, barY,
            ScreenState.SCROLLBAR_W,
            barH);
        shape.end();
    }


    public float getScrollbarX(float w) {
        return w - ScreenState.SCROLLBAR_W
            - 8f * Gdx.graphics.getDensity();
    }

    public float getScrollbarHeight(float h) {

        float totalH = state.getTotalContentHeight();
        float visibleH = state.getVisibleHeight(h);

        float ratio = visibleH / totalH;

        return Math.max(
            40f * Gdx.graphics.getDensity(),
            visibleH * ratio
        );
    }

    public float getScrollbarY(float h) {

        float visibleH = state.getVisibleHeight(h);

        float barH = getScrollbarHeight(h);
        float travel = visibleH - barH;

        float ratio = state.maxScroll > 0
            ? state.scrollY / state.maxScroll
            : 0;

        return ScreenState.BOTTOM_BAR_H
            + travel
            - ratio * travel;
    }

    private void drawConfirmPopup(float w, float h) {
        float dp  = Gdx.graphics.getDensity();
        float pw  = 310f * dp;
        float ph  = 160f * dp;
        float px  = (w - pw) / 2f;
        float py  = (h - ph) / 2f;
        float pad = 8f * dp;
        float btnW = (pw - 3 * pad) / 2f;
        float btnH = 44f * dp;
        float btnY = py + pad;

        // dim background
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shape.begin(ShapeRenderer.ShapeType.Filled);
        shape.setColor(0f, 0f, 0f, 0.55f);
        shape.rect(0, 0, w, h);
        // popup card
        shape.setColor(0.15f, 0.15f, 0.22f, 1f);
        shape.rect(px, py, pw, ph);
        // cancel button
        shape.setColor(0.25f, 0.25f, 0.35f, 1f);
        shape.rect(px + pad, btnY, btnW, btnH);
        // confirm button
        shape.setColor(0.7f, 0.15f, 0.15f, 1f);
        shape.rect(px + btnW + 2 * pad, btnY, btnW, btnH);
        shape.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // text
        batch.begin();
        fonts.body.setColor(Color.WHITE);
        fonts.body.draw(batch,
            "Remove \"" + state.pendingRemove.getName() + "\"?",
            px + pad, py + ph - pad*3f, pw - pad * 2, 1, true);
        fonts.small.setColor(Color.WHITE);
        fonts.small.draw(batch, "Cancel",
            px + pad, btnY + btnH / 2f + 6f * dp, btnW, 1, false);
        fonts.small.draw(batch, "Remove",
            px + btnW + 2 * pad, btnY + btnH / 2f + 6f * dp, btnW, 1, false);
        batch.end();
    }

    public void dispose() {
        bgTexture.dispose();
        doneTexture.dispose();
        notDoneTexture.dispose();
        barTexture.dispose();
        catHeadTex.dispose();
        ghostRowTex.dispose();
        rowTex.dispose();
        btnTex.dispose();
        searchIcon.dispose();
        visibleTexture.dispose();
        notAllVisibleTexture.dispose();

        rowPatch.getTexture().dispose();
        catHeadPatch.getTexture().dispose();
        ghostRowPatch.getTexture().dispose();



    }

}
