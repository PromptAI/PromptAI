package com.zervice.kbase.email.templates.styling;

import java.util.*;

public enum ColorPalette {
    BRICK("a54e3c"),
    POMEGRANTE("881f30"),
    CABERNET("721432"),
    AUBERGINE("5b1946"),
    INDIGO("461c5d"),
    PLUM("5c145e"),
    ROYAL("97348a"),
    FRENCH_BLUE("3266ad"),
    TEAL("337b8d"),
    AQUAMARINE("22586f"),
    MARINE("18456b"),
    NAVY("0e2e57"),
    LILAC("ae9cc7"),
    ORCHID("985a9c"),
    COBALT("4099d4"),
    POOL("9cd6f5"),
    PERIWINKLE("a7b4d0"),
    DOVE("aec4d0"),
    DUCK_EGG("97babf"),
    TURQUOISE("6fbabf"),
    JADE("73aba0"),
    TIFFANY("a5d2c1"),
    MINT("c8e0c4"),
    SPRING("d3e2a3"),
    CHIVE("b8d26e"),
    TREE_TOP("759150"),
    BOTTLE("378159"),
    EMERALD("29634e"),
    LEMON("fef6a6"),
    SUNSHINE("fcf151"),
    TUMERIC("f9d549"),
    CLEMENTINE("f3b143"),
    PEACH("f7d19d"),
    SALMON("f0b79f"),
    CORAL("e89487"),
    WATERMELON("e3708e"),
    RASBERRY("ac2c69"),
    MAGENTA("db318a"),
    RED_BALLOON("db3656"),
    FIRE_ENGINE("db3732"),
    BLOOD_ORANGE("e06151"),
    PAPAYA("e88b5b"),
    ROSE("e6aab3"),
    CHERRY_BLOSSOM("f6d6dc"),
    BLUSH("f6d6d1"),
    FRENCH_VANILLA("f8f1de"),
    IVORY("fbf8da"),
    CLAY("e8d4b8"),
    SANDSTONE("c4b199"),
    SAGE("a5b4a3"),
    MOSS("a2a495"),
    OLIVE("7b7d6a"),
    SMOKE("63605c"),
    ESPRESSO("473937"),
    CHOCOLATE("684940"),
    CARAMEL("957d62"),
    BLACK("221f20"),
    CHARCOAL("58585a"),
    SLATE("949599"),
    SILVER_LINING("d1d2d4"),
    OYSTER("e3ddd4"),
    STONE("d2cfc9"),
    SMOG("928b88"),
    WHITE("ffffff");

    private String _hexCode;

    private ColorPalette(String hexCode) {
        this._hexCode = hexCode;
    }

    public static ColorPalette getNearest(String hexCode) {
        RgbColor given = RgbColor.hex2rgb(hexCode);
        if (given == null) {
            return null;
        } else {
            List<Double> differenceArray = new ArrayList();
            Arrays.asList(values()).forEach((color) -> {
                RgbColor c = color.getRgbColor();
                differenceArray.add(Math.sqrt((double)((given.getR() - c.getR()) * (given.getR() - c.getR()) + (given.getG() - c.getG()) * (given.getG() - c.getG()) + (given.getB() - c.getB()) * (given.getB() - c.getB()))));
            });
            Double min = (Double)Collections.min(differenceArray);
            int index = differenceArray.indexOf(min);
            return values()[index];
        }
    }

    public static ColorPalette getRandomValue() {
        return values()[(new Random()).nextInt(values().length)];
    }

    public static ColorPalette getRandomValue(Collection<ColorPalette> existingCollection) {
        ColorPalette newValue = null;
        if (existingCollection != null && existingCollection.size() < values().length) {
            do {
                newValue = getRandomValue();
            } while(!existingCollection.contains(newValue));

            return newValue;
        } else {
            return null;
        }
    }

    public RgbColor getRgbColor() {
        return RgbColor.hex2rgb(this.getHexCode());
    }

    public boolean isBlackContrastingColor() {
        return this.getRgbColor().isBlackContrastingColor();
    }

    public String getHexCode() {
        return this._hexCode;
    }
}
