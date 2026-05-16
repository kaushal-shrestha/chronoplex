package com.zoneanchor;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class AlarmTimeCalculator {
    private AlarmTimeCalculator() {
    }

    public static long nextTriggerMillis(ZoneId zoneId, int hour, int minute, Instant now) {
        return nextTriggerMillis(zoneId, hour, minute, ZonedAlarm.ALL_DAYS, now);
    }

    public static long nextTriggerMillis(
            ZoneId zoneId,
            int hour,
            int minute,
            int daysOfWeekMask,
            Instant now
    ) {
        ZonedDateTime zoneNow = now.atZone(zoneId);
        LocalTime targetTime = LocalTime.of(hour, minute);
        int selectedDays = daysOfWeekMask == 0 ? ZonedAlarm.ALL_DAYS : daysOfWeekMask;

        for (int dayOffset = 0; dayOffset <= 7; dayOffset++) {
            ZonedDateTime candidate = zoneNow
                    .plusDays(dayOffset)
                    .withHour(targetTime.getHour())
                    .withMinute(targetTime.getMinute())
                    .withSecond(0)
                    .withNano(0);
            if (isSelected(candidate.getDayOfWeek(), selectedDays)
                    && candidate.toInstant().isAfter(now)) {
                return candidate.toInstant().toEpochMilli();
            }
        }

        return zoneNow.plusDays(1).toInstant().toEpochMilli();
    }

    private static boolean isSelected(DayOfWeek day, int daysOfWeekMask) {
        return (daysOfWeekMask & (1 << (day.getValue() - 1))) != 0;
    }
}
