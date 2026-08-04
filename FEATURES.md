# ZoneAnchor - Implemented Feature Catalog

This file describes what is currently implemented in ZoneAnchor. It is meant to
be a quick product reference and a lightweight QA checklist.

App version: **0.1.0**
Min SDK: **26**
Target SDK: **36**

## Product Shape

ZoneAnchor is a timezone-aware time-management app. Its core idea is that a
clock can be a named reference point - a city, team, office, family member,
trip, or routine - and alarms can be scheduled against that reference clock.

The app currently has five primary tabs:

- [x] Clocks
- [x] Alarms
- [x] Timers
- [x] Stopwatch
- [x] Settings

## Clocks

### Device Time

- [x] Shows the device's current time.
- [x] Shows weekday, date, and system time zone.
- [x] Updates live.

### World Clocks

- [x] Add multiple clocks for IANA time zones.
- [x] Give each clock a custom label.
- [x] Use a fallback label derived from the time zone when the label is blank.
- [x] Edit existing clocks.
- [x] Delete individual clocks.
- [x] Clear all clocks with confirmation.
- [x] Reset to suggested clocks.
- [x] Group clocks when grouping is enabled.
- [x] Reorder clocks with drag handles.
- [x] Reorder clock groups.

### Time Converter

- [x] Show converted times across saved clocks.
- [x] Use a saved clock as the conversion reference.
- [x] Jump relative time with quick controls such as previous hour, next hour,
      next day, yesterday, today, and tomorrow.
- [x] Select converter result rows to make that clock the reference.

## Alarms

### Timezone-Anchored Alarms

- [x] Create alarms locked to a selected IANA time zone.
- [x] Preserve alarm wall-clock behavior in the selected zone, even when the
      device time zone changes.
- [x] Choose the zone picker source per alarm: My clocks or All zones.
- [x] Configure the default zone picker source in Settings.
- [x] Add a custom alarm label.
- [x] Enable or disable sound.
- [x] Enable or disable vibration.
- [x] Toggle alarms on or off from the alarm list.
- [x] Delete alarms from the long-press actions menu.
- [x] Clear all alarms with confirmation.

### Associated Clock Labels

- [x] Associate an alarm with a saved world clock label.
- [x] Auto-fill the alarm time zone from the associated clock.
- [x] Disable manual zone selection while a clock association is active.
- [x] Let Settings choose whether alarm cards show the associated clock label
      or the raw zone ID.
- [x] Keep alarms functional if the associated clock is deleted by detaching
      the alarm and falling back to the saved zone ID.
- [x] Warn before deleting a clock that has associated alarms.
- [x] Warn before changing the time zone of a clock that has associated alarms.
- [x] Detach affected alarms when a clock's time zone changes.
- [x] Preserve clock-label changes for associated alarms when only the label
      changes.

### Repeat Rules

- [x] One-time alarms.
- [x] Weekly alarms with any combination of weekdays.
- [x] Presets for weekdays, weekends, and every day.
- [x] Every N weeks.
- [x] Monthly by day of month.
- [x] Monthly by ordinal weekday, such as first Sunday or last Monday.
- [x] Every N months from a start date.
- [x] Natural repeat labels on alarm cards, such as "Repeats every Friday".

### Next Fire Display And Ordering

- [x] Sort enabled alarms by their actual next ring instant instead of simple
      face-value time.
- [x] Keep disabled alarms after enabled alarms.
- [x] Show next fire in the alarm's reference clock time.
- [x] Show next fire in the device time.
- [x] Show remaining duration until the next fire.
- [x] Respect snoozed alarms in the next-fire display and sort order.

### Alarm Ringing

- [x] Schedule Android alarm-clock alarms with `setAlarmClock` when exact alarm
      permission is available.
- [x] Use an inexact fallback when exact alarm permission is unavailable.
- [x] Launch a full-screen alarm activity over the lock screen.
- [x] Turn the screen on for ringing alarms.
- [x] Play the system alarm tone on a loop when sound is enabled.
- [x] Vibrate on a repeating waveform when vibration is enabled.
- [x] Prevent Back from dismissing the ringing screen.
- [x] Dismiss alarms.
- [x] Snooze alarms for 1, 5, or 10 minutes.
- [x] Show a persistent snoozed-alarm notification.
- [x] Cancel snooze from the alarm list.

## Timers

- [x] Add timers.
- [x] Edit timers.
- [x] Delete timers with undo.
- [x] Clear all timers with confirmation.
- [x] Start, pause, resume, stop, and reset timers.
- [x] Add one minute to a timer.
- [x] Use quick presets such as 1m, 5m, 10m, 15m, 30m, and 1h.
- [x] Choose timer finish behavior: notification or full-screen.
- [x] Configure the default timer finish behavior in Settings.
- [x] Group timers when grouping is enabled.
- [x] Reorder timers with drag handles.
- [x] Reorder timer groups.

## Stopwatch

- [x] Add multiple stopwatches.
- [x] Start, pause, resume, and reset stopwatches.
- [x] Record laps.
- [x] Rename stopwatches.
- [x] Delete stopwatches with undo, including their laps.
- [x] Clear all stopwatches with confirmation.
- [x] Group stopwatches when grouping is enabled.
- [x] Reorder stopwatches with drag handles.
- [x] Reorder stopwatch groups.

## Groups And Organization

- [x] Optional grouping for clocks.
- [x] Optional grouping for alarms.
- [x] Optional grouping for timers.
- [x] Optional grouping for stopwatches.
- [x] Create, rename, delete, collapse, and reorder groups.
- [x] Move items into groups.
- [x] Move items back to Ungrouped.
- [x] Deleting a group preserves its items and moves them to Ungrouped.

## Settings

- [x] Appearance mode: Follow system, Light, Dark.
- [x] Theme palettes: Anchor, Daybreak, Harbor, Grove, Ember, Twilight,
      Sunrise, Forest, Slate, Plum.
- [x] Material You dynamic color through the Anchor palette on Android 12+.
- [x] Default zone source for new alarms.
- [x] Alarm zone display: Clock label or Zone ID.
- [x] First day of week: Monday or Sunday.
- [x] Grouping toggles for clocks, alarms, timers, and stopwatches.
- [x] About dialog with app name, version, description, and copyright.

## System Integration

- [x] Requests notification permission where required.
- [x] Surfaces notification-permission problems in the alarm readiness UI.
- [x] Surfaces exact-alarm permission problems in the alarm readiness UI.
- [x] Opens system settings for exact alarm or notification fixes.
- [x] Reschedules alarms after boot.
- [x] Reschedules alarms after package replacement.
- [x] Reschedules alarms after system time changes.
- [x] Reschedules alarms after system time zone changes.
- [x] Reschedules alarms after exact-alarm permission changes.
- [x] Populates Android's system next-alarm chip when possible.

## Persistence

- [x] Room persistence for clocks, alarms, timers, stopwatches, laps, and groups.
- [x] DataStore persistence for app settings.
- [x] Repository-level validation for labels, time zones, alarm time fields,
      repeat fields, and day masks.
- [x] Schema migrations for added app features.

## Correctness Coverage

- [x] Unit tests for alarm next-trigger calculations.
- [x] DST spring-forward gap handling.
- [x] DST fall-back ambiguity handling.
- [x] One-shot alarm semantics.
- [x] Invalid-zone fallback behavior.
- [x] Advanced recurrence scheduling.
- [x] Repository validation tests.
- [x] Group lifecycle tests.
- [x] Associated clock-label detach behavior tests.

Run the core verification suite with:

```sh
cd android
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## Known Product Gaps

These are not implemented yet:

- [ ] Natural-language alarm/reminder creation.
- [ ] City/place search beyond IANA time-zone IDs and simple zone matching.
- [ ] Calendar-style reminder notes, completion state, or history.
- [ ] Backup and restore.
- [ ] Cloud sync.
- [ ] Home-screen widgets.
- [ ] Launcher shortcuts.
- [ ] Per-alarm sound selection, volume, or fade-in.
- [ ] Localized strings beyond English.
- [ ] Public README with screenshots and positioning.
