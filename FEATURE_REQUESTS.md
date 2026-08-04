# ZoneAnchor Feature Requests - Status Ledger

This file captures user-facing product requests discussed so far and whether
they have been implemented, remain open, or were deferred/cancelled.

Legend:

- `[x]` Implemented
- `[~]` Partially implemented or implemented differently
- `[ ]` Not implemented yet
- `[-]` Deferred or intentionally cancelled

## Naming And Brand

- [x] Rename the app away from "Universal Clock" to avoid copyright/trademark
      risk.
- [x] Use **ZoneAnchor** as the app name.
- [x] Keep the name clear enough that users understand the app is related to
      alarms and time zones.
- [x] Add app icon/source artwork and an About dialog.

## Clocks

- [x] Show the device's current local/device time.
- [x] Show time-zone-locked clocks chosen by the user.
- [x] Let users add multiple time-zone clocks.
- [x] Display clocks in their own **Clocks** tab.
- [x] Provide an add-clock flow where the user selects a time zone and label.
- [x] Let users add a custom label for each clock.
- [x] Let users edit existing clocks.
- [x] Let users delete clocks.
- [x] Let users clear all clocks.
- [x] Let users reset clocks to suggested defaults.
- [x] Let users reorder clocks.
- [x] Let users group clocks.
- [x] Add a time converter across saved clocks.
- [x] Make converter result rows selectable as the reference clock.
- [ ] Add richer city/place search or city-to-time-zone lookup.

## Alarms

- [x] Display alarms in their own **Alarms** tab.
- [x] Provide a consistent floating add button for adding alarms.
- [x] Let users add a new alarm locked to a selected time zone.
- [x] Let users choose whether the alarm zone picker shows only saved clocks
      or all available world time zones.
- [x] Let users add a custom label for each alarm.
- [x] Let users choose day(s) of the week for each alarm.
- [x] Let users choose alarm sound mode.
- [x] Let users choose vibration mode.
- [x] Let users edit existing alarms.
- [x] Let users toggle alarms on/off.
- [x] Let users cancel/delete alarms.
- [x] Keep alarm behavior tied to the selected zone's wall-clock time, not the
      device's current time zone.
- [x] Add advanced repeat rules for every N weeks, monthly dates, monthly
      ordinal weekdays, and every N months from a start date.
- [x] Show natural repeat descriptions on alarm cards.
- [x] Sort alarm cards by actual next ring instant, not face-value time.
- [x] Show reference clock fire time, device fire time, and remaining duration.
- [x] Allow alarms to associate with a saved world clock label.
- [x] Allow Settings to show either associated clock labels or raw zone IDs on
      alarm cards.
- [x] Detach alarms from clock labels when a clock's time zone changes.
- [x] Let the user know when deleting or changing a clock will detach alarms.
- [ ] Natural-language alarm/reminder creation, such as "first Sunday every
      month" or "every 6 months starting August 3, 2026".
- [ ] Per-alarm sound picker, volume, or fade-in.

## Timers

- [x] Add a dedicated **Timers** tab.
- [x] Add, edit, start, pause, resume, reset, and delete timers.
- [x] Add timer presets and keypad-style duration entry.
- [x] Let timers finish as notification-only or full-screen alerts.
- [x] Add a foreground timer alert service.
- [x] Let users configure default timer finish behavior.
- [x] Let users group and reorder timers.
- [ ] Sequenced timers that auto-start another timer.

## Stopwatch

- [x] Add a dedicated **Stopwatch** tab.
- [x] Support multiple stopwatches.
- [x] Start, pause, resume, reset, and delete stopwatches.
- [x] Record laps.
- [x] Rename stopwatches.
- [x] Let users group and reorder stopwatches.
- [ ] Lap stats such as best/slowest lap and delta from previous lap.

## Navigation And Layout

- [x] Organize the app into tabs.
- [x] Expand from the original three tabs to Clocks, Alarms, Timers,
      Stopwatch, and Settings.
- [x] Keep add actions visually consistent between tabs.
- [x] Avoid placing the top of the app behind the Pixel camera, status bar, or
      emulator cutout area.
- [x] Remove empty panels unless they provide meaningful status or controls.
- [x] Move destructive row actions mostly behind long-press menus where that
      saves card space.
- [ ] Allow users to reorder or hide tabs.

## Settings And Themes

- [x] Add a **Settings** page.
- [x] Add appearance settings: Follow System, Light, Dark.
- [x] Add selectable app themes/palettes.
- [x] Apply theme and appearance changes app-wide.
- [x] Add default zone source for new alarms.
- [x] Add alarm zone display preference: Clock label or Zone ID.
- [x] Add first-day-of-week preference.
- [x] Add grouping toggles per feature area.
- [ ] AMOLED-black theme.

## Persistence And Editing

- [x] Persist added clocks.
- [x] Persist added alarms.
- [x] Persist timers, stopwatches, laps, and groups.
- [x] Persist theme and appearance settings.
- [x] Allow clocks and alarms to be edited after they are created.
- [x] Add Room migrations as the schema evolved.
- [ ] Backup/restore import/export.
- [ ] Cloud or multi-device sync.

## Deferred Or Cancelled Requests

- [-] Keep alarms attached to a clock label after that clock's time zone
      changes. Cancelled because it can make an alarm display a label whose
      current clock no longer matches the alarm's saved time zone.
- [-] Offer three choices when a clock time zone changes: keep, migrate all,
      or decide one by one. Cancelled in favor of a simpler detach-and-explain
      behavior.
- [-] Reorder disabled alarms manually. Cancelled because alarm order should
      communicate actual next-ring order.

## Still Open Product Questions

- [ ] Should ZoneAnchor become a broader reminders app with notes/completion,
      or remain an alarm-first app with reminder-like power?
- [ ] Should natural-language parsing be implemented locally with deterministic
      rules, through an AI parser, or both?
- [ ] Should city/place search ship as a bundled offline database, a lightweight
      alias list, or an online lookup?
