# ZoneAnchor Alarm

ZoneAnchor Alarm is a native Android MVP for clocks and alarms pinned to chosen time zones.

The core behavior is intentionally different from a normal phone alarm:

- Add the time zones you care about on the Clocks tab, with optional custom labels.
- Pick a wall-clock time, such as `4:00 PM`.
- Pick the zone that owns that time, such as `America/New_York`.
- Add the alarm from the Alarms tab, either from your saved clock zones or the full time-zone list.
- Edit saved clocks and alarms after creating them.
- Give alarms custom names, selected weekdays, sound mode, and vibration mode.
- The app stores each alarm and schedules the next selected day where that local time occurs in that zone.
- Pick Follow System, Light Mode, or Dark Mode plus an app-wide visual theme from the Settings tab.
- If the phone travels to another country or the device time zone changes, the alarm remains locked to the selected zone.

Example: an alarm set for `4:00 PM America/New_York` fires when New York reaches 4:00 PM. If the device is in London that day, the phone will alert at the equivalent London time.

## Build

Open this folder in Android Studio, or run:

```sh
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
./gradlew assembleDebug
```

The app uses platform Android APIs only: `AlarmManager`, `SharedPreferences`, Java time, and notifications. No AndroidX dependency is required for this first version.

## Android Permissions

The app declares:

- `USE_EXACT_ALARM` for precise alarm scheduling.
- `POST_NOTIFICATIONS` for Android 13+ notification delivery.
- `RECEIVE_BOOT_COMPLETED` so saved alarms are restored after reboot.

`AlarmManager.setAlarmClock` is used for the scheduled alert because this is a user-visible alarm experience and should remain precise.
