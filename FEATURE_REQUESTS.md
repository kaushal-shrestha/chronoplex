# ZoneAnchor Alarm Feature Requests

This file captures the user-facing product requests discussed so far, separate from implementation details.

## Naming and Brand

- Rename the app away from "Universal Clock" to avoid copyright/trademark risk.
- Use **ZoneAnchor Alarm** as the app name.
- Keep the name clear enough that users understand the app is related to alarms and time zones.

## Clocks

- Show the device's current local time.
- Show time-zone-locked clocks chosen by the user.
- Let users add multiple time-zone clocks.
- Display clocks in their own **Clocks** tab.
- Provide an add-clock flow where the user selects a time zone first, then adds the clock.
- Let users add a custom label for each clock.
- Let users edit existing clocks.
- Let users delete clocks.
- Consider future support for adding clocks by city name.

## Alarms

- Display alarms in their own **Alarms** tab.
- Provide a consistent floating add button for adding alarms.
- Let users add a new alarm locked to a selected time zone.
- Let users choose whether the alarm zone picker shows:
  - only the zones already added on the Clocks tab, or
  - all available world time zones.
- Let users add a custom label for each alarm.
- Let users choose day(s) of the week for each alarm.
- Let users choose alarm sound mode.
- Let users choose vibration mode.
- Let users edit existing alarms.
- Let users cancel/delete alarms.
- Keep alarm behavior tied to the selected zone's wall-clock time, not the device's current time zone.

## Navigation and Layout

- Organize the app into tabs for Clocks, Alarms, and Settings.
- Keep add actions visually consistent between tabs.
- Avoid placing the top of the app behind the Pixel camera, status bar, or emulator cutout area.
- Remove useless/empty panels unless they provide meaningful status or controls.

## Settings and Themes

- Add a **Settings** page.
- Add appearance settings:
  - Follow System
  - Light Mode
  - Dark Mode
- Add selectable app themes/palettes.
- Apply theme and appearance changes as app-wide settings.

## Persistence and Editing

- Persist added clocks.
- Persist added alarms.
- Persist theme and appearance settings.
- Allow clocks and alarms to be edited after they are created.

## Future Ideas

- Add city-based clock search or city-to-time-zone lookup.
