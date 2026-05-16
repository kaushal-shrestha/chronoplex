package com.zoneanchor;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ZoneClockStore {
    private static final String PREFS = "zoneanchor_clocks";
    private static final String KEY_ZONES_JSON = "zones_json";
    private static final String KEY_ZONE_ID = "zone_id";
    private static final String KEY_LABEL = "label";

    private ZoneClockStore() {
    }

    public static ArrayList<ClockEntry> load(Context context) {
        String json = prefs(context).getString(KEY_ZONES_JSON, null);
        if (json == null) {
            return defaultZones();
        }

        return parseEntries(json);
    }

    public static void add(Context context, String zoneId, String label) {
        if (!isKnownZone(zoneId)) {
            return;
        }

        ArrayList<ClockEntry> entries = load(context);
        String cleanLabel = cleanLabel(label);
        for (int i = 0; i < entries.size(); i++) {
            ClockEntry entry = entries.get(i);
            if (entry.zoneId.equals(zoneId)) {
                entries.set(i, new ClockEntry(zoneId, cleanLabel));
                save(context, entries);
                return;
            }
        }
        entries.add(new ClockEntry(zoneId, cleanLabel));
        save(context, entries);
    }

    public static void update(Context context, String oldZoneId, String newZoneId, String label) {
        if (!isKnownZone(newZoneId)) {
            return;
        }

        ArrayList<ClockEntry> entries = load(context);
        ArrayList<ClockEntry> updated = new ArrayList<>();
        boolean replaced = false;
        for (ClockEntry entry : entries) {
            if (entry.zoneId.equals(oldZoneId)) {
                if (!containsZone(updated, newZoneId)) {
                    updated.add(new ClockEntry(newZoneId, label));
                }
                replaced = true;
            } else if (!entry.zoneId.equals(newZoneId) && !containsZone(updated, entry.zoneId)) {
                updated.add(entry);
            }
        }
        if (!replaced && !containsZone(updated, newZoneId)) {
            updated.add(new ClockEntry(newZoneId, label));
        }
        save(context, updated);
    }

    public static void remove(Context context, String zoneId) {
        ArrayList<ClockEntry> entries = load(context);
        for (int i = entries.size() - 1; i >= 0; i--) {
            if (entries.get(i).zoneId.equals(zoneId)) {
                entries.remove(i);
            }
        }
        save(context, entries);
    }

    public static void save(Context context, List<ClockEntry> entries) {
        JSONArray array = new JSONArray();
        Set<String> uniqueZoneIds = new LinkedHashSet<>();
        for (ClockEntry entry : entries) {
            if (isKnownZone(entry.zoneId) && uniqueZoneIds.add(entry.zoneId)) {
                JSONObject object = new JSONObject();
                try {
                    object.put(KEY_ZONE_ID, entry.zoneId);
                    object.put(KEY_LABEL, cleanLabel(entry.label));
                    array.put(object);
                } catch (JSONException ignored) {
                }
            }
        }
        prefs(context).edit().putString(KEY_ZONES_JSON, array.toString()).apply();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static ArrayList<ClockEntry> defaultZones() {
        ArrayList<ClockEntry> zones = new ArrayList<>();
        String localZone = ZoneId.systemDefault().getId();
        if (!ZonedAlarm.DEFAULT_ZONE_ID.equals(localZone)) {
            zones.add(new ClockEntry(ZonedAlarm.DEFAULT_ZONE_ID, ""));
        }
        if (!"UTC".equals(localZone)) {
            zones.add(new ClockEntry("UTC", ""));
        }
        if (zones.isEmpty()) {
            zones.add(new ClockEntry("Europe/London", ""));
        }
        return zones;
    }

    private static ArrayList<ClockEntry> parseEntries(String json) {
        ArrayList<ClockEntry> entries = new ArrayList<>();
        Set<String> uniqueZoneIds = new LinkedHashSet<>();
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                Object value = array.opt(i);
                ClockEntry entry = parseEntry(value);
                if (entry != null && uniqueZoneIds.add(entry.zoneId)) {
                    entries.add(entry);
                }
            }
        } catch (JSONException ignored) {
        }
        return entries;
    }

    private static ClockEntry parseEntry(Object value) {
        if (value instanceof JSONObject) {
            JSONObject object = (JSONObject) value;
            String zoneId = object.optString(KEY_ZONE_ID, "");
            if (isKnownZone(zoneId)) {
                return new ClockEntry(zoneId, cleanLabel(object.optString(KEY_LABEL, "")));
            }
        } else if (value instanceof String) {
            String zoneId = (String) value;
            if (isKnownZone(zoneId)) {
                return new ClockEntry(zoneId, "");
            }
        }
        return null;
    }

    private static String cleanLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim();
    }

    private static boolean containsZone(List<ClockEntry> entries, String zoneId) {
        for (ClockEntry entry : entries) {
            if (entry.zoneId.equals(zoneId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isKnownZone(String zoneId) {
        try {
            ZoneId.of(zoneId);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static final class ClockEntry {
        public final String zoneId;
        public final String label;

        public ClockEntry(String zoneId, String label) {
            this.zoneId = zoneId;
            this.label = cleanLabel(label);
        }

        public String displayName() {
            return label.isEmpty() ? zoneId : label;
        }
    }
}
