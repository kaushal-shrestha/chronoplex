import XCTest
@testable import ZoneAnchor

final class AlarmMathTests: XCTestCase {
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

    func testMonthlyOrdinalWeekday() {
        var alarm = AlarmEntry(id: 3, zoneId: "UTC", hour: 8, minute: 15, daysMask: 0)
        alarm.repeatType = .monthlyWeekday
        alarm.monthlyOrdinal = -1
        alarm.monthlyWeekday = 1

        let next = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: millis("2026-07-01T00:00:00Z"))

        XCTAssertEqual(next, millis("2026-07-27T08:15:00Z"))
    }

    private func millis(_ iso: String) -> Int64 {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        return Int64(formatter.date(from: iso)!.timeIntervalSince1970 * 1000)
    }
}
