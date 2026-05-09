package at.tool.shoppinglist;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;

public class ScreenFonts {
    public final BitmapFont title;
    public final BitmapFont body;
    public final BitmapFont small;

    public ScreenFonts() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/DMSans-Regular.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();

        float dp = Gdx.graphics.getDensity();

        p.characters += "äöüÄÖÜßéàâ";
        p.color = ScreenColors.TEXT_PRI;

        p.size = (int)(30 * dp);
        title = gen.generateFont(p);

        p.size = (int)(15 * dp);
        p.color = Color.WHITE;
        body = gen.generateFont(p);

        p.size = (int)(12 * dp);
        p.color = ScreenColors.TEXT_SEC;
        small = gen.generateFont(p);

        gen.dispose();
    }

    public void dispose() {
        title.dispose();
        body.dispose();
        small.dispose();
    }
}
