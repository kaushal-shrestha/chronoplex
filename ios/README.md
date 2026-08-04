# ZoneAnchor iOS

Native SwiftUI implementation of ZoneAnchor, created on branch `codex/ios` in the `worktrees/ios` worktree.

## Implemented

- Five-tab app: Clocks, Alarms, Timers, Stopwatch, Settings.
- Live device clock, world clocks, custom labels, editing, deleting, clear-all, suggested reset, grouping, and a saved-clock time converter.
- Timezone-anchored alarms with custom labels, associated saved clocks, all-zone/my-clock zone source, sound/vibration flags, enable toggles, snooze, clear-all, grouping, repeat presets, weekly intervals, monthly day repeats, monthly ordinal-weekday repeats, next-fire display, and device/reference-time summaries.
- Timers with presets, edit/delete, undo after delete, clear-all, start/pause/resume/stop/reset, add-one-minute, default finish behavior, per-timer finish behavior, grouping, and local notifications.
- Multiple stopwatches with start/pause/resume/reset, laps, rename, delete undo, clear-all, grouping, and reordering.
- Settings for appearance, palette, alarm zone source/display, first weekday, grouping toggles, timer finish default, notification status, and About.
- Codable/UserDefaults persistence for clocks, alarms, timers, stopwatches, laps, groups, and settings.
- XCTest coverage for DST spring-forward gap handling, weekly interval anchors, and monthly ordinal-weekday recurrence.

## iOS Platform Notes

Android exact alarms and full-screen lock-screen alarm activities do not have direct public iOS equivalents. This port uses iOS local notifications with time-sensitive presentation where available, notification actions for snooze/dismiss, and a rolling schedule of future alarm occurrences.

## Verify

```sh
xcodebuild -project ios/ZoneAnchor.xcodeproj -scheme ZoneAnchor -sdk iphonesimulator -configuration Debug CODE_SIGNING_ALLOWED=NO build
xcodebuild test -project ios/ZoneAnchor.xcodeproj -scheme ZoneAnchor -destination 'platform=iOS Simulator,name=iPhone 17,OS=26.5' CODE_SIGNING_ALLOWED=NO
```
