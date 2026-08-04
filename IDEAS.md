# ZoneAnchor - Ideas, Backlog, And Decisions

This file is the product parking lot. It keeps track of ideas that shipped,
ideas still worth considering, and ideas that were deferred or cancelled.

Legend:

- `[x]` Shipped
- `[ ]` Open
- `[~]` Needs design
- `[-]` Deferred or cancelled

## Shipped Ideas

### Time converter across clocks

- [x] Pick a time in one zone and see the equivalent time in saved clocks.
- [x] Select a converter result row to make that clock the reference.
- [x] Add quick relative controls such as previous hour, next hour, next day,
      yesterday, today, and tomorrow.

### Timers and stopwatch expansion

- [x] Add a dedicated Timers tab.
- [x] Add a dedicated Stopwatch tab.
- [x] Support multiple timers and multiple stopwatches.
- [x] Add foreground/full-screen timer alert behavior.
- [x] Add stopwatch laps.

### Groups and reordering

- [x] Add groups for clocks, alarms, timers, and stopwatches.
- [x] Let users collapse groups.
- [x] Let users move items between groups.
- [x] Let users reorder clocks, timers, stopwatches, and groups.
- [~] Alarm item order is automatic by next ring time, so manual alarm reorder
      is intentionally not supported.

### Advanced alarm recurrence

- [x] Add advanced repeat options below the simple repeat presets.
- [x] Support every N weeks.
- [x] Support monthly by date.
- [x] Support monthly by ordinal weekday, such as first Sunday.
- [x] Support every N months from a start date.
- [x] Show natural repeat labels on alarm cards.

### Clock-label-aware alarms

- [x] Let alarms associate with a saved world clock label.
- [x] Auto-fill and lock the alarm time zone when a clock label is selected.
- [x] Let users choose whether alarm cards show clock labels or zone IDs.
- [x] Explain what happens when deleting a clock with associated alarms.
- [x] Detach alarms when an associated clock's time zone changes.

### Alarm card clarity

- [x] Remove the inline trash icon from alarm cards to gain space.
- [x] Keep delete behind long-press actions.
- [x] Sort alarms by actual next ring instant.
- [x] Show reference clock date/time, device date/time, and remaining duration.

### Branding and polish

- [x] Rename the app to ZoneAnchor.
- [x] Add the C+P launcher icon artwork.
- [x] Add About in Settings.
- [x] Add haptic and click feedback to tappable controls.
- [x] Add multiple theme palettes.

## High-Leverage Open Ideas

### Natural-language alarm/reminder creation

- [ ] Parse phrases like "first Sunday of every month" into the existing
      advanced recurrence model.
- [ ] Parse phrases like "every 6 months starting August 3, 2026, on the 3rd
      of the reminder month".
- [ ] Decide whether parsing should be deterministic/local, AI-assisted, or a
      hybrid with review before save.

Why it matters: the recurrence engine is now powerful, but advanced forms are
still UI-driven. Natural language could make ZoneAnchor feel like a reminder
assistant without losing its explicit scheduling model.

### City/place search

- [ ] Search by city aliases and common place names, not only IANA zone IDs.
- [ ] Decide whether to use a bundled offline alias database or online lookup.
- [ ] Decide how to represent places that share a time zone but should have
      different user labels.

Why it matters: ZoneAnchor's mental model is "people/places/routines", while
IANA IDs are still a technical interface.

### README and product screenshots

- [ ] Add a proper `README.md`.
- [ ] Position ZoneAnchor as a timezone-aware alarm/reminder app built around
      named reference clocks.
- [ ] Add screenshots or short screen recordings.
- [ ] Add build/install notes.

Why it matters: the product has become distinctive enough that the repo should
explain the idea clearly.

### Backup and restore

- [ ] Export clocks, alarms, timers, stopwatches, groups, and settings to JSON.
- [ ] Import from JSON with validation.
- [ ] Consider whether imports merge or replace existing data.

Why it matters: this app is personal infrastructure. Users should be able to
move it between devices or recover after reinstalling.

## Moderate Open Ideas

### Home-screen widgets

- [ ] Widget for next alarm.
- [ ] Widget for alarms list.
- [ ] Widget for a selected timer.
- [ ] Possibly a widget for saved world clocks.

### Launcher shortcuts

- [ ] Long-press app icon actions for Add alarm, Add timer, and New stopwatch.
- [ ] Deep links into the relevant add/edit surfaces.

### Stopwatch lap stats

- [ ] Highlight best and slowest laps.
- [ ] Show delta from the previous lap.
- [ ] Consider a compact summary for total, average lap, best, and slowest.

### Per-alarm sound and volume

- [ ] Pick a custom sound per alarm.
- [ ] Add per-alarm volume.
- [ ] Add optional fade-in.
- [ ] Preview alarm sounds from the alarm actions menu.

### Sequenced timers

- [ ] Let one timer auto-start another.
- [ ] Consider named timer sequences for cooking, workouts, study sessions, or
      routines.

### DND and manufacturer behavior

- [ ] Verify alarm and timer behavior through Do Not Disturb on supported
      Android versions and major OEMs.
- [ ] Add settings/help affordances if specific manufacturers need extra setup.

### Tab customization

- [ ] Let users hide tabs they do not use.
- [ ] Let users reorder bottom navigation tabs.

### AMOLED-black theme

- [ ] Add a pure-black dark theme variant for OLED screens.

## Bigger Product Questions

### Reminder mode

- [~] Decide whether ZoneAnchor should grow into a broader reminder/task app.
- [~] Possible additions: notes, completion, history, missed reminders,
      categories, and non-ringing notification reminders.

Current leaning: ZoneAnchor should stay alarm-first for now, but the reference
clock + recurrence model is already reminder-shaped.

### Clock labels as durable references

- [~] Decide how much "identity" a clock label should carry.
- [x] Current behavior: alarms can reference a saved clock label.
- [x] If the clock label changes, associated alarm display follows it.
- [x] If the clock time zone changes, associated alarms detach and keep their
      original saved time zone.
- [-] Cancelled: migrating all associated alarms automatically when a clock's
      time zone changes.

## Deferred Or Probably Not

### Smart wake

- [-] Alarm earlier based on sleep cycle. Deferred because it is complex,
      imprecise, and not central to timezone-aware scheduling.

### Themed Android 13 monochrome icon

- [-] Deferred. The current C+P artwork does not have a clean monochrome
      silhouette yet.

### Separate world-map/globe view

- [-] Deferred. Visually interesting, but the list and converter carry the
      product value better right now.

### Bedtime mode

- [-] Deferred. Wind-down reminders, wake alarms, and Do Not Disturb
      integration are a large product area.

## Documentation Maintenance

- [x] Refresh `FEATURES.md` to match the current five-tab app.
- [x] Mark implemented feature requests in `FEATURE_REQUESTS.md`.
- [x] Split shipped, open, deferred, and cancelled ideas in this file.
- [ ] Create `README.md`.
- [ ] Consider adding `BACKLOG.md` if `IDEAS.md` becomes too broad.
