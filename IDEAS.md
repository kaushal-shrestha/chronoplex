# Chronoplex — Future Ideas

Parking lot for features and polish not yet built. Pulled together as v0.1.0
shipped. Ordered roughly by leverage: how much value the feature adds for the
effort it takes. Real daily use should reshuffle this list — don't take the
order too seriously.

---

## High leverage

### Meeting planner across clocks
The thing the "Chronoplex / Advanced Time Management" name promises and the app
doesn't yet deliver. Pick a time in one zone and see the equivalent in every
added clock at once — a horizontal time strip you can scrub. Differentiates
the app from yet-another-clock.

### Launcher shortcuts
Long-press the app icon → quick "Add alarm" / "Add timer" / "New stopwatch"
without opening the app. `shortcuts.xml` + deep links. ~1 hour of work, high
daily payoff.

### Stopwatch lap stats
On each lap row, show best/slowest highlight and Δ from previous lap. Data is
already captured; this is purely a display change.

---

## Moderate

### Home-screen widget (Glance)
Single-timer or alarms-list widget on the home screen. Glance makes this
tractable but it's still a meaningful chunk.

### Tab reorder / hide
Let users hide tabs they don't use (e.g., Stopwatches) and reorder the bottom
nav. Small settings UI; updates to AppRoot.

### AMOLED-black theme
Pure-black variant of the dark theme for OLED screens. Small if the theme
plumbing already isolates surface colors.

### Per-alarm volume / fade-in
Today sound is just on/off. Add a per-alarm volume + an optional fade-in over
N seconds.

### Sequenced timers
A timer that auto-starts another when it finishes — useful for cooking, HIIT,
study sessions. Either a "next timer" field on the timer model, or a small
"sequence" entity.

### Backup / restore (JSON)
Export all clocks/alarms/timers/groups to a JSON file; restore from it.
Useful before reinstalls and for moving between devices.

### Quick Settings tile
A tile to toggle "all alarms off" or jump to the alarms list from the system
Quick Settings panel.

### DND override for alarms
Verify alarms ring through Do-Not-Disturb on every Android variant we support.
Documentation + a settings affordance if a manufacturer needs a special path.

### "Bedtime" mode
Wind-down reminder + matching wake alarm + Do-Not-Disturb activation. Google
Clock's headline feature. Large scope.

---

## Polish / small

### Stopwatch in-row label inline edit
Tap label to edit inline (currently behind long-press → Rename).

### Voice / Assistant intents
"Hey, set a timer for 5 minutes" hand-off into our app.

### Alarm preview
Long-press an alarm → "Preview tone" so users can sample the chosen sound.

### Alarms summary on Clocks tab
A subtle line at the top of Clocks: "Next alarm: 7:00 AM tomorrow (NYC)".

### Widget for next alarm
A tiny widget showing the next alarm time and label.

### World map / globe
Visualize added zones on a globe with day/night terminator. Cool but cosmetic.

### Refresh FEATURES.md
The catalog at the root is stale — it still talks about 3 tabs and pre-sheet
edit flows. Worth updating before any new contributor reads it.

---

## Probably not

These show up in similar apps but feel like noise for this one:

- Smart wake (alarm earlier based on sleep cycle) — gimmicky, complex
- Themed icons (Android 13+ monochrome) — the new C+P artwork doesn't have a
  clean silhouette and a separate monochrome mark would be a lot of work
- A second "world clock" view in addition to the list — list is sufficient
