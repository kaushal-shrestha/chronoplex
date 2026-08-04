import XCTest
@testable import ZoneAnchor

final class AlarmMathTests: XCTestCase {
    func testDayMaskConversionsAndToggle() {
        let mask = DayMask.mask([1, 5, 7])

        XCTAssertEqual(mask, 0b1010001)
        XCTAssertTrue(DayMask.contains(mask, weekday: 5))
        XCTAssertFalse(DayMask.contains(mask, weekday: 6))
        XCTAssertEqual(DayMask.sanitize(mask | (1 << 12)), mask)
        XCTAssertEqual(DayMask.toggle(mask, weekday: 5), DayMask.mask([1, 7]))
        XCTAssertEqual(DayMask.days(mask), [1, 5, 7])
        XCTAssertEqual(DayMask.weekdays, DayMask.mask([1, 2, 3, 4, 5]))
        XCTAssertEqual(DayMask.weekends, DayMask.mask([6, 7]))
        XCTAssertEqual(DayMask.everyDay, DayMask.mask([1, 2, 3, 4, 5, 6, 7]))
    }

    func testValidationHelpersClampPersistedValues() {
        XCTAssertEqual(Validate.label("   wake up   "), "wake up")
        XCTAssertEqual(Validate.label(nil), "")
        XCTAssertEqual(Validate.label(String(repeating: "x", count: 120)).count, 80)
        XCTAssertEqual(Validate.zoneId("America/New_York"), "America/New_York")
        XCTAssertNotEqual(Validate.zoneId("Mars/Olympus_Mons"), "Mars/Olympus_Mons")
        XCTAssertEqual(Validate.hour(-3), 0)
        XCTAssertEqual(Validate.hour(30), 23)
        XCTAssertEqual(Validate.minute(-3), 0)
        XCTAssertEqual(Validate.minute(90), 59)
        XCTAssertEqual(Validate.repeatInterval(-1), 1)
        XCTAssertEqual(Validate.repeatInterval(200), 99)
        XCTAssertEqual(Validate.monthlyDay(-1), 1)
        XCTAssertEqual(Validate.monthlyDay(80), 31)
        XCTAssertEqual(Validate.monthlyOrdinal(-1), -1)
        XCTAssertEqual(Validate.monthlyOrdinal(0), 1)
        XCTAssertEqual(Validate.monthlyOrdinal(99), 4)
        XCTAssertEqual(Validate.monthlyWeekday(0), 1)
        XCTAssertEqual(Validate.monthlyWeekday(99), 7)
    }

    func testModelDerivedFields() {
        let clock = ClockEntry(label: "   ", zoneId: "America/New_York")
        let namedClock = ClockEntry(label: "Home", zoneId: "America/New_York")
        var alarm = AlarmEntry(id: 1, zoneId: "UTC", hour: 8, minute: 0, daysMask: 0)
        alarm.repeatType = .weekly
        alarm.snoozeUntilMillis = 2_000

        XCTAssertEqual(clock.displayLabel, "America/New York")
        XCTAssertEqual(namedClock.displayLabel, "Home")
        XCTAssertEqual(alarm.effectiveRepeatType, .once)
        XCTAssertTrue(alarm.isSnoozed(nowMillis: 1_999))
        XCTAssertFalse(alarm.isSnoozed(nowMillis: 2_000))
    }

    func testEnumTitlesAndSettingsDefaults() {
        XCTAssertEqual(EntityKind.allCases, [.clocks, .alarms, .timers, .stopwatches])
        XCTAssertEqual(AlarmRepeatType.allCases.map(\.title), ["Once", "Weekly", "Monthly by day", "Monthly by weekday"])
        XCTAssertEqual(AppearanceMode.allCases.map(\.title), ["Follow system", "Light", "Dark"])
        XCTAssertNil(AppearanceMode.system.colorScheme)
        XCTAssertEqual(AppearanceMode.light.colorScheme, .light)
        XCTAssertEqual(AppearanceMode.dark.colorScheme, .dark)
        XCTAssertEqual(TimerFinishMode.allCases.map(\.title), ["Notification", "Open app alert"])
        XCTAssertEqual(ThemePalette.allCases.map(\.title), [
            "Anchor",
            "Daybreak",
            "Harbor",
            "Grove",
            "Ember",
            "Twilight",
            "Sunrise",
            "Forest",
            "Slate",
            "Plum"
        ])
        ThemePalette.allCases.forEach { _ = $0.accent }
        XCTAssertEqual(ZoneSource.allCases.map(\.title), ["My clocks", "All zones"])
        XCTAssertEqual(AlarmZoneDisplay.allCases.map(\.title), ["Clock label", "Zone ID"])
        XCTAssertEqual(FirstDayOfWeek.allCases.map(\.title), ["Monday", "Sunday"])

        let settings = AppSettings()
        XCTAssertEqual(settings.appearance, .system)
        XCTAssertEqual(settings.palette, .anchor)
        XCTAssertEqual(settings.defaultZoneSource, .myClocks)
        XCTAssertEqual(settings.alarmZoneDisplay, .clockLabel)
        XCTAssertEqual(settings.firstDayOfWeek, .monday)
        XCTAssertEqual(settings.defaultTimerFinishMode, .notification)
        XCTAssertFalse(settings.groupedClocks)
        XCTAssertFalse(settings.groupedAlarms)
        XCTAssertFalse(settings.groupedTimers)
        XCTAssertFalse(settings.groupedStopwatches)
    }

    func testTimerAndStopwatchMath() {
        XCTAssertEqual(ChronoTimer(label: "Idle", durationMillis: 90_000).remainingMillis(nowMillis: 10_000), 90_000)
        XCTAssertEqual(
            ChronoTimer(
                label: "Running",
                durationMillis: 90_000,
                state: .running,
                endsAtMillis: 40_000
            ).remainingMillis(nowMillis: 10_000),
            30_000
        )
        XCTAssertEqual(
            ChronoTimer(
                label: "Overdue",
                durationMillis: 90_000,
                state: .running,
                endsAtMillis: 5_000
            ).remainingMillis(nowMillis: 10_000),
            0
        )
        XCTAssertEqual(
            ChronoTimer(
                label: "Paused",
                durationMillis: 90_000,
                state: .paused,
                pausedRemainingMillis: 12_345
            ).remainingMillis(nowMillis: 10_000),
            12_345
        )
        XCTAssertEqual(
            ChronoTimer(
                label: "Paused fallback",
                durationMillis: 90_000,
                state: .paused
            ).remainingMillis(nowMillis: 10_000),
            90_000
        )
        XCTAssertEqual(
            ChronoTimer(
                label: "Done",
                durationMillis: 90_000,
                state: .finished
            ).remainingMillis(nowMillis: 10_000),
            0
        )

        XCTAssertEqual(StopwatchEntry(label: "Idle").elapsedMillis(nowMillis: 10_000), 0)
        XCTAssertEqual(
            StopwatchEntry(
                label: "Paused",
                state: .paused,
                accumulatedMillis: 12_000
            ).elapsedMillis(nowMillis: 20_000),
            12_000
        )
        XCTAssertEqual(
            StopwatchEntry(
                label: "Running",
                state: .running,
                startedAtMillis: 10_000,
                accumulatedMillis: 5_000
            ).elapsedMillis(nowMillis: 20_000),
            15_000
        )
        XCTAssertEqual(
            StopwatchEntry(
                label: "Future start",
                state: .running,
                startedAtMillis: 30_000,
                accumulatedMillis: 5_000
            ).elapsedMillis(nowMillis: 20_000),
            5_000
        )
    }

    func testTimeFormattingHelpers() {
        let date = Date(millis: millis("2026-08-03T20:00:00Z"))
        XCTAssertFalse(TimeFormat.clock(date, zoneId: "UTC", seconds: true).isEmpty)
        XCTAssertFalse(TimeFormat.clock(date, zoneId: "UTC", seconds: false).isEmpty)
        XCTAssertFalse(TimeFormat.clock(date, zoneId: "Mars/Olympus_Mons", seconds: true).isEmpty)
        XCTAssertFalse(TimeFormat.weekdayDate(date, zoneId: "UTC").isEmpty)
        XCTAssertFalse(TimeFormat.weekdayDate(date, zoneId: "Mars/Olympus_Mons").isEmpty)
        XCTAssertFalse(TimeFormat.nextFire(date, zoneId: "UTC").isEmpty)
        XCTAssertFalse(TimeFormat.nextFire(date, zoneId: "Mars/Olympus_Mons").isEmpty)
        XCTAssertEqual(TimeFormat.duration(-1_000), "0:00")
        XCTAssertEqual(TimeFormat.duration(59_000), "0:59")
        XCTAssertEqual(TimeFormat.duration(61_000), "1:01")
        XCTAssertEqual(TimeFormat.duration(3_661_000), "1:01:01")
        XCTAssertEqual(TimeFormat.remaining(1), "in 1m")
        XCTAssertEqual(TimeFormat.remaining(60 * 60_000), "in 1h")
        XCTAssertEqual(TimeFormat.remaining(75 * 60_000), "in 1h 15m")
        XCTAssertEqual(TimeFormat.orderedWeekdays(firstDay: .monday), [1, 2, 3, 4, 5, 6, 7])
        XCTAssertEqual(TimeFormat.orderedWeekdays(firstDay: .sunday), [7, 1, 2, 3, 4, 5, 6])
        XCTAssertEqual(TimeFormat.shortWeekdayName(1), "Mon")
        XCTAssertEqual(TimeFormat.longWeekdayName(7), "Sunday")
    }

    func testRepeatLabels() {
        var alarm = AlarmEntry(id: 1, zoneId: "UTC", hour: 8, minute: 0, daysMask: 0)
        alarm.repeatType = .once
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Once")

        alarm.repeatType = .weekly
        alarm.daysMask = DayMask.everyDay
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Every day")

        alarm.daysMask = DayMask.weekdays
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Weekdays")

        alarm.daysMask = DayMask.weekends
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Weekends")

        alarm.daysMask = DayMask.mask([1, 3])
        alarm.repeatInterval = 1
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Repeats Mon, Wed")

        alarm.repeatInterval = 2
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .sunday), "Every 2 weeks on Mon, Wed")

        alarm.repeatType = .monthlyDay
        alarm.repeatInterval = 1
        alarm.monthlyDay = 8
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Monthly on day 8")

        alarm.repeatInterval = 3
        alarm.monthlyDay = 12
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Every 3 months on day 12")

        alarm.repeatType = .monthlyWeekday
        alarm.monthlyOrdinal = 2
        alarm.monthlyWeekday = 1
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Monthly on the second Monday")

        alarm.monthlyOrdinal = -1
        alarm.monthlyWeekday = 5
        XCTAssertEqual(TimeFormat.repeatLabel(for: alarm, firstDay: .monday), "Monthly on the last Friday")
    }

    func testZoneSearchAndSmallExtensions() {
        XCTAssertFalse(TimeZones.matches("").isEmpty)
        XCTAssertTrue(TimeZones.matches("America/New_York").contains("America/New_York"))
        XCTAssertTrue(TimeZones.matches("new_york").contains("America/New_York"))
        XCTAssertTrue(TimeZones.matches("New York").contains("America/New_York"))
        XCTAssertEqual("  hello  \n".trimmed, "hello")
        XCTAssertEqual(Date(millis: 1_500).timeIntervalSince1970, 1.5, accuracy: 0.001)
        XCTAssertEqual(["UTC", "UTC", "America/New_York"].uniqued(), ["UTC", "America/New_York"])
    }

    func testOneShotAlarmUsesTheAlarmZoneWallClock() {
        var alarm = AlarmEntry(id: 4, zoneId: "America/New_York", hour: 16, minute: 0, daysMask: 0)
        alarm.repeatType = .once

        let nextBefore = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-08-03T18:00:00Z"))
        let nextAfter = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-08-03T21:00:00Z"))

        XCTAssertEqual(nextBefore, millis("2026-08-03T20:00:00Z"))
        XCTAssertEqual(nextAfter, millis("2026-08-04T20:00:00Z"))
    }

    func testDefaultAlarmMathStartTimeReturnsAFutureTrigger() {
        var alarm = AlarmEntry(id: 10, zoneId: "UTC", hour: 23, minute: 59, daysMask: DayMask.everyDay)
        alarm.repeatType = .weekly

        let before = Date.millis
        let next = AlarmMath.nextTriggerMillis(for: alarm)

        XCTAssertNotNil(next)
        XCTAssertGreaterThan(next ?? 0, before)
    }

    func testSpringForwardGapSkipsNonexistentWallTime() {
        var alarm = AlarmEntry(id: 1, zoneId: "America/New_York", hour: 2, minute: 30, daysMask: DayMask.mask([7]))
        alarm.repeatType = .weekly

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-03-08T05:00:00Z"))

        XCTAssertEqual(next, millis("2026-03-15T06:30:00Z"))
    }

    func testWeeklyIntervalUsesStartDateAnchor() {
        var alarm = AlarmEntry(id: 2, zoneId: "UTC", hour: 9, minute: 0, daysMask: DayMask.mask([1]))
        alarm.repeatType = .weekly
        alarm.repeatInterval = 2
        alarm.repeatStartDate = "2026-01-05"

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-01-12T00:00:00Z"))

        XCTAssertEqual(next, millis("2026-01-19T09:00:00Z"))
    }

    func testWeeklyFutureAnchorOutsideSearchHorizonHasNoTriggerYet() {
        var alarm = AlarmEntry(id: 11, zoneId: "UTC", hour: 9, minute: 0, daysMask: DayMask.mask([1]))
        alarm.repeatType = .weekly
        alarm.repeatStartDate = "2099-01-01"

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-01-01T00:00:00Z"))

        XCTAssertNil(next)
    }

    func testMonthlyOrdinalWeekday() {
        var alarm = AlarmEntry(id: 3, zoneId: "UTC", hour: 8, minute: 15, daysMask: 0)
        alarm.repeatType = .monthlyWeekday
        alarm.monthlyOrdinal = -1
        alarm.monthlyWeekday = 1

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-07-01T00:00:00Z"))

        XCTAssertEqual(next, millis("2026-07-27T08:15:00Z"))
    }

    func testInvalidZoneReturnsNil() {
        let alarm = AlarmEntry(id: 4, zoneId: "Mars/Olympus_Mons", hour: 8, minute: 0, daysMask: DayMask.everyDay)

        XCTAssertNil(AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-01-01T00:00:00Z")))
    }

    func testWeeklyAlarmWithOnlyGarbageHighBitsFallsBackToOneShot() {
        var alarm = AlarmEntry(id: 5, zoneId: "UTC", hour: 8, minute: 0, daysMask: 1 << 12)
        alarm.repeatType = .weekly

        XCTAssertEqual(
            AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-01-01T00:00:00Z")),
            millis("2026-01-01T08:00:00Z")
        )
    }

    func testMonthlyDaySkipsMonthsWithoutThatDate() {
        var alarm = AlarmEntry(id: 6, zoneId: "UTC", hour: 8, minute: 0, daysMask: 0)
        alarm.repeatType = .monthlyDay
        alarm.repeatStartDate = "2026-01-01"
        alarm.monthlyDay = 31

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-01-31T09:00:00Z"))

        XCTAssertEqual(next, millis("2026-03-31T08:00:00Z"))
    }

    func testMonthlyDayIntervalUsesAnchorMonth() {
        var alarm = AlarmEntry(id: 7, zoneId: "UTC", hour: 9, minute: 30, daysMask: 0)
        alarm.repeatType = .monthlyDay
        alarm.repeatStartDate = "2026-08-03"
        alarm.repeatInterval = 6
        alarm.monthlyDay = 3

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-09-01T09:00:00Z"))

        XCTAssertEqual(next, millis("2027-02-03T09:30:00Z"))
    }

    func testMonthlyWeekdaySkipsAnchorMonthWhenCandidateIsBeforeStartDate() {
        var alarm = AlarmEntry(id: 8, zoneId: "UTC", hour: 10, minute: 0, daysMask: 0)
        alarm.repeatType = .monthlyWeekday
        alarm.repeatStartDate = "2026-08-03"
        alarm.monthlyOrdinal = 1
        alarm.monthlyWeekday = 7

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-08-04T09:00:00Z"))

        XCTAssertEqual(next, millis("2026-09-06T10:00:00Z"))
    }

    func testMonthlyWeekdayClampsCorruptOrdinalAndWeekday() {
        var alarm = AlarmEntry(id: 9, zoneId: "UTC", hour: 6, minute: 30, daysMask: 0)
        alarm.repeatType = .monthlyWeekday
        alarm.repeatStartDate = "2026-07-01"
        alarm.monthlyOrdinal = 99
        alarm.monthlyWeekday = 99

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-07-01T09:00:00Z"))

        XCTAssertEqual(next, millis("2026-07-26T06:30:00Z"))
    }

    private func millis(_ iso: String) -> Int64 {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return Int64(formatter.date(from: iso)!.timeIntervalSince1970 * 1000)
    }
}
