package com.zervice.kbase.email.templates.styling;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RgbColor {
    protected static final Pattern RGB_PATTERN = Pattern.compile("rgb\\(\\ *([0-9]{1,3})\\ *,\\ *([0-9]{1,3})\\ *,\\ *([0-9]{1,3})\\ *\\)", 2);
    private final int r;
    private final int g;
    private final int b;

    public static RgbColor hex2rgb(String hexCode) {
        if (hexCode == null) {
            return null;
        } else {
            String colour = new String(hexCode);
            if (colour.startsWith("#")) {
                colour = colour.substring(1);
            }

            int r = 0;
            int g = 0;
            int b = 0;

            if (colour.length() == 6) {
                r = Integer.valueOf(colour.substring(0, 1), 16) * 17;
                g = Integer.valueOf(colour.substring(2, 3), 16) * 17;
                b = Integer.valueOf(colour.substring(4, 5), 16) * 17;
            } else {
                if (colour.length() != 3) {
                    return null;
                }

                r = Integer.valueOf(colour.substring(0, 1) + colour.substring(0, 1), 16);
                g = Integer.valueOf(colour.substring(1, 2) + colour.substring(1, 2), 16);
                b = Integer.valueOf(colour.substring(2, 3) + colour.substring(2, 3), 16);
            }

            return new RgbColor(r, g, b);
        }
    }

    public static RgbColor readRgb(String rgbCode) {
        if (rgbCode == null) {
            return null;
        } else {
            Matcher matcher = RGB_PATTERN.matcher(rgbCode);
            return !matcher.matches() ? null : new RgbColor(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
        }
    }

    public static RgbColor readRgbOrHex(String rgbOrHex) {
        RgbColor result = readRgb(rgbOrHex);
        return result != null ? result : hex2rgb(rgbOrHex);
    }

    public String getHexCode() {
        String rHex = Integer.toHexString(this.r);
        String gHex = Integer.toHexString(this.g);
        String bHex = Integer.toHexString(this.b);
        if (rHex.length() < 2) {
            rHex = rHex + rHex;
        }

        if (gHex.length() < 2) {
            gHex = gHex + gHex;
        }

        if (bHex.length() < 2) {
            bHex = bHex + bHex;
        }

        return rHex + gHex + bHex;
    }

    public String getHexCodeWithLeadingHash() {
        return "#" + this.getHexCode();
    }

    public boolean isBlackContrastingColor() {
        return (double)Integer.valueOf(this.getHexCode(), 16) > 1.118481E7D;
    }

    public RgbColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getR() {
        return this.r;
    }

    public int getG() {
        return this.g;
    }

    public int getB() {
        return this.b;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof RgbColor)) {
            return false;
        } else {
            RgbColor other = (RgbColor)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (this.getR() != other.getR()) {
                return false;
            } else if (this.getG() != other.getG()) {
                return false;
            } else {
                return this.getB() == other.getB();
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof RgbColor;
    }

    public int hashCode() {
        int PRIME = 1;
        int result = PRIME * 59 + this.getR();
        result = result * 59 + this.getG();
        result = result * 59 + this.getB();
        return result;
    }
}
