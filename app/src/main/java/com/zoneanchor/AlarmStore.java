package com.zoneanchor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

public final class AlarmStore {
    private static final String PREFS = "zoneanchor_alarm";
    private static final String KEY_ALARMS_JSON = "alarms_json";
    private static final String KEY_NEXT_ID = "next_alarm_id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_ZONE_ID = "zone_id";
    private static final String KEY_HOUR = "hour";
    private static final String KEY_MINUTE = "minute";
    private static final String KEY_DAYS = "days";
    private static final String KEY_SOUND_MODE = "sound_mode";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_NEXT_TRIGGER = "next_trigger";

    private AlarmStore() {
    }

    public static void save(Context context, ZonedAlarm alarm) {
        ArrayList<ZonedAlarm> alarms = loadAll(context);
        boolean replaced = false;
        for (int i = 0; i < alarms.size(); i++) {
            if (alarms.get(i).id == alarm.id) {
                alarms.set(i, alarm);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            alarms.add(alarm);
        }
        saveAll(context, alarms);
    }

    public static ZonedAlarm load(Context context, int alarmId) {
        ArrayList<ZonedAlarm> alarms = loadAll(context);
        for (ZonedAlarm alarm : alarms) {
            if (alarm.id == alarmId) {
                return alarm;
            }
        }
        return null;
    }

    public static ArrayList<ZonedAlarm> loadAll(Context context) {
        SharedPreferences prefs = prefs(context);
        String json = prefs.getString(KEY_ALARMS_JSON, null);
        if (json != null) {
            return parseAlarms(json);
        }

        ArrayList<ZonedAlarm> alarms = new ArrayList<>();
        ZonedAlarm legacyAlarm = loadLegacyAlarm(prefs);
        if (legacyAlarm != null) {
            alarms.add(legacyAlarm);
        }
        return alarms;
    }

    public static int nextId(Context context) {
        SharedPreferences prefs = prefs(context);
        int nextId = prefs.getInt(KEY_NEXT_ID, maxAlarmId(loadAll(context)) + 1);
        if (nextId < ZonedAlarm.FIRST_ID) {
            nextId = ZonedAlarm.FIRST_ID;
        }
        prefs.edit().putInt(KEY_NEXT_ID, nextId + 1).apply();
        return nextId;
    }

    public static void remove(Context context, int alarmId) {
        ArrayList<ZonedAlarm> alarms = loadAll(context);
        for (int i = alarms.size() - 1; i >= 0; i--) {
            if (alarms.get(i).id == alarmId) {
                alarms.remove(i);
            }
        }
        saveAll(context, alarms);
    }

    public static void saveAll(Context context, List<ZonedAlarm> alarms) {
        JSONArray array = new JSONArray();
        for (ZonedAlarm alarm : alarms) {
            JSONObject object = new JSONObject();
            try {
                object.put("id", alarm.id);
                object.put(KEY_LABEL, alarm.label);
                object.put(KEY_ZONE_ID, alarm.zoneId);
                object.put(KEY_HOUR, alarm.hour);
                object.put(KEY_MINUTE, alarm.minute);
                object.put(KEY_DAYS, alarm.daysOfWeekMask);
                object.put(KEY_SOUND_MODE, alarm.soundMode);
                object.put(KEY_VIBRATE, alarm.vibrate);
                object.put(KEY_ENABLED, alarm.enabled);
                object.put(KEY_NEXT_TRIGGER, alarm.nextTriggerAtMillis);
                array.put(object);
            } catch (JSONException ignored) {
            }
        }

        int nextId = Math.max(
                prefs(context).getInt(KEY_NEXT_ID, ZonedAlarm.FIRST_ID),
                maxAlarmId(alarms) + 1
        );
        prefs(context).edit()
                .putString(KEY_ALARMS_JSON, array.toString())
                .putInt(KEY_NEXT_ID, nextId)
                .remove(KEY_ZONE_ID)
                .remove(KEY_HOUR)
                .remove(KEY_MINUTE)
                .remove(KEY_DAYS)
                .remove(KEY_SOUND_MODE)
                .remove(KEY_VIBRATE)
                .remove(KEY_ENABLED)
                .remove(KEY_NEXT_TRIGGER)
                .apply();
    }

    public static void clearAll(Context context) {
        prefs(context).edit().clear().apply();
    }

    private static ZonedAlarm loadLegacyAlarm(SharedPreferences prefs) {
        if (!prefs.contains(KEY_ZONE_ID)) {
            return null;
        }
        String zoneId = prefs.getString(KEY_ZONE_ID, ZonedAlarm.DEFAULT_ZONE_ID);
        if (!isKnownZone(zoneId)) {
            zoneId = ZonedAlarm.DEFAULT_ZONE_ID;
        }

        return new ZonedAlarm(
                ZonedAlarm.DEFAULT_ID,
                "",
                zoneId,
                prefs.getInt(KEY_HOUR, ZonedAlarm.DEFAULT_HOUR),
                prefs.getInt(KEY_MINUTE, ZonedAlarm.DEFAULT_MINUTE),
                ZonedAlarm.ALL_DAYS,
                ZonedAlarm.SOUND_DEFAULT,
                true,
                prefs.getBoolean(KEY_ENABLED, false),
                prefs.getLong(KEY_NEXT_TRIGGER, 0L)
        );
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static ArrayList<ZonedAlarm> parseAlarms(String json) {
        ArrayList<ZonedAlarm> alarms = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) {
                    continue;
                }
                ZonedAlarm alarm = parseAlarm(object);
                if (alarm != null) {
                    alarms.add(alarm);
                }
            }
        } catch (JSONException ignored) {
        }
        return alarms;
    }

    private static ZonedAlarm parseAlarm(JSONObject object) {
        String zoneId = object.optString(KEY_ZONE_ID, ZonedAlarm.DEFAULT_ZONE_ID);
        if (!isKnownZone(zoneId)) {
            zoneId = ZonedAlarm.DEFAULT_ZONE_ID;
        }

        int hour = clamp(object.optInt(KEY_HOUR, ZonedAlarm.DEFAULT_HOUR), 0, 23);
        int minute = clamp(object.optInt(KEY_MINUTE, ZonedAlarm.DEFAULT_MINUTE), 0, 59);
        int id = object.optInt("id", ZonedAlarm.DEFAULT_ID);
        if (id < ZonedAlarm.FIRST_ID) {
            id = ZonedAlarm.FIRST_ID;
        }
        int daysOfWeekMask = object.optInt(KEY_DAYS, ZonedAlarm.ALL_DAYS) & ZonedAlarm.ALL_DAYS;
        if (daysOfWeekMask == 0) {
            daysOfWeekMask = ZonedAlarm.ALL_DAYS;
        }
        String soundMode = object.optString(KEY_SOUND_MODE, ZonedAlarm.SOUND_DEFAULT);

        return new ZonedAlarm(
                id,
                object.optString(KEY_LABEL, ""),
                zoneId,
                hour,
                minute,
                daysOfWeekMask,
                soundMode,
                object.optBoolean(KEY_VIBRATE, true),
                object.optBoolean(KEY_ENABLED, false),
                object.optLong(KEY_NEXT_TRIGGER, 0L)
        );
    }

    private static int maxAlarmId(List<ZonedAlarm> alarms) {
        int max = ZonedAlarm.FIRST_ID - 1;
        for (ZonedAlarm alarm : alarms) {
            if (alarm.id > max) {
                max = alarm.id;
            }
        }
        return max;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static boolean isKnownZone(String zoneId) {
        try {
            ZoneId.of(zoneId);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
