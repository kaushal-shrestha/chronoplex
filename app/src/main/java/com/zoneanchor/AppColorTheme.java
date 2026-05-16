package com.zoneanchor;

import android.graphics.Color;

public final class AppColorTheme {
    public final String id;
    public final String name;
    public final String description;
    public final int backgroundColor;
    public final int surfaceColor;
    public final int primaryTextColor;
    public final int secondaryTextColor;
    public final int accentColor;
    public final int accentTextColor;
    public final int borderColor;
    public final int tabSelectedColor;
    public final int tabSelectedTextColor;
    public final int tabUnselectedColor;
    public final int tabUnselectedTextColor;
    public final boolean lightSystemBars;

    private static final AppColorTheme[] THEMES = new AppColorTheme[]{
            new AppColorTheme(
                    "daybreak",
                    "Daybreak",
                    "Clean paper, deep ink, and calm teal.",
                    Color.rgb(247, 249, 252),
                    Color.WHITE,
                    Color.rgb(15, 23, 42),
                    Color.rgb(67, 81, 104),
                    Color.rgb(0, 108, 103),
                    Color.WHITE,
                    Color.rgb(225, 231, 239),
                    Color.rgb(15, 23, 42),
                    Color.WHITE,
                    Color.WHITE,
                    Color.rgb(15, 23, 42),
                    true
            ),
            new AppColorTheme(
                    "harbor",
                    "Harbor",
                    "Soft blue-gray with a crisp marine accent.",
                    Color.rgb(239, 246, 250),
                    Color.rgb(253, 254, 255),
                    Color.rgb(22, 37, 54),
                    Color.rgb(74, 91, 108),
                    Color.rgb(0, 91, 140),
                    Color.WHITE,
                    Color.rgb(207, 224, 234),
                    Color.rgb(22, 65, 94),
                    Color.WHITE,
                    Color.rgb(253, 254, 255),
                    Color.rgb(22, 37, 54),
                    true
            ),
            new AppColorTheme(
                    "grove",
                    "Grove",
                    "A green workspace with warm ivory surfaces.",
                    Color.rgb(241, 247, 241),
                    Color.rgb(255, 253, 247),
                    Color.rgb(27, 45, 35),
                    Color.rgb(80, 96, 84),
                    Color.rgb(35, 112, 70),
                    Color.WHITE,
                    Color.rgb(212, 226, 213),
                    Color.rgb(27, 75, 48),
                    Color.WHITE,
                    Color.rgb(255, 253, 247),
                    Color.rgb(27, 45, 35),
                    true
            ),
            new AppColorTheme(
                    "ember",
                    "Ember",
                    "Warm rose accents on a quiet neutral base.",
                    Color.rgb(250, 246, 244),
                    Color.rgb(255, 255, 255),
                    Color.rgb(48, 35, 38),
                    Color.rgb(104, 82, 87),
                    Color.rgb(173, 69, 67),
                    Color.WHITE,
                    Color.rgb(234, 219, 216),
                    Color.rgb(92, 50, 52),
                    Color.WHITE,
                    Color.rgb(255, 255, 255),
                    Color.rgb(48, 35, 38),
                    true
            ),
            new AppColorTheme(
                    "twilight",
                    "Twilight",
                    "Cool slate tones with a bright evening accent.",
                    Color.rgb(238, 242, 248),
                    Color.rgb(252, 253, 255),
                    Color.rgb(31, 41, 55),
                    Color.rgb(82, 93, 110),
                    Color.rgb(92, 91, 176),
                    Color.WHITE,
                    Color.rgb(215, 222, 235),
                    Color.rgb(57, 60, 124),
                    Color.WHITE,
                    Color.rgb(252, 253, 255),
                    Color.rgb(31, 41, 55),
                    true
            )
    };

    private AppColorTheme(
            String id,
            String name,
            String description,
            int backgroundColor,
            int surfaceColor,
            int primaryTextColor,
            int secondaryTextColor,
            int accentColor,
            int accentTextColor,
            int borderColor,
            int tabSelectedColor,
            int tabSelectedTextColor,
            int tabUnselectedColor,
            int tabUnselectedTextColor,
            boolean lightSystemBars
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.backgroundColor = backgroundColor;
        this.surfaceColor = surfaceColor;
        this.primaryTextColor = primaryTextColor;
        this.secondaryTextColor = secondaryTextColor;
        this.accentColor = accentColor;
        this.accentTextColor = accentTextColor;
        this.borderColor = borderColor;
        this.tabSelectedColor = tabSelectedColor;
        this.tabSelectedTextColor = tabSelectedTextColor;
        this.tabUnselectedColor = tabUnselectedColor;
        this.tabUnselectedTextColor = tabUnselectedTextColor;
        this.lightSystemBars = lightSystemBars;
    }

    public static AppColorTheme defaultTheme() {
        return THEMES[0];
    }

    public static AppColorTheme[] all() {
        return THEMES.clone();
    }

    public static AppColorTheme byId(String id) {
        for (AppColorTheme theme : THEMES) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return defaultTheme();
    }

    public AppColorTheme resolve(boolean dark) {
        if (!dark) {
            return this;
        }

        int accent = darkAccentColor();
        return new AppColorTheme(
                id,
                name,
                description,
                Color.rgb(15, 23, 42),
                Color.rgb(30, 41, 59),
                Color.rgb(248, 250, 252),
                Color.rgb(203, 213, 225),
                accent,
                Color.rgb(8, 15, 28),
                Color.rgb(71, 85, 105),
                accent,
                Color.rgb(8, 15, 28),
                Color.rgb(30, 41, 59),
                Color.rgb(248, 250, 252),
                false
        );
    }

    private int darkAccentColor() {
        if ("harbor".equals(id)) {
            return Color.rgb(96, 165, 250);
        }
        if ("grove".equals(id)) {
            return Color.rgb(74, 222, 128);
        }
        if ("ember".equals(id)) {
            return Color.rgb(251, 113, 133);
        }
        if ("twilight".equals(id)) {
            return Color.rgb(167, 139, 250);
        }
        return Color.rgb(45, 212, 191);
    }
}
