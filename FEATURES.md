# ZoneAnchor Alarm — Feature Catalog

What the app does, organized by area. Each entry has a short description
(release-notes voice) and a checklist for QA so the same document supports
shipping and verification.

App version: **0.1.0** · Min SDK: **26** · Target SDK: **36**

---

## Tabs

A bottom navigation bar with three tabs: **Clocks**, **Alarms**, **Settings**.
The active tab survives device rotation and is preserved across navigation
to child screens (clock/alarm editors, time-zone picker).

- [ ] Launch the app — lands on the Clocks tab
- [ ] Switch to each tab — content renders, FAB only shows on Clocks/Alarms
- [ ] Rotate while on the Alarms tab — still on Alarms after rotation
- [ ] Open an alarm editor, rotate — editor preserves state (label, time, etc.)

---

## Clocks tab

### Device time card

The hero card at the top shows the device's current clock (whatever the
device is set to — the term "Device time" makes it clear this is not the
user's geographic local time if the device's automatic time zone is off).
Shows the time, weekday + date ("Saturday, May 17"), and the zone id.
Updates once per second.

- [ ] Time ticks every second
- [ ] Weekday + date show correctly for the device's current zone
- [ ] The zone id reflects the device's system zone (e.g. `America/Los_Angeles`)
- [ ] Time format respects the system 24-hour / 12-hour setting (h:mm:ss a)

### Multi-zone world clocks

Below the device card, any time-zones the user has added appear as cards in
insertion order. Each card shows the clock's label, zone id, day-of-week +
date in that zone, and the live time.

- [ ] Add a clock for `Asia/Tokyo` — appears below device time, ticks every second
- [ ] Date label reflects whatever day it currently is in Tokyo (may differ
      from the device's date around midnight)
- [ ] Add 5+ clocks — list scrolls smoothly

### Add a clock

The FAB on the Clocks tab opens a clock editor. The editor has a label
field (optional) and a tappable zone tile that opens the zone picker. The
picker pins the device's default zone, `America/New_York`, and `UTC` at the
top when the search box is empty; below that, all canonical zones sorted by
their current UTC offset with a city/region/offset triple per row.

- [ ] Tap FAB → editor opens with no label and zone defaulting to `ZoneId.systemDefault()`
- [ ] Tap "Select time zone" → picker opens with the three pinned zones at top
- [ ] Type "Tokyo" → list filters to matches
- [ ] Pick a zone → returns to editor with that zone selected
- [ ] Save with no label → uses zone's city as label fallback
- [ ] Cancel from editor → no clock added

### Edit a clock

Each clock row exposes an edit pencil. Tapping the row anywhere (other than
the delete icon) also opens the editor. The editor pre-populates with the
clock's current label and zone.

- [ ] Tap the pencil on a clock → editor pre-populates with that clock's data
- [ ] Tap the row body (not the icons) → same editor opens
- [ ] Change label, save → clocks list reflects the new label
- [ ] Change zone, save → clocks list reflects the new zone (time updates)

### Delete a clock

A trash icon on each row immediately removes that clock (no confirm — the
clock can be re-added in seconds, so prompting felt excessive).

- [ ] Tap delete on a clock → it disappears from the list
- [ ] Other clocks remain
- [ ] Alarms anchored to that zone are unaffected

### Clear all clocks

The top-bar overflow menu has a "Clear all clocks" action with a confirm
dialog. The action is disabled when no clocks exist.

- [ ] Tap overflow → "Clear all clocks" visible
- [ ] With zero clocks → menu item is disabled
- [ ] With ≥1 clock → confirm dialog appears before deletion
- [ ] Confirm clear → all clocks removed; alarms unaffected
- [ ] Cancel from the dialog → no change

---

## Alarms tab

### Alarm readiness indicator

A compact card at the top of the Alarms tab summarizes whether the app can
actually fire alarms. When everything's fine, it's a single subdued row
reading "Alarms ready ✓". When something's wrong (exact-alarm permission
denied on API 31+, notifications denied on API 33+), the same card expands
to error-container colors and lists each missing permission with a Grant /
Open button.

- [ ] Fresh install API 33+ — indicator shows "Alarms ready ✓"
- [ ] Deny notifications in system settings — indicator turns red with a row
      for notifications and an "Open" button that lands on app notification settings
- [ ] On API 31/32 only: indicator initially red with "Grant" → tapping
      opens "Alarms & reminders" settings → granting flips the indicator green
      without app restart (see "Exact-alarm permission state changed" below)

### Add an alarm

The FAB opens the alarm editor with the device's current zone pre-selected
and a default time of 7:00. The editor has:
- A Material 3 time picker (12/24-hour follows system setting)
- A zone-source toggle ("My clocks" / "All zones") that determines what the
  zone picker shows
- A label field
- Day-of-week chips (single letter each, in seven equal-width slots so all
  seven fit on one row regardless of phone size)
- Repeat presets: Once, Weekdays, Weekends, Every day
- Sound toggle (default alarm tone vs silent)
- Vibration toggle

- [ ] FAB on Alarms tab → editor opens
- [ ] Time picker accepts entries; respects system 12/24-hour setting
- [ ] Zone-source toggle defaults to whatever the user set in Settings
- [ ] Flipping the toggle and opening the picker shows the correct zone subset
- [ ] Day-of-week chips: tap each one to toggle individually
- [ ] Preset chips (Once, Weekdays, Weekends, Every day) set the mask correctly
- [ ] "Once" + tomorrow's time → fires once and disables itself
- [ ] Save with no zone → save button disabled
- [ ] Save with no label → alarm shows time + zone id

### Edit an alarm

Tap an alarm card to open the editor with that alarm's data populated.

- [ ] Tap an alarm → editor pre-populates with its time, zone, label, days, sound, vibrate
- [ ] Change repeat from "Once" to "Weekdays" → save → list reflects new repeat label

### Snooze options (1 / 5 / 10 min)

When the alarm fires, the full-screen ring activity shows three snooze
choices in a row (`+1 min`, `+5 min`, `+10 min`) and a Dismiss button below.
Choosing a snooze schedules a fresh `setAlarmClock` at `now + that many
minutes` and closes the ring activity.

- [ ] Schedule an alarm 1 min in the future — ring activity appears
- [ ] Tap `+1 min` → activity dismisses; one minute later the alarm rings again
- [ ] Tap `+5 min` → similarly, 5 minutes later
- [ ] Repeated snooze (snooze → ring → snooze again) works
- [ ] Dismiss → alarm goes back to its regular recurring schedule (or, for
      a one-shot alarm, disables itself)

### Toggle / delete alarms from the list

Each alarm row has a Switch on the right (enabled/disabled) and a delete
icon. Toggling off cancels the system alarm but keeps the row so you can
re-enable it. Delete cancels and removes the row.

- [ ] Toggle an alarm off → system status-bar alarm chip clears
- [ ] Toggle on → next-fire text updates; status-bar chip appears
- [ ] Delete → row disappears

### "Next: …" line

Each enabled alarm card shows when it will next fire (in the alarm's zone)
plus a relative duration: e.g. `Fires Wed 7:00 AM (8h 23m)`. Computed
each render from the alarm row — accounts for the day-of-week mask, DST
gaps and fall-back ambiguity.

- [ ] An alarm for tomorrow at 7:00 AM shows the right next-fire row
- [ ] A weekdays-only alarm at 7:00 AM viewed on Saturday morning shows
      Monday, not today

### Full-screen ring experience

When an alarm fires, an opaque full-screen activity launches over the lock
screen (`showWhenLocked` + `turnScreenOn`). The activity plays the system
alarm tone (`USAGE_ALARM` / `CONTENT_TYPE_SONIFICATION`) on a loop and
holds a partial wake lock for up to 10 minutes. Vibration runs on a
700/500 ms repeating waveform. Back is intercepted — only Snooze or
Dismiss can close the screen.

- [ ] Lock the device, schedule an alarm 1 min out, wait → screen turns on,
      activity appears over the lock screen, alarm tone plays, device vibrates
- [ ] Pressing Back does nothing
- [ ] Snooze or Dismiss closes the activity and stops sound + vibration

---

## Settings tab

### Appearance

Three options as segmented buttons: **Follow system**, **Light**, **Dark**.
Selected mode persists in DataStore and is applied app-wide without an
Activity restart.

- [ ] Switch to Dark → entire app updates without flicker, no restart
- [ ] Switch to Light → same
- [ ] Switch to Follow system → matches the OS theme; toggling system theme
      flips the app live

### Theme palette

Ten palettes (5 Claude-original, 5 ported from the Codex implementation),
shown as filter chips. **Anchor** is the default; on Android 12+ it pulls
Material You dynamic color from the wallpaper, falling back to a deep navy
elsewhere. Other palettes (Daybreak, Harbor, Grove, Ember, Twilight,
Sunrise, Forest, Slate, Plum) are hand-tuned and apply identically across
all API levels. A description card below the chip row shows the selected
palette's name and a one-sentence color summary, updating live.

- [ ] Tap each palette → app theme recomposes instantly
- [ ] Description card updates on every pick
- [ ] On Android 12+: Anchor reflects wallpaper accent colors; change
      wallpaper → re-open app → accent updates
- [ ] Selection survives an app kill / cold start

### Default zone source for new alarms

A radio group: **My clocks** (only added zones) or **All zones**.
This is the *initial* value of the per-alarm zone-source toggle on the
alarm editor — the user can still flip it per alarm.

- [ ] Pick "My clocks" → next time you open the alarm editor, the toggle
      defaults to "My clocks"
- [ ] Flip the per-alarm toggle to "All zones" → the global setting doesn't
      change

### First day of week

Segmented buttons: **Monday** or **Sunday**. Drives the order of the
day-of-week chips on the alarm editor.

- [ ] Pick Monday → alarm editor day chips read M T W T F S S
- [ ] Pick Sunday → alarm editor day chips read S M T W T F S
- [ ] Saved alarms retain their day mask regardless of the order

---

## System integration

### Boot rescheduling

The app re-arms every enabled alarm when:
- The device boots (`BOOT_COMPLETED`, also `LOCKED_BOOT_COMPLETED`)
- The package is replaced (e.g. app updates)
- The system time zone changes
- The system time is set
- **The exact-alarm permission state changes** (`SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`)
  — so a user who newly grants exact-alarm permission immediately gets all
  existing alarms upgraded from the inexact fallback to `setAlarmClock`.

- [ ] Schedule a recurring alarm → reboot the device → alarm still fires at
      its next scheduled time
- [ ] Schedule an alarm anchored to NYC → change device time zone to Tokyo →
      alarm still fires at NYC's wall-clock minute (not at Tokyo's)
- [ ] (API 31/32 only) Deny exact-alarm → schedule an alarm → grant
      exact-alarm in system settings → alarm is automatically re-armed
      exactly without re-editing it

### System status-bar alarm chip

`setAlarmClock` populates the OS-level "Next alarm" chip in the status bar.
Tapping it from the system shade opens the app.

- [ ] Schedule a future alarm → status-bar alarm icon appears
- [ ] Tap it from the lock screen / quick settings → app opens

### Permission flows

- **`POST_NOTIFICATIONS`** (API 33+): requested on first launch via
  `ActivityResultContracts.RequestPermission`. If denied, the readiness
  indicator surfaces an "Open" CTA into app notification settings.
- **`SCHEDULE_EXACT_ALARM`** (API 31/32): user-grantable. If denied,
  alarms fall back to `setAndAllowWhileIdle` (still wakes the device,
  not minute-exact). The readiness indicator surfaces a "Grant" CTA
  into the system's alarm-permission settings.
- **`USE_EXACT_ALARM`** (API 33+): claimed in the manifest as the
  alarm-clock app category — auto-granted on install, no user prompt.

- [ ] API 33+: first launch shows the system notifications prompt
- [ ] Deny → readiness indicator red with "Open" → tapping lands on app
      notification settings
- [ ] API 31/32: deny exact alarm → indicator red with "Grant" → tapping
      lands on the "Alarms & reminders" system setting

### Persistence

- **Room** (SQLite): clocks and alarms.
- **DataStore Preferences**: appearance mode, theme palette, default zone
  source, first-day-of-week.

- [ ] Add clocks + alarms → force-stop the app → reopen — everything's
      still there
- [ ] Change theme + first-day-of-week → reopen — both persist
- [ ] Uninstall + reinstall — wipes everything (expected)

---

## Under the hood — correctness

### Zone-anchored wall-time alarms

Alarms fire at the **wall-clock** time in the zone the alarm was created
for, not at a fixed instant. If the user is in Tokyo and creates an alarm
for 7:00 New York time, the alarm rings at 9:00 PM Tokyo time.

- [ ] Set device zone to `Asia/Tokyo`
- [ ] Create an alarm for 7:00 AM `America/New_York`
- [ ] The "Next: …" row shows the New York time and the relative duration
      should reflect the time difference

### DST handling

The next-trigger math (`AlarmScheduler.nextTriggerMillis`) explicitly
validates each candidate against the zone's DST rules:
- **Spring-forward gap** — if the wall-clock time doesn't exist on a given
  day (e.g. 02:30 in NYC on the second Sunday of March), that day is
  skipped entirely; the alarm fires the next valid day.
- **Fall-back ambiguity** — when the wall-clock time exists twice on a
  given day (e.g. 01:30 in NYC on the first Sunday of November), the
  earlier occurrence (still-DST) is chosen.

Covered by `AlarmSchedulerNextTriggerTest` (5 DST/edge cases + 2 more for
one-shot semantics + invalid zone fallback).

- [ ] Run `./gradlew :app:testDebugUnitTest` — all 12 tests pass
- [ ] (Manual) Pick a future spring-forward Sunday in NYC, set device zone to
      NYC, manipulate device clock to just before 02:30 of that day, schedule
      a 02:30 every-day alarm — should fire the *next* day at 02:30, not at
      03:30 on the spring-forward Sunday

### Repository-layer validation

Every read and write of a clock or alarm passes through a sanitizer:
- Labels trimmed and capped at 80 characters
- Zone ids validated against `ZoneId.of(...)`; unknown zones fall back to
  the device's default zone
- Hour clamped to 0–23, minute to 0–59
- Day-of-week mask AND'd with `0b1111111` to strip garbage high bits;
  `mask = 0` is preserved (it means one-shot in this app's semantics)

Covered by `ValidateTest`.

### Permission state listener

`BootReceiver` listens for
`android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`, the
broadcast Android fires when the user toggles the exact-alarm permission
in Settings. On receipt, every enabled alarm is re-scheduled, upgrading
from the inexact fallback to `setAlarmClock` automatically.

### Tests

| Suite | What it covers |
|---|---|
| `AlarmSchedulerNextTriggerTest` | DST spring-forward skip, DST fall-back first-instance pick, current-minute skip, Monday-only forward walk, one-shot today vs tomorrow, invalid-zone fallback |
| `ValidateTest` | Label trim/cap, zone validation, hour/minute clamp, day-mask sanitization while preserving one-shot |

Run them with `./gradlew :app:testDebugUnitTest`.

---

## Build & tooling

- **Gradle 8.13** via the project wrapper (set by Android Studio's AGP
  Upgrade Assistant)
- **AGP 8.9.2** (Studio may suggest 8.13 — safe to accept)
- **Kotlin 2.1.0**, Compose plugin 2.1.0, KSP for Room
- **Daemon JVM Toolchain** pinned to JDK 21 via
  `gradle/gradle-daemon-jvm.properties`. Vendor unconstrained so CLI builds
  work on any JDK 21 (Android Studio still uses its bundled JBR).

- [ ] `./gradlew :app:assembleDebug :app:testDebugUnitTest` succeeds on a
      fresh checkout (with `JAVA_HOME` pointing at any JDK 21+ on CLI)
- [ ] Android Studio Run button works after a Gradle sync

---

## Deferred / future enhancements

These are flagged on purpose, not bugs:

- **Custom alarm sound per alarm.** Today every alarm uses the system
  default alarm tone (`RingtoneManager.TYPE_ALARM`). Tabling until v0.2 —
  would require a Room schema bump (`soundUri` column), a system
  `RingtoneManager.ACTION_RINGTONE_PICKER` flow on the alarm editor, and
  `AlarmService.playRingtone()` updates.
- **Snooze indicator on the alarms list + persistent snoozed notification.**
  Today the only outward sign that an alarm is currently snoozed is the
  OS-level "Next alarm" status-bar chip. We could surface "Snoozed until
  7:35 AM" on the alarm card and post a low-importance notification with a
  "Cancel snooze" action. Needs a `snoozeUntilMillis` field on the alarm
  entity plus AlarmService changes.
- **Legacy migration from prior Java versions of this app.** Codex's
  implementation has this (it's reading from SharedPreferences); Claude's
  version doesn't because it's a fresh codebase. If this branch ever ships
  as an upgrade *over* the Codex APK, users would lose their alarms and
  clocks. Worth porting if that scenario matters.
- **City-name search in the zone picker.** Currently we match against the
  zone id (region/city). A friendlier search by city name (e.g. "Tokyo"
  always works because the id contains it, but "New York City" wouldn't
  match `America/New_York` cleanly without alias data) would need a city
  database.
- **Widget / lock-screen complication.**
- **Localized strings.** Today's `strings.xml` is English-only.
