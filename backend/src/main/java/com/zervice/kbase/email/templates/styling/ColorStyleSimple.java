package com.zervice.kbase.email.templates.styling;

import lombok.Getter;

@Getter
public class ColorStyleSimple implements ColorStyle {

    public static ColorStyleSimple BASE_STYLE = new ColorStyleSimple(WHITE, BLUE);
    public static ColorStyleSimple WARNING_STYLE = new ColorStyleSimple(WHITE, YELLOW);

    public static ColorStyleSimple BLUE_STYLE = new ColorStyleSimple(WHITE, BLUE);
    public static ColorStyleSimple GREEN_STYLE = new ColorStyleSimple(WHITE, GREEN);
    public static ColorStyleSimple RED_STYLE = new ColorStyleSimple(WHITE, RED);
    public static ColorStyleSimple YELLOW_STYLE = new ColorStyleSimple(BLACK, YELLOW);
    public static ColorStyleSimple BLACK_STYLE = new ColorStyleSimple(WHITE, DARK);
    public static ColorStyleSimple GRAY_STYLE = new ColorStyleSimple(BLACK, GRAY);

    private final String _text;
    private final String _bg;

    /**
     * removes automatically leading #
     */
    public ColorStyleSimple(String text, String bg) {
        this._text = text;
        this._bg = bg;
    }

    public ColorStyleSimple(ColorPalette colorPalette) {
        this._text = colorPalette.isBlackContrastingColor() ? BLACK : WHITE;
        this._bg = "#" + colorPalette.getHexCode();
    }

    /**
     * specify background color - code detect automatically the text-color (black/white)<br>
     * format could be #fff, ffffff, rgb(255,255,255)
     */
    public ColorStyleSimple(String bg) {
        RgbColor color = RgbColor.readRgbOrHex(bg);
        if (color == null) {
            throw new RuntimeException("invalid bg value!");
        }
        this._text = color.isBlackContrastingColor() ? BLACK : WHITE;
        this._bg = "#" + color.getHexCode();
    }
}
