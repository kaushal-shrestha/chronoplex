package com.zoneanchor;

import android.content.Context;
import android.content.SharedPreferences;

public final class SettingsStore {
    public static final String APPEARANCE_SYSTEM = "system";
    public static final String APPEARANCE_LIGHT = "light";
    public static final String APPEARANCE_DARK = "dark";

    private static final String PREFS = "zoneanchor_settings";
    private static final String KEY_THEME_ID = "theme_id";
    private static final String KEY_APPEARANCE = "appearance";

    private SettingsStore() {
    }

    public static AppColorTheme loadTheme(Context context) {
        String themeId = prefs(context).getString(KEY_THEME_ID, AppColorTheme.defaultTheme().id);
        return AppColorTheme.byId(themeId);
    }

    public static void saveTheme(Context context, AppColorTheme theme) {
        prefs(context).edit().putString(KEY_THEME_ID, theme.id).apply();
    }

    public static String loadAppearance(Context context) {
        String mode = prefs(context).getString(KEY_APPEARANCE, APPEARANCE_SYSTEM);
        if (APPEARANCE_LIGHT.equals(mode) || APPEARANCE_DARK.equals(mode)) {
            return mode;
        }
        return APPEARANCE_SYSTEM;
    }

    public static void saveAppearance(Context context, String mode) {
        if (!APPEARANCE_LIGHT.equals(mode) && !APPEARANCE_DARK.equals(mode)) {
            mode = APPEARANCE_SYSTEM;
        }
        prefs(context).edit().putString(KEY_APPEARANCE, mode).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
