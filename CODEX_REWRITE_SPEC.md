# Spec for Codex — ZoneAnchor Alarm rewrite (Kotlin + Compose + AndroidX)

This replaces `CODEX_SPEC.md` for this pass. Treat the existing Java codebase as a *reference implementation of the product*, not a structural template. The rewrite must preserve every user-visible behavior listed in the "Behavior preservation checklist" below.

---

## Goal

Rewrite the app in Kotlin + Jetpack Compose + AndroidX, with Room for persistence and DataStore for settings. Single Activity. MVVM. No third-party DI framework.

## Non-goals

- New features. If it isn't in the current app, don't add it (full-screen ring activity from `CODEX_SPEC.md` #11 is the only addition — see below).
- Material You / dynamic color. We keep the five named themes.
- Multi-module Gradle setup. Single `:app` module.

---

## Framework choices (locked, not suggestions)

| Concern | Choice |
|---|---|
| Language | Kotlin 2.1+ (`jvmTarget = 17`) |
| Build | AGP 8.9+, Gradle Kotlin DSL (`.kts`) for new build files |
| UI | Jetpack Compose, latest stable Compose BOM, Material 3 |
| Architecture | Single-Activity + Navigation-Compose 2.8+ |
| State | `ViewModel` + `StateFlow`. **No LiveData.** |
| DI | Manual (constructor injection from `Application`). **No Hilt, no Koin.** |
| Persistence | Room 2.7+ (with KSP, not KAPT) for alarms + clocks; DataStore Preferences 1.1+ for settings |
| Coroutines | `kotlinx.coroutines` — use `Dispatchers.IO` for DB, `Dispatchers.Default` for time math |
| Time | `java.time` (already API-26-safe). **No ThreeTenBP.** |
| Tests | JUnit4 + Truth + Robolectric for `AlarmTimeCalculator` + Room DAO tests |
| `minSdk` / `targetSdk` / `compileSdk` | 26 / 36 / 36 (unchanged) |

---

## Module structure

```
app/src/main/java/com/zoneanchor/
  ZoneAnchorApp.kt                  # Application — creates DB, repos, ensures channels
  MainActivity.kt                   # Sole Activity. Hosts NavHost. Applies theme.

  alarm/
    AlarmTimeCalculator.kt
    AlarmScheduler.kt
    AlarmReceiver.kt                # goAsync + coroutine scope
    SystemEventReceiver.kt          # goAsync + coroutine scope
    NotificationHelper.kt
    AlarmRingActivity.kt            # full-screen ring UI (new)

  data/
    AppDatabase.kt
    alarms/ { AlarmEntity, AlarmDao, AlarmRepository }
    clocks/ { ClockEntity, ClockDao, ClockRepository }
    settings/ { SettingsRepository }   # DataStore-backed
    migration/LegacyPrefsMigration.kt  # one-shot on first run

  model/
    ZonedAlarm.kt                   # domain model, not the entity
    ClockEntry.kt
    AppearanceMode.kt               # enum: SYSTEM, LIGHT, DARK
    AppPalette.kt                   # enum: DAYBREAK, HARBOR, GROVE, EMBER, TWILIGHT

  ui/
    theme/ { AppTheme.kt, ColorSchemes.kt }
    nav/ { Screen.kt, AppNavHost.kt }
    clocks/ { ClocksScreen.kt, ClockEditorScreen.kt, ClocksViewModel.kt }
    alarms/ { AlarmsScreen.kt, AlarmEditorScreen.kt, AlarmsViewModel.kt }
    settings/ { SettingsScreen.kt, SettingsViewModel.kt }
    components/ { ZonePicker.kt, DayOfWeekPicker.kt, AppScaffold.kt }
```

No file over 300 lines. If a screen would exceed it, split its composables into siblings in the same package.

---

## Data layer

### Room

`AlarmEntity` columns mirror the current JSON keys: `id INTEGER PK`, `label TEXT`, `zoneId TEXT`, `hour INTEGER`, `minute INTEGER`, `daysOfWeekMask INTEGER`, `soundMode TEXT`, `vibrate INTEGER`, `enabled INTEGER`, `nextTriggerAtMillis INTEGER`.

`ClockEntity`: `zoneId TEXT PK`, `label TEXT`, `position INTEGER` (insertion order).

DAOs expose `Flow<List<Entity>>` for observers and `suspend` functions for writes. Repositories own the `Entity` ↔ domain-model mapping; ViewModels and the rest of the app never see entities.

### Settings (DataStore)

Single Preferences DataStore named `zoneanchor_settings`. Keys: `theme_palette` (string, enum name), `appearance_mode` (string, enum name). Expose as `Flow<AppearanceMode>` and `Flow<AppPalette>` from `SettingsRepository`.

### Legacy migration (must not be skipped)

The current app stores data in three SharedPreferences files: `zoneanchor_alarm` (key `alarms_json`), `zoneanchor_clocks` (key `zones_json`), `zoneanchor_settings` (keys `theme_id`, `appearance`). On first launch of the new version:

1. If `AppDatabase` is empty AND any of those prefs files exist with valid JSON: parse and import.
2. Migrate `theme_id` → `AppPalette` by id string (`daybreak` → `DAYBREAK`, etc.). Migrate `appearance` similarly.
3. Also handle the *legacy-legacy* path in `AlarmStore.loadLegacyAlarm` (loose `zone_id`, `hour`, `minute` keys without the JSON array). Import as a single alarm with id 400 if present.
4. After successful import, `getSharedPreferences(...).edit().clear().apply()` on all three files.
5. Wrap the migration in a Room `Migration` or a one-shot `SettingsRepository` flag (`legacy_imported`) so it runs at most once.

Write a unit test (Robolectric) that seeds the three SharedPreferences files with realistic JSON and asserts Room + DataStore contents post-migration.

---

## Per-screen specs

### MainActivity

- Hosts `MaterialTheme` (palette from DataStore × dark-mode resolution) and `AppNavHost`.
- Calls `enableEdgeToEdge()`. Status & nav bars use `surface` color. System bar icons follow theme luminance.
- On `onCreate`: trigger `LegacyPrefsMigration.runIfNeeded()` via a top-level coroutine on the Application scope.
- Three top-level destinations: `Clocks`, `Alarms`, `Settings`. Use `NavigationBar` (Material 3) at the bottom, not a row of styled `Button`s.
- A `Scaffold` with a `FloatingActionButton` for Clocks and Alarms tabs (`extended = false`, content = `Icons.Default.Add`). Hidden on Settings.

### Clocks tab

- LazyColumn of clock cards. First card is always "Device time" (no edit, no delete) showing `ZoneId.systemDefault()`.
- Each user clock: large time text, `zoneId — EEE, MMM d, yyyy`, edit + delete buttons.
- Editor is a separate route (`clocks/editor?zoneId=...`), not an inline form. Fields: zone (via `ZonePicker`), label. Save / Cancel.
- A `LaunchedEffect` keyed on `Unit` with a `while (true) { delay(1.seconds); tick++ }` loop drives the per-second time refresh. Do **not** read from the repo each tick — cache the clock list in state and recompute the displayed `ZonedDateTime` from the cached `zoneId`s.

### Alarms tab

- LazyColumn of alarm cards. Empty state: "No alarms yet."
- Top panel (collapsible/conditional): exact-alarm permission button + status string, notification permission status, last-action status string. Same logic as the current `alarmStatusPanel`.
- Each alarm card: label (or zoneId), time, days summary ("Every day" / "Weekdays" / "Weekends" / comma list), zoneId, sound + vibrate summary, "Next: ..." line. Edit + Cancel.
- Editor is a separate route (`alarms/editor?id=...`). Fields: label, zone (with the "use clock zones" Switch backed by `ClockRepository.allFlow().map { it.isNotEmpty() }`), time (Material 3 `TimePicker`), day-of-week multi-select (`DayOfWeekPicker`), sound (Default / Silent radio or dropdown), vibrate Switch.
- The "Pick at least one day." validation is enforced in the ViewModel before calling `AlarmScheduler.scheduleNext`.

### Settings tab

- Section: Appearance. Three options as Material 3 `RadioButton` rows: Follow system, Light, Dark.
- Section: Theme. Five palette cards (Daybreak, Harbor, Grove, Ember, Twilight). Each card shows name, description, three color swatches (background / surface / accent) sampled from the palette's *light-mode* resolution, and a "Use this theme" / "Selected" button.
- Choosing a palette or appearance mode persists via DataStore. The MaterialTheme recomposes — **no `recreate()`**. This is one of the wins of the rewrite.

### AlarmRingActivity (new — rolls in CODEX_SPEC.md #11)

- Compose UI. `windowFlags = FLAG_SHOW_WHEN_LOCKED | FLAG_TURN_SCREEN_ON | FLAG_KEEP_SCREEN_ON` plus `setShowWhenLocked(true)` / `setTurnScreenOn(true)` on API ≥ 27.
- Big time text (alarm's locked wall-clock time in the alarm's zone), small "It is HH:mm a z in <zoneId>." line, alarm label.
- Two large buttons: **Snooze 9 min** (cancels current trigger, schedules a one-off via `AlarmManager.setAlarmClock` at `Instant.now() + 9 min`, finishes Activity), **Dismiss** (finishes Activity, lets `AlarmScheduler.scheduleNext` re-arm the recurring schedule).
- Launched from a `setFullScreenIntent(...)` on the notification. The notification also has Snooze / Dismiss `Notification.Action`s that route to the same logic via a `BroadcastReceiver` (`AlarmActionReceiver`).
- Add `<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />` to the manifest.

---

## Behavior preservation checklist

Codex must verify each item before opening the PR. Drop anything from this list = regression.

- [ ] `AlarmManager.setAlarmClock` (not `setExact`/`set`) for all scheduling, with both the trigger broadcast PendingIntent **and** the `showIntent` for the system's status-bar alarm chip.
- [ ] All PendingIntents use `FLAG_IMMUTABLE | FLAG_UPDATE_CURRENT`. Request code = `alarmId` so cancel/update target the right alarm.
- [ ] Four notification channels split by `(soundMode × vibrate)`, exactly as `NotificationHelper.ensureChannel` does today. Channels are immutable once created — we cannot dynamically toggle sound/vibrate at notify time on API ≥ 26, so the four-channel matrix stays.
- [ ] `RingtoneManager.getDefaultUri(TYPE_ALARM)` with a `TYPE_NOTIFICATION` fallback. `AudioAttributes` set to `USAGE_ALARM` + `CONTENT_TYPE_SONIFICATION`.
- [ ] `SystemEventReceiver` listens for **all** of: `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED`. The last one is easy to miss — keep it.
- [ ] Manifest export: split into two receivers if needed. `BOOT_COMPLETED` + `MY_PACKAGE_REPLACED` exported; protected broadcasts unexported.
- [ ] Day-of-week bitmask: bit 0 = Monday … bit 6 = Sunday. `mask == 0` is treated as `ALL_DAYS` (0b1111111). Preserved on read from Room.
- [ ] "Next trigger" search walks up to 7 days forward and falls back to `now + 1 day` only if no day matches (shouldn't happen with the `mask==0 → ALL_DAYS` normalization, but keep the fallback).
- [ ] Time-zone validation: `ZoneId.of(...)` in a try/catch. Unknown zones fall back to `America/New_York` (current `ZonedAlarm.DEFAULT_ZONE_ID`).
- [ ] Default seed clocks on first run when no user clocks exist: `America/New_York` and `UTC` (skip whichever matches `systemDefault`), with `Europe/London` as the empty-result fallback. See current `ZoneClockStore.defaultZones`.
- [ ] Exact-alarm permission gate: check `AlarmManager.canScheduleExactAlarms()` on API ≥ 31. Surface the "Enable exact alarms" CTA pointing at `Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM` with a `package:` URI.
- [ ] Notification post-permission flow on Tiramisu+: prompt on first launch.
- [ ] Edit-in-progress state survives rotation (Compose handles this for free via `rememberSaveable` — verify).
- [ ] Tab selection survives rotation.
- [ ] Theme/appearance change is instant (no Activity restart).

## Required fixes rolled in from `CODEX_SPEC.md`

These are not optional. The rewrite must address them or it isn't done.

- **#1 — alarm list cached.** ViewModels collect a `StateFlow<List<ZonedAlarm>>` from the repository. The tick loop reads from the flow's current value, never the DAO.
- **#2 / #3 — broadcast receivers async.** `AlarmReceiver` and `SystemEventReceiver` use `goAsync()` + a `CoroutineScope(Dispatchers.IO + SupervisorJob())`. `pendingResult.finish()` in a `finally` block. No DB or `SharedPreferences` access on `onReceive`'s thread.
- **#4 — DST spring-forward.** In `AlarmTimeCalculator`, after building each candidate `ZonedDateTime`, assert `candidate.hour == hour && candidate.minute == minute`. If they differ, the wall-clock time didn't exist that day — skip the candidate, continue to the next. Document the policy. Required tests:
  - Sat 17:00 alarm, days = Mon only → next Mon 17:00.
  - `daysOfWeekMask = 0` falls back to `ALL_DAYS`.
  - `America/New_York`, alarm 02:30, evaluated on the morning of US spring-forward Sunday → returns the *next* day's 02:30, not a shifted instant.
  - `America/New_York`, alarm 01:30, evaluated just before US fall-back Sunday's 01:30 → returns the first 01:30 EDT occurrence, not the second.
  - Alarm time == current minute → skipped, returns next allowed day.
- **#10 — searchable zone picker.** `ZonePicker` composable opens a Material 3 `ModalBottomSheet` (or `AlertDialog`) with a `TextField` filter and a `LazyColumn` of matches. `systemDefault()`, `America/New_York`, and `UTC` pinned to the top when the filter is empty.

---

## Out of scope for this PR

- Snooze duration setting (hardcode 9 min for now).
- Multiple snooze cycles tracking.
- Per-alarm custom ringtone picker.
- Backup / cloud sync.
- Widget.
- Localization beyond extracting strings (English-only `strings.xml` is fine).

---

## Tests (required, not optional)

- `AlarmTimeCalculatorTest` — the five DST/edge cases listed under #4.
- `LegacyPrefsMigrationTest` (Robolectric) — seeded SharedPreferences → asserts Room + DataStore.
- `AlarmDaoTest` (Room in-memory) — insert/update/delete + flow emits new state.
- `AlarmRepositoryTest` — entity ↔ domain mapping round-trips.

Skip Compose UI tests — too costly for the value at this stage. Manual verification via the checklist below is enough.

---

## Manual verification before PR

Run on a real device or emulator, API 34+:

1. Fresh install: app launches, shows seeded clocks, no alarms.
2. Add a clock for `Asia/Tokyo`. It appears, time updates each second.
3. Add an alarm for 2 minutes from now in your local zone. Wait. Full-screen ring fires. Snooze works. Dismiss works.
4. Toggle Dark mode → recomposes without restart.
5. Switch palette → recomposes without restart.
6. Rotate during alarm editor with text typed in the label field → text survives.
7. Force-stop, then reopen — clocks and alarms persist.
8. Upgrade-in-place test: install the old Java APK first, create an alarm + a clock, then install the new APK over it. Both data items must appear in the rewritten app.
9. Reboot the device with an enabled alarm scheduled for 5 min after boot → it fires.

---

## Definition of done

- All items in **Behavior preservation checklist** ticked.
- All items in **Required fixes rolled in** addressed.
- All four test files exist and pass.
- All nine manual verification steps pass.
- No file over 300 lines.
- `./gradlew assembleDebug lint test` is clean.
- Old Java sources in `com.zoneanchor` are deleted, not left in `legacy/` or commented out.
- `CODEX_SPEC.md` deleted (its items are absorbed here or explicitly deferred).
- `README.md` updated to mention Kotlin + Compose + Room + DataStore and the new build invocation (still works with the existing `gradlew` script).

---

## Anti-patterns Codex must avoid

- Don't introduce Hilt "just because." Manual wiring is fine at this size — pass repos in via `Application` getters or `CompositionLocal`.
- Don't use `LiveData`. `StateFlow` only.
- Don't use `runBlocking` outside tests.
- Don't read from a repo inside a `@Composable` body — only inside `LaunchedEffect`/`collectAsState`/`ViewModel`.
- Don't add a splash screen, onboarding flow, analytics, or crash reporter.
- Don't restructure into multiple Gradle modules.
- Don't replace `AlarmManager.setAlarmClock` with `setExactAndAllowWhileIdle` or `WorkManager`. The user-visible status-bar alarm icon depends on `setAlarmClock`.
- Don't preserve any of the current programmatic-view UI code "for reference."
