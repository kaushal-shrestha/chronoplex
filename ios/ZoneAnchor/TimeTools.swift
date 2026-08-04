import Foundation

enum TimeZones {
    static let suggested: [String] = [
        TimeZone.current.identifier,
        "America/New_York",
        "Europe/London",
        "Asia/Kathmandu",
        "Asia/Tokyo",
        "Australia/Sydney"
    ]

    static let all: [String] = TimeZone.knownTimeZoneIdentifiers.sorted()

    static func matches(_ query: String) -> [String] {
        let clean = query.trimmed.lowercased()
        guard !clean.isEmpty else { return all }
        return all.filter {
            $0.lowercased().contains(clean) ||
            $0.replacingOccurrences(of: "_", with: " ").lowercased().contains(clean)
        }
    }
}

enum TimeFormat {
    static func clock(_ date: Date, zoneId: String, seconds: Bool = true) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = TimeZone(identifier: zoneId) ?? .current
        formatter.dateStyle = .none
        formatter.timeStyle = seconds ? .medium : .short
        return formatter.string(from: date)
    }

    static func weekdayDate(_ date: Date, zoneId: String) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = TimeZone(identifier: zoneId) ?? .current
        formatter.dateFormat = "EEEE, MMM d"
        return formatter.string(from: date)
    }

    static func nextFire(_ date: Date, zoneId: String) -> String {
        let formatter = DateFormatter()
        formatter.timeZone = TimeZone(identifier: zoneId) ?? .current
        formatter.dateFormat = "EEE, MMM d h:mm a"
        return formatter.string(from: date)
    }

    static func duration(_ millis: Int64) -> String {
        let total = max(0, millis / 1000)
        let hours = total / 3600
        let minutes = (total % 3600) / 60
        let seconds = total % 60
        if hours > 0 {
            return String(format: "%d:%02d:%02d", hours, minutes, seconds)
        }
        return String(format: "%d:%02d", minutes, seconds)
    }

    static func remaining(_ millis: Int64) -> String {
        let totalMinutes = max(1, Int((millis + 59_999) / 60_000))
        if totalMinutes < 60 { return "in \(totalMinutes)m" }
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60
        return minutes == 0 ? "in \(hours)h" : "in \(hours)h \(minutes)m"
    }

    static func repeatLabel(for alarm: AlarmEntry, firstDay: FirstDayOfWeek) -> String {
        switch alarm.effectiveRepeatType {
        case .once:
            return "Once"
        case .weekly:
            let days = orderedWeekdays(firstDay: firstDay).filter { DayMask.contains(alarm.daysMask, weekday: $0) }
            if DayMask.sanitize(alarm.daysMask) == DayMask.everyDay { return "Every day" }
            if DayMask.sanitize(alarm.daysMask) == DayMask.weekdays { return "Weekdays" }
            if DayMask.sanitize(alarm.daysMask) == DayMask.weekends { return "Weekends" }
            let names = days.map(shortWeekdayName).joined(separator: ", ")
            return alarm.repeatInterval == 1 ? "Repeats \(names)" : "Every \(alarm.repeatInterval) weeks on \(names)"
        case .monthlyDay:
            let prefix = alarm.repeatInterval == 1 ? "Monthly" : "Every \(alarm.repeatInterval) months"
            return "\(prefix) on day \(alarm.monthlyDay)"
        case .monthlyWeekday:
            let ordinal = alarm.monthlyOrdinal == -1 ? "last" : ["", "first", "second", "third", "fourth"][alarm.monthlyOrdinal]
            return "Monthly on the \(ordinal) \(longWeekdayName(alarm.monthlyWeekday))"
        }
    }

    static func orderedWeekdays(firstDay: FirstDayOfWeek) -> [Int] {
        firstDay == .monday ? [1, 2, 3, 4, 5, 6, 7] : [7, 1, 2, 3, 4, 5, 6]
    }

    static func shortWeekdayName(_ weekday: Int) -> String {
        ["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][weekday - 1]
    }

    static func longWeekdayName(_ weekday: Int) -> String {
        ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"][weekday - 1]
    }
}

enum AlarmMath {
    static func nextTriggerMillis(for alarm: AlarmEntry, fromMillis: Int64 = Date.millis) -> Int64? {
        guard let zone = TimeZone(identifier: alarm.zoneId) else { return nil }
        let now = Date(millis: fromMillis)
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = zone

        switch alarm.effectiveRepeatType {
        case .once:
            return nextOneShot(alarm, now: now, calendar: calendar)
        case .weekly:
            return nextWeekly(alarm, now: now, calendar: calendar)
        case .monthlyDay:
            return nextMonthlyDay(alarm, now: now, calendar: calendar)
        case .monthlyWeekday:
            return nextMonthlyWeekday(alarm, now: now, calendar: calendar)
        }
    }

    private static func nextOneShot(_ alarm: AlarmEntry, now: Date, calendar: Calendar) -> Int64? {
        for offset in 0...2 {
            guard let date = calendar.date(byAdding: .day, value: offset, to: calendar.startOfDay(for: now)),
                  let candidate = candidateDate(on: date, hour: alarm.hour, minute: alarm.minute, calendar: calendar),
                  candidate > now else { continue }
            return Int64(candidate.timeIntervalSince1970 * 1000)
        }
        return nil
    }

    private static func nextWeekly(_ alarm: AlarmEntry, now: Date, calendar: Calendar) -> Int64? {
        let selectedDays = DayMask.sanitize(alarm.daysMask)
        guard selectedDays != 0 else { return nil }
        let interval = Validate.repeatInterval(alarm.repeatInterval)
        let anchor = startDate(for: alarm, now: now, calendar: calendar)
        let horizon = 8 + interval * 7
        for offset in 0...horizon {
            guard let date = calendar.date(byAdding: .day, value: offset, to: calendar.startOfDay(for: now)) else { continue }
            if date < anchor { continue }
            if !isOnWeeklyInterval(date: date, anchor: anchor, interval: interval, calendar: calendar) { continue }
            guard DayMask.contains(selectedDays, weekday: mondayBasedWeekday(date, calendar: calendar)),
                  let candidate = candidateDate(on: date, hour: alarm.hour, minute: alarm.minute, calendar: calendar),
                  candidate > now else { continue }
            return Int64(candidate.timeIntervalSince1970 * 1000)
        }
        return nil
    }

    private static func nextMonthlyDay(_ alarm: AlarmEntry, now: Date, calendar: Calendar) -> Int64? {
        let anchor = startDate(for: alarm, now: now, calendar: calendar)
        let interval = Validate.repeatInterval(alarm.repeatInterval)
        let day = Validate.monthlyDay(alarm.monthlyDay)
        let firstMonth = monthStart(for: maxDate(calendar.startOfDay(for: now), anchor), calendar: calendar)
        for offset in 0...2400 {
            guard let month = calendar.date(byAdding: .month, value: offset, to: firstMonth) else { continue }
            if !isOnMonthlyInterval(month: month, anchor: anchor, interval: interval, calendar: calendar) { continue }
            let range = calendar.range(of: .day, in: .month, for: month) ?? 1..<32
            if !range.contains(day) { continue }
            var comps = calendar.dateComponents([.year, .month], from: month)
            comps.day = day
            guard let date = calendar.date(from: comps), date >= anchor,
                  let candidate = candidateDate(on: date, hour: alarm.hour, minute: alarm.minute, calendar: calendar),
                  candidate > now else { continue }
            return Int64(candidate.timeIntervalSince1970 * 1000)
        }
        return nil
    }

    private static func nextMonthlyWeekday(_ alarm: AlarmEntry, now: Date, calendar: Calendar) -> Int64? {
        let anchor = startDate(for: alarm, now: now, calendar: calendar)
        let interval = Validate.repeatInterval(alarm.repeatInterval)
        let weekday = Validate.monthlyWeekday(alarm.monthlyWeekday)
        let ordinal = Validate.monthlyOrdinal(alarm.monthlyOrdinal)
        let firstMonth = monthStart(for: maxDate(calendar.startOfDay(for: now), anchor), calendar: calendar)
        for offset in 0...2400 {
            guard let month = calendar.date(byAdding: .month, value: offset, to: firstMonth) else { continue }
            if !isOnMonthlyInterval(month: month, anchor: anchor, interval: interval, calendar: calendar) { continue }
            guard let date = dateForMonthlyWeekday(month: month, weekday: weekday, ordinal: ordinal, calendar: calendar),
                  date >= anchor,
                  let candidate = candidateDate(on: date, hour: alarm.hour, minute: alarm.minute, calendar: calendar),
                  candidate > now else { continue }
            return Int64(candidate.timeIntervalSince1970 * 1000)
        }
        return nil
    }

    private static func candidateDate(on date: Date, hour: Int, minute: Int, calendar: Calendar) -> Date? {
        var comps = calendar.dateComponents([.year, .month, .day], from: date)
        comps.hour = Validate.hour(hour)
        comps.minute = Validate.minute(minute)
        comps.second = 0
        comps.nanosecond = 0
        comps.timeZone = calendar.timeZone
        guard let candidate = calendar.date(from: comps) else { return nil }
        let roundtrip = calendar.dateComponents([.year, .month, .day, .hour, .minute], from: candidate)
        guard roundtrip.year == comps.year,
              roundtrip.month == comps.month,
              roundtrip.day == comps.day,
              roundtrip.hour == comps.hour,
              roundtrip.minute == comps.minute else {
            return nil
        }
        return candidate
    }

    private static func startDate(for alarm: AlarmEntry, now: Date, calendar: Calendar) -> Date {
        guard let parsed = ISO8601DateFormatter.dateOnly.date(from: alarm.repeatStartDate) else {
            return calendar.startOfDay(for: now)
        }
        return calendar.startOfDay(for: parsed)
    }

    private static func isOnWeeklyInterval(date: Date, anchor: Date, interval: Int, calendar: Calendar) -> Bool {
        let dateWeek = weekStart(for: date, calendar: calendar)
        let anchorWeek = weekStart(for: anchor, calendar: calendar)
        let weeks = calendar.dateComponents([.weekOfYear], from: anchorWeek, to: dateWeek).weekOfYear ?? 0
        return weeks >= 0 && weeks % interval == 0
    }

    private static func isOnMonthlyInterval(month: Date, anchor: Date, interval: Int, calendar: Calendar) -> Bool {
        let months = calendar.dateComponents([.month], from: monthStart(for: anchor, calendar: calendar), to: monthStart(for: month, calendar: calendar)).month ?? 0
        return months >= 0 && months % interval == 0
    }

    private static func dateForMonthlyWeekday(month: Date, weekday: Int, ordinal: Int, calendar: Calendar) -> Date? {
        let range = calendar.range(of: .day, in: .month, for: month) ?? 1..<32
        let candidates = range.compactMap { day -> Date? in
            var comps = calendar.dateComponents([.year, .month], from: month)
            comps.day = day
            guard let date = calendar.date(from: comps) else { return nil }
            return mondayBasedWeekday(date, calendar: calendar) == weekday ? date : nil
        }
        guard !candidates.isEmpty else { return nil }
        if ordinal == -1 { return candidates.last }
        let index = ordinal - 1
        return candidates.indices.contains(index) ? candidates[index] : nil
    }

    private static func mondayBasedWeekday(_ date: Date, calendar: Calendar) -> Int {
        let system = calendar.component(.weekday, from: date)
        return system == 1 ? 7 : system - 1
    }

    private static func weekStart(for date: Date, calendar: Calendar) -> Date {
        var cal = calendar
        cal.firstWeekday = 2
        return cal.dateInterval(of: .weekOfYear, for: date)?.start ?? cal.startOfDay(for: date)
    }

    private static func monthStart(for date: Date, calendar: Calendar) -> Date {
        let comps = calendar.dateComponents([.year, .month], from: date)
        return calendar.date(from: comps) ?? calendar.startOfDay(for: date)
    }

    private static func maxDate(_ lhs: Date, _ rhs: Date) -> Date {
        lhs > rhs ? lhs : rhs
    }
}

extension ISO8601DateFormatter {
    static let dateOnly: ISO8601DateFormatter = {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withFullDate]
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        return formatter
    }()
}
