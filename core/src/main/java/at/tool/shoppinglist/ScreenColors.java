package at.tool.shoppinglist;


import com.badlogic.gdx.graphics.Color;


public class ScreenColors {
    public static final Color BG = hex(0x1a1a18ff);
    public static final Color SURFACE = hex(0x3d3d33ff);
    public static final Color RED = new Color(0.251f,0.016f,0f,1f);
    public static final Color BLUE   = new Color(0,0,139,1);
    public static final Color TEXT_DARK = hex(0x1a1a18ff);
    public static final Color TEXT_WHITE = new Color(255,255,255,1);

    public static Color hex(long rgba) {
        float r = ((rgba >> 24) & 0xFF) / 255f;
        float g = ((rgba >> 16) & 0xFF) / 255f;
        float b = ((rgba >>  8) & 0xFF) / 255f;
        float a = (rgba & 0xFF) / 255f;
        return new Color(r, g, b, a);
    }
}
