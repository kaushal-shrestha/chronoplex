import Foundation
import SwiftUI

enum EntityKind: String, Codable, CaseIterable {
    case clocks
    case alarms
    case timers
    case stopwatches
}

struct ItemGroup: Identifiable, Codable, Hashable {
    var id: Int64 = Date.millis
    var name: String
    var sortOrder: Int64 = Date.millis
    var collapsed: Bool = false
}

struct ClockEntry: Identifiable, Codable, Hashable {
    var id: Int64 = Date.millis
    var label: String
    var zoneId: String
    var sortOrder: Int64 = Date.millis
    var groupId: Int64?

    var displayLabel: String {
        let clean = label.trimmed
        return clean.isEmpty ? zoneId.replacingOccurrences(of: "_", with: " ") : clean
    }
}

enum AlarmRepeatType: String, Codable, CaseIterable, Identifiable {
    case once
    case weekly
    case monthlyDay
    case monthlyWeekday

    var id: String { rawValue }
    var title: String {
        switch self {
        case .once: "Once"
        case .weekly: "Weekly"
        case .monthlyDay: "Monthly by day"
        case .monthlyWeekday: "Monthly by weekday"
        }
    }
}

struct AlarmEntry: Identifiable, Codable, Hashable {
    var id: Int64 = Date.millis
    var label: String = ""
    var zoneId: String = TimeZone.current.identifier
    var hour: Int = 7
    var minute: Int = 0
    var daysMask: Int = DayMask.weekdays
    var soundEnabled: Bool = true
    var vibrationEnabled: Bool = true
    var enabled: Bool = true
    var snoozeUntilMillis: Int64?
    var groupId: Int64?
    var clockId: Int64?
    var repeatType: AlarmRepeatType = .weekly
    var repeatInterval: Int = 1
    var repeatStartDate: String = ""
    var monthlyDay: Int = 1
    var monthlyOrdinal: Int = 1
    var monthlyWeekday: Int = 1

    var effectiveRepeatType: AlarmRepeatType {
        repeatType == .weekly && DayMask.sanitize(daysMask) == 0 ? .once : repeatType
    }

    func isSnoozed(nowMillis: Int64 = Date.millis) -> Bool {
        (snoozeUntilMillis ?? 0) > nowMillis
    }
}

enum AppearanceMode: String, Codable, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }
    var title: String {
        switch self {
        case .system: "Follow system"
        case .light: "Light"
        case .dark: "Dark"
        }
    }

    var colorScheme: ColorScheme? {
        switch self {
        case .system: nil
        case .light: .light
        case .dark: .dark
        }
    }
}

enum TimerState: String, Codable {
    case idle
    case running
    case paused
    case finished
}

enum TimerFinishMode: String, Codable, CaseIterable, Identifiable {
    case notification
    case fullScreen

    var id: String { rawValue }
    var title: String {
        switch self {
        case .notification: "Notification"
        case .fullScreen: "Open app alert"
        }
    }
}

struct ChronoTimer: Identifiable, Codable, Hashable {
    var id: Int64 = Date.millis
    var label: String = ""
    var durationMillis: Int64
    var state: TimerState = .idle
    var endsAtMillis: Int64?
    var pausedRemainingMillis: Int64?
    var finishMode: TimerFinishMode = .notification
    var sortOrder: Int64 = Date.millis
    var groupId: Int64?

    func remainingMillis(nowMillis: Int64 = Date.millis) -> Int64 {
        switch state {
        case .idle:
            return durationMillis
        case .running:
            return max(0, (endsAtMillis ?? nowMillis) - nowMillis)
        case .paused:
            return pausedRemainingMillis ?? durationMillis
        case .finished:
            return 0
        }
    }
}

enum StopwatchState: String, Codable {
    case idle
    case running
    case paused
}

struct StopwatchEntry: Identifiable, Codable, Hashable {
    var id: Int64 = Date.millis
    var label: String = ""
    var state: StopwatchState = .idle
    var startedAtMillis: Int64?
    var accumulatedMillis: Int64 = 0
    var sortOrder: Int64 = Date.millis
    var groupId: Int64?

    func elapsedMillis(nowMillis: Int64 = Date.millis) -> Int64 {
        switch state {
        case .idle:
            return 0
        case .paused:
            return accumulatedMillis
        case .running:
            return accumulatedMillis + max(0, nowMillis - (startedAtMillis ?? nowMillis))
        }
    }
}

struct StopwatchLap: Identifiable, Codable, Hashable {
    var id: Int64 = Date.millis
    var stopwatchId: Int64
    var lapNumber: Int
    var totalElapsedMillis: Int64
}

enum ThemePalette: String, Codable, CaseIterable, Identifiable {
    case anchor
    case daybreak
    case harbor
    case grove
    case ember
    case twilight
    case sunrise
    case forest
    case slate
    case plum

    var id: String { rawValue }
    var title: String {
        switch self {
        case .anchor: "Anchor"
        case .daybreak: "Daybreak"
        case .harbor: "Harbor"
        case .grove: "Grove"
        case .ember: "Ember"
        case .twilight: "Twilight"
        case .sunrise: "Sunrise"
        case .forest: "Forest"
        case .slate: "Slate"
        case .plum: "Plum"
        }
    }

    var accent: Color {
        switch self {
        case .anchor: .blue
        case .daybreak: .teal
        case .harbor: .cyan
        case .grove: .green
        case .ember: .pink
        case .twilight: .indigo
        case .sunrise: .orange
        case .forest: .mint
        case .slate: .gray
        case .plum: .purple
        }
    }
}

enum ZoneSource: String, Codable, CaseIterable, Identifiable {
    case myClocks
    case allZones

    var id: String { rawValue }
    var title: String {
        switch self {
        case .myClocks: "My clocks"
        case .allZones: "All zones"
        }
    }
}

enum AlarmZoneDisplay: String, Codable, CaseIterable, Identifiable {
    case clockLabel
    case zoneId

    var id: String { rawValue }
    var title: String {
        switch self {
        case .clockLabel: "Clock label"
        case .zoneId: "Zone ID"
        }
    }
}

enum FirstDayOfWeek: String, Codable, CaseIterable, Identifiable {
    case monday
    case sunday

    var id: String { rawValue }
    var title: String {
        switch self {
        case .monday: "Monday"
        case .sunday: "Sunday"
        }
    }
}

struct AppSettings: Codable, Hashable {
    var appearance: AppearanceMode = .system
    var palette: ThemePalette = .anchor
    var defaultZoneSource: ZoneSource = .myClocks
    var alarmZoneDisplay: AlarmZoneDisplay = .clockLabel
    var firstDayOfWeek: FirstDayOfWeek = .monday
    var defaultTimerFinishMode: TimerFinishMode = .notification
    var groupedClocks: Bool = false
    var groupedAlarms: Bool = false
    var groupedTimers: Bool = false
    var groupedStopwatches: Bool = false
}

enum DayMask {
    static let allBits = 0b1111111
    static let weekdays = mask([1, 2, 3, 4, 5])
    static let weekends = mask([6, 7])
    static let everyDay = mask([1, 2, 3, 4, 5, 6, 7])

    static func mask(_ days: [Int]) -> Int {
        days.reduce(0) { $0 | (1 << ($1 - 1)) }
    }

    static func sanitize(_ mask: Int) -> Int {
        mask & allBits
    }

    static func contains(_ mask: Int, weekday: Int) -> Bool {
        ((sanitize(mask) >> (weekday - 1)) & 1) == 1
    }

    static func toggle(_ mask: Int, weekday: Int) -> Int {
        sanitize(mask) ^ (1 << (weekday - 1))
    }

    static func days(_ mask: Int) -> [Int] {
        (1...7).filter { contains(mask, weekday: $0) }
    }
}

enum Validate {
    static func label(_ value: String?) -> String {
        String((value ?? "").trimmed.prefix(80))
    }

    static func zoneId(_ value: String?) -> String {
        guard let value, TimeZone(identifier: value) != nil else {
            return TimeZone.current.identifier
        }
        return value
    }

    static func hour(_ value: Int) -> Int { min(23, max(0, value)) }
    static func minute(_ value: Int) -> Int { min(59, max(0, value)) }
    static func repeatInterval(_ value: Int) -> Int { min(99, max(1, value)) }
    static func monthlyDay(_ value: Int) -> Int { min(31, max(1, value)) }
    static func monthlyOrdinal(_ value: Int) -> Int { value == -1 ? -1 : min(4, max(1, value)) }
    static func monthlyWeekday(_ value: Int) -> Int { min(7, max(1, value)) }
}

extension String {
    var trimmed: String {
        trimmingCharacters(in: .whitespacesAndNewlines)
    }
}

extension Date {
    static var millis: Int64 {
        Int64(Date().timeIntervalSince1970 * 1000)
    }

    init(millis: Int64) {
        self = Date(timeIntervalSince1970: TimeInterval(millis) / 1000)
    }
}

extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
