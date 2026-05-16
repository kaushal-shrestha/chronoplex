package com.zoneanchor;

public final class ZonedAlarm {
    public static final int FIRST_ID = 400;
    public static final int DEFAULT_ID = FIRST_ID;
    public static final String DEFAULT_ZONE_ID = "America/New_York";
    public static final int DEFAULT_HOUR = 16;
    public static final int DEFAULT_MINUTE = 0;
    public static final int ALL_DAYS = 0b1111111;
    public static final String SOUND_DEFAULT = "default";
    public static final String SOUND_SILENT = "silent";

    public final int id;
    public final String label;
    public final String zoneId;
    public final int hour;
    public final int minute;
    public final int daysOfWeekMask;
    public final String soundMode;
    public final boolean vibrate;
    public final boolean enabled;
    public final long nextTriggerAtMillis;

    public ZonedAlarm(
            int id,
            String label,
            String zoneId,
            int hour,
            int minute,
            int daysOfWeekMask,
            String soundMode,
            boolean vibrate,
            boolean enabled,
            long nextTriggerAtMillis
    ) {
        this.id = id;
        this.label = cleanLabel(label);
        this.zoneId = zoneId;
        this.hour = hour;
        this.minute = minute;
        this.daysOfWeekMask = daysOfWeekMask == 0 ? ALL_DAYS : daysOfWeekMask;
        this.soundMode = SOUND_SILENT.equals(soundMode) ? SOUND_SILENT : SOUND_DEFAULT;
        this.vibrate = vibrate;
        this.enabled = enabled;
        this.nextTriggerAtMillis = nextTriggerAtMillis;
    }

    public static ZonedAlarm defaultAlarm() {
        return new ZonedAlarm(
                DEFAULT_ID,
                "",
                DEFAULT_ZONE_ID,
                DEFAULT_HOUR,
                DEFAULT_MINUTE,
                ALL_DAYS,
                SOUND_DEFAULT,
                true,
                false,
                0L
        );
    }

    public ZonedAlarm enabledWithNextTrigger(long nextTriggerAtMillis) {
        return new ZonedAlarm(
                id,
                label,
                zoneId,
                hour,
                minute,
                daysOfWeekMask,
                soundMode,
                vibrate,
                true,
                nextTriggerAtMillis
        );
    }

    public ZonedAlarm disabled() {
        return new ZonedAlarm(
                id,
                label,
                zoneId,
                hour,
                minute,
                daysOfWeekMask,
                soundMode,
                vibrate,
                false,
                0L
        );
    }

    public String displayName() {
        return label.isEmpty() ? zoneId : label;
    }

    private static String cleanLabel(String value) {
        return value == null ? "" : value.trim();
    }
}
