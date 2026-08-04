import Foundation
import SwiftUI
import UserNotifications

private struct ZoneAnchorSnapshot: Codable {
    var nextId: Int64 = 1
    var clocks: [ClockEntry] = []
    var alarms: [AlarmEntry] = []
    var timers: [ChronoTimer] = []
    var stopwatches: [StopwatchEntry] = []
    var laps: [StopwatchLap] = []
    var clockGroups: [ItemGroup] = []
    var alarmGroups: [ItemGroup] = []
    var timerGroups: [ItemGroup] = []
    var stopwatchGroups: [ItemGroup] = []
    var settings: AppSettings = AppSettings()
}

struct ZoneAnchorBackupDocument: Codable {
    var app: String = "zoneanchor"
    var schemaVersion: Int = 1
    var exportedAt: Date = Date()
    var settings: AppSettings = AppSettings()
    var groups: Groups = Groups()
    var clocks: [ClockEntry] = []
    var alarms: [AlarmEntry] = []
    var timers: [ChronoTimer] = []
    var stopwatches: [StopwatchEntry] = []
    var laps: [StopwatchLap] = []

    struct Groups: Codable {
        var clocks: [ItemGroup] = []
        var alarms: [ItemGroup] = []
        var timers: [ItemGroup] = []
        var stopwatches: [ItemGroup] = []
    }
}

@MainActor
final class ZoneAnchorStore: ObservableObject {
    @Published var clocks: [ClockEntry] = [] { didSet { persistAndReschedule() } }
    @Published var alarms: [AlarmEntry] = [] { didSet { persistAndReschedule() } }
    @Published var timers: [ChronoTimer] = [] { didSet { persistAndReschedule() } }
    @Published var stopwatches: [StopwatchEntry] = [] { didSet { persist() } }
    @Published var laps: [StopwatchLap] = [] { didSet { persist() } }
    @Published var clockGroups: [ItemGroup] = [] { didSet { persist() } }
    @Published var alarmGroups: [ItemGroup] = [] { didSet { persist() } }
    @Published var timerGroups: [ItemGroup] = [] { didSet { persist() } }
    @Published var stopwatchGroups: [ItemGroup] = [] { didSet { persist() } }
    @Published var settings = AppSettings() { didSet { persistAndReschedule() } }
    @Published var notificationsAuthorized = false

    private var nextId: Int64 = 1
    private let key = "zoneanchor.snapshot.v1"
    private var isLoading = true

    init() {
        NotificationScheduler.configureCategories()
        NotificationRouter.shared.store = self
        load()
        isLoading = false
        if clocks.isEmpty {
            resetSuggestedClocks()
        }
        refreshNotificationStatus()
        rescheduleAllNotifications()
    }

    func refreshNotificationStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { [weak self] settings in
            Task { @MainActor in
                self?.notificationsAuthorized = settings.authorizationStatus == .authorized ||
                    settings.authorizationStatus == .provisional ||
                    settings.authorizationStatus == .ephemeral
            }
        }
    }

    func requestNotifications() {
        NotificationScheduler.requestAuthorization { [weak self] in
            Task { @MainActor in
                self?.refreshNotificationStatus()
                self?.rescheduleAllNotifications()
            }
        }
    }

    func exportBackupData() throws -> Data {
        let document = ZoneAnchorBackupDocument(
            settings: settings,
            groups: ZoneAnchorBackupDocument.Groups(
                clocks: clockGroups,
                alarms: alarmGroups,
                timers: timerGroups,
                stopwatches: stopwatchGroups
            ),
            clocks: clocks,
            alarms: alarms,
            timers: timers,
            stopwatches: stopwatches,
            laps: laps
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        encoder.dateEncodingStrategy = .iso8601
        return try encoder.encode(document)
    }

    func importBackupData(_ data: Data) throws -> String {
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        let document = try decoder.decode(ZoneAnchorBackupDocument.self, from: data)
        guard document.schemaVersion <= 1 else {
            throw BackupError.unsupportedVersion
        }
        isLoading = true
        settings = document.settings
        clockGroups = document.groups.clocks.sorted { $0.sortOrder < $1.sortOrder }
        alarmGroups = document.groups.alarms.sorted { $0.sortOrder < $1.sortOrder }
        timerGroups = document.groups.timers.sorted { $0.sortOrder < $1.sortOrder }
        stopwatchGroups = document.groups.stopwatches.sorted { $0.sortOrder < $1.sortOrder }
        clocks = document.clocks.map(cleanClock).sorted { $0.sortOrder < $1.sortOrder }
        alarms = document.alarms.map(cleanAlarm).sorted(by: alarmSort)
        timers = document.timers.map(cleanTimer).sorted { $0.sortOrder < $1.sortOrder }
        stopwatches = document.stopwatches.map(cleanStopwatch).sorted { $0.sortOrder < $1.sortOrder }
        laps = document.laps.sorted {
            if $0.stopwatchId != $1.stopwatchId { return $0.stopwatchId < $1.stopwatchId }
            return $0.lapNumber < $1.lapNumber
        }
        var importedMaxId = Int64(0)
        for id in clocks.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in alarms.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in timers.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in stopwatches.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in laps.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in clockGroups.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in alarmGroups.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in timerGroups.map(\.id) { importedMaxId = max(importedMaxId, id) }
        for id in stopwatchGroups.map(\.id) { importedMaxId = max(importedMaxId, id) }
        nextId = max(nextId, importedMaxId + 1)
        isLoading = false
        persistAndReschedule()
        return "Imported \(clocks.count) clocks, \(alarms.count) alarms, \(timers.count) timers, \(stopwatches.count) stopwatches."
    }

    // MARK: - Clocks

    func upsertClock(_ clock: ClockEntry) {
        var clean = clock
        clean.id = clean.id == 0 ? allocateId() : clean.id
        clean.label = Validate.label(clean.label)
        clean.zoneId = Validate.zoneId(clean.zoneId)
        clean.sortOrder = clean.sortOrder == 0 ? Date.millis : clean.sortOrder
        if let index = clocks.firstIndex(where: { $0.id == clean.id }) {
            let old = clocks[index]
            clocks[index] = clean
            if old.zoneId != clean.zoneId {
                alarms = alarms.map { alarm in
                    alarm.clockId == clean.id ? alarm.detachedFromClock() : alarm
                }
            }
        } else {
            clocks.append(clean)
        }
        sortClocks()
    }

    func deleteClock(_ id: Int64) {
        clocks.removeAll { $0.id == id }
        alarms = alarms.map { $0.clockId == id ? $0.detachedFromClock() : $0 }
    }

    func deleteAllClocks() {
        clocks.removeAll()
        alarms = alarms.map { $0.detachedFromClock() }
    }

    func resetSuggestedClocks() {
        let existingGroups = clockGroups
        clocks = TimeZones.suggested.uniqued().enumerated().map { offset, zone in
            ClockEntry(
                id: allocateId(),
                label: zone == TimeZone.current.identifier ? "Device zone" : zone.cityLabel,
                zoneId: Validate.zoneId(zone),
                sortOrder: Int64(offset)
            )
        }
        clockGroups = existingGroups
    }

    func reorderClocks(from source: IndexSet, to destination: Int) {
        clocks.move(fromOffsets: source, toOffset: destination)
        for index in clocks.indices { clocks[index].sortOrder = Int64(index) }
        sortClocks()
    }

    // MARK: - Alarms

    func upsertAlarm(_ alarm: AlarmEntry) {
        var clean = cleanAlarm(alarm)
        clean.id = clean.id == 0 ? allocateId() : clean.id
        if let clockId = clean.clockId, let clock = clocks.first(where: { $0.id == clockId }) {
            clean.zoneId = clock.zoneId
        }
        if let index = alarms.firstIndex(where: { $0.id == clean.id }) {
            alarms[index] = clean
        } else {
            alarms.append(clean)
        }
        sortAlarms()
    }

    func setAlarmEnabled(_ id: Int64, enabled: Bool) {
        guard let index = alarms.firstIndex(where: { $0.id == id }) else { return }
        alarms[index].enabled = enabled
        if !enabled {
            alarms[index].snoozeUntilMillis = nil
        }
        sortAlarms()
    }

    func snoozeAlarm(id: Int64, minutes: Int) {
        guard let index = alarms.firstIndex(where: { $0.id == id }) else { return }
        alarms[index].snoozeUntilMillis = Date.millis + Int64(minutes * 60_000)
        alarms[index].enabled = true
        sortAlarms()
    }

    func cancelSnooze(id: Int64) {
        guard let index = alarms.firstIndex(where: { $0.id == id }) else { return }
        alarms[index].snoozeUntilMillis = nil
        sortAlarms()
    }

    func deleteAlarm(_ id: Int64) {
        alarms.removeAll { $0.id == id }
        NotificationScheduler.cancelAlarm(id: id)
    }

    func deleteAllAlarms() {
        alarms.forEach { NotificationScheduler.cancelAlarm(id: $0.id) }
        alarms.removeAll()
    }

    // MARK: - Timers

    func upsertTimer(_ timer: ChronoTimer) {
        var clean = timer
        clean.id = clean.id == 0 ? allocateId() : clean.id
        clean.label = Validate.label(clean.label)
        clean.durationMillis = max(0, clean.durationMillis)
        clean.sortOrder = clean.sortOrder == 0 ? Date.millis : clean.sortOrder
        if let index = timers.firstIndex(where: { $0.id == clean.id }) {
            timers[index] = clean
        } else {
            timers.append(clean)
        }
        sortTimers()
    }

    func startTimer(_ id: Int64) {
        guard let index = timers.firstIndex(where: { $0.id == id }) else { return }
        let remaining = timers[index].remainingMillis()
        timers[index].state = .running
        timers[index].endsAtMillis = Date.millis + remaining
        timers[index].pausedRemainingMillis = nil
    }

    func pauseTimer(_ id: Int64) {
        guard let index = timers.firstIndex(where: { $0.id == id }) else { return }
        timers[index].pausedRemainingMillis = timers[index].remainingMillis()
        timers[index].state = .paused
        timers[index].endsAtMillis = nil
    }

    func resetTimer(_ id: Int64) {
        guard let index = timers.firstIndex(where: { $0.id == id }) else { return }
        timers[index].state = .idle
        timers[index].endsAtMillis = nil
        timers[index].pausedRemainingMillis = nil
    }

    func stopTimer(_ id: Int64) {
        resetTimer(id)
    }

    func addMinuteToTimer(_ id: Int64) {
        guard let index = timers.firstIndex(where: { $0.id == id }) else { return }
        timers[index].durationMillis += 60_000
        switch timers[index].state {
        case .running:
            timers[index].endsAtMillis = (timers[index].endsAtMillis ?? Date.millis) + 60_000
        case .paused:
            timers[index].pausedRemainingMillis = (timers[index].pausedRemainingMillis ?? 0) + 60_000
        case .idle, .finished:
            break
        }
    }

    func finishExpiredTimers(nowMillis: Int64 = Date.millis) {
        var changed = false
        for index in timers.indices where timers[index].state == .running && timers[index].remainingMillis(nowMillis: nowMillis) == 0 {
            timers[index].state = .finished
            timers[index].endsAtMillis = nil
            changed = true
        }
        if changed { persist() }
    }

    func deleteTimer(_ id: Int64) {
        timers.removeAll { $0.id == id }
        NotificationScheduler.cancelTimer(id: id)
    }

    func deleteAllTimers() {
        timers.forEach { NotificationScheduler.cancelTimer(id: $0.id) }
        timers.removeAll()
    }

    func reorderTimers(from source: IndexSet, to destination: Int) {
        timers.move(fromOffsets: source, toOffset: destination)
        for index in timers.indices { timers[index].sortOrder = Int64(index) }
        sortTimers()
    }

    // MARK: - Stopwatches

    func addStopwatch(label: String = "") {
        stopwatches.append(StopwatchEntry(id: allocateId(), label: Validate.label(label), sortOrder: Date.millis))
        sortStopwatches()
    }

    func renameStopwatch(id: Int64, label: String) {
        guard let index = stopwatches.firstIndex(where: { $0.id == id }) else { return }
        stopwatches[index].label = Validate.label(label)
    }

    func startStopwatch(_ id: Int64) {
        guard let index = stopwatches.firstIndex(where: { $0.id == id }) else { return }
        stopwatches[index].state = .running
        stopwatches[index].startedAtMillis = Date.millis
    }

    func pauseStopwatch(_ id: Int64) {
        guard let index = stopwatches.firstIndex(where: { $0.id == id }) else { return }
        stopwatches[index].accumulatedMillis = stopwatches[index].elapsedMillis()
        stopwatches[index].startedAtMillis = nil
        stopwatches[index].state = .paused
    }

    func resetStopwatch(_ id: Int64) {
        guard let index = stopwatches.firstIndex(where: { $0.id == id }) else { return }
        stopwatches[index].state = .idle
        stopwatches[index].startedAtMillis = nil
        stopwatches[index].accumulatedMillis = 0
        laps.removeAll { $0.stopwatchId == id }
    }

    func addLap(stopwatchId: Int64) {
        guard let stopwatch = stopwatches.first(where: { $0.id == stopwatchId }) else { return }
        let next = (laps.filter { $0.stopwatchId == stopwatchId }.map(\.lapNumber).max() ?? 0) + 1
        laps.append(StopwatchLap(id: allocateId(), stopwatchId: stopwatchId, lapNumber: next, totalElapsedMillis: stopwatch.elapsedMillis()))
    }

    func deleteStopwatch(_ id: Int64) {
        stopwatches.removeAll { $0.id == id }
        laps.removeAll { $0.stopwatchId == id }
    }

    func deleteAllStopwatches() {
        stopwatches.removeAll()
        laps.removeAll()
    }

    func reorderStopwatches(from source: IndexSet, to destination: Int) {
        stopwatches.move(fromOffsets: source, toOffset: destination)
        for index in stopwatches.indices { stopwatches[index].sortOrder = Int64(index) }
        sortStopwatches()
    }

    // MARK: - Groups

    func groups(for kind: EntityKind) -> [ItemGroup] {
        switch kind {
        case .clocks: clockGroups
        case .alarms: alarmGroups
        case .timers: timerGroups
        case .stopwatches: stopwatchGroups
        }
    }

    func createGroup(kind: EntityKind, name: String) {
        let group = ItemGroup(id: allocateId(), name: Validate.label(name).ifBlank("Group"), sortOrder: Date.millis)
        switch kind {
        case .clocks: clockGroups.append(group); sortGroups(kind)
        case .alarms: alarmGroups.append(group); sortGroups(kind)
        case .timers: timerGroups.append(group); sortGroups(kind)
        case .stopwatches: stopwatchGroups.append(group); sortGroups(kind)
        }
    }

    func renameGroup(kind: EntityKind, id: Int64, name: String) {
        mutateGroup(kind: kind, id: id) { $0.name = Validate.label(name).ifBlank($0.name) }
    }

    func setGroupCollapsed(kind: EntityKind, id: Int64, collapsed: Bool) {
        mutateGroup(kind: kind, id: id) { $0.collapsed = collapsed }
    }

    func deleteGroup(kind: EntityKind, id: Int64) {
        switch kind {
        case .clocks:
            clocks = clocks.map { $0.groupId == id ? $0.withGroup(nil) : $0 }
            clockGroups.removeAll { $0.id == id }
        case .alarms:
            alarms = alarms.map { $0.groupId == id ? $0.withGroup(nil) : $0 }
            alarmGroups.removeAll { $0.id == id }
        case .timers:
            timers = timers.map { $0.groupId == id ? $0.withGroup(nil) : $0 }
            timerGroups.removeAll { $0.id == id }
        case .stopwatches:
            stopwatches = stopwatches.map { $0.groupId == id ? $0.withGroup(nil) : $0 }
            stopwatchGroups.removeAll { $0.id == id }
        }
    }

    func assignToGroup(kind: EntityKind, itemId: Int64, groupId: Int64?) {
        switch kind {
        case .clocks:
            if let index = clocks.firstIndex(where: { $0.id == itemId }) { clocks[index].groupId = groupId }
        case .alarms:
            if let index = alarms.firstIndex(where: { $0.id == itemId }) { alarms[index].groupId = groupId }
        case .timers:
            if let index = timers.firstIndex(where: { $0.id == itemId }) { timers[index].groupId = groupId }
        case .stopwatches:
            if let index = stopwatches.firstIndex(where: { $0.id == itemId }) { stopwatches[index].groupId = groupId }
        }
    }

    private func mutateGroup(kind: EntityKind, id: Int64, mutate: (inout ItemGroup) -> Void) {
        switch kind {
        case .clocks:
            if let index = clockGroups.firstIndex(where: { $0.id == id }) { mutate(&clockGroups[index]) }
        case .alarms:
            if let index = alarmGroups.firstIndex(where: { $0.id == id }) { mutate(&alarmGroups[index]) }
        case .timers:
            if let index = timerGroups.firstIndex(where: { $0.id == id }) { mutate(&timerGroups[index]) }
        case .stopwatches:
            if let index = stopwatchGroups.firstIndex(where: { $0.id == id }) { mutate(&stopwatchGroups[index]) }
        }
    }

    private func sortGroups(_ kind: EntityKind) {
        switch kind {
        case .clocks: clockGroups.sort { $0.sortOrder < $1.sortOrder }
        case .alarms: alarmGroups.sort { $0.sortOrder < $1.sortOrder }
        case .timers: timerGroups.sort { $0.sortOrder < $1.sortOrder }
        case .stopwatches: stopwatchGroups.sort { $0.sortOrder < $1.sortOrder }
        }
    }

    // MARK: - Persistence

    private func load() {
        guard let data = UserDefaults.standard.data(forKey: key),
              let snapshot = try? JSONDecoder().decode(ZoneAnchorSnapshot.self, from: data) else {
            return
        }
        nextId = snapshot.nextId
        clocks = snapshot.clocks.map(cleanClock).sorted { $0.sortOrder < $1.sortOrder }
        alarms = snapshot.alarms.map(cleanAlarm).sorted(by: alarmSort)
        timers = snapshot.timers.map(cleanTimer).sorted { $0.sortOrder < $1.sortOrder }
        stopwatches = snapshot.stopwatches.map(cleanStopwatch).sorted { $0.sortOrder < $1.sortOrder }
        laps = snapshot.laps
        clockGroups = snapshot.clockGroups.sorted { $0.sortOrder < $1.sortOrder }
        alarmGroups = snapshot.alarmGroups.sorted { $0.sortOrder < $1.sortOrder }
        timerGroups = snapshot.timerGroups.sorted { $0.sortOrder < $1.sortOrder }
        stopwatchGroups = snapshot.stopwatchGroups.sorted { $0.sortOrder < $1.sortOrder }
        settings = snapshot.settings
    }

    private func persistAndReschedule() {
        persist()
        if !isLoading {
            rescheduleAllNotifications()
        }
    }

    private func persist() {
        guard !isLoading else { return }
        let snapshot = ZoneAnchorSnapshot(
            nextId: nextId,
            clocks: clocks,
            alarms: alarms,
            timers: timers,
            stopwatches: stopwatches,
            laps: laps,
            clockGroups: clockGroups,
            alarmGroups: alarmGroups,
            timerGroups: timerGroups,
            stopwatchGroups: stopwatchGroups,
            settings: settings
        )
        if let data = try? JSONEncoder().encode(snapshot) {
            UserDefaults.standard.set(data, forKey: key)
        }
    }

    private func rescheduleAllNotifications() {
        alarms.forEach(NotificationScheduler.scheduleAlarm)
        timers.forEach(NotificationScheduler.scheduleTimer)
    }

    private func allocateId() -> Int64 {
        let result = nextId
        nextId += 1
        return result
    }

    private func sortClocks() { clocks.sort { $0.sortOrder < $1.sortOrder } }
    private func sortTimers() { timers.sort { $0.sortOrder < $1.sortOrder } }
    private func sortStopwatches() { stopwatches.sort { $0.sortOrder < $1.sortOrder } }
    private func sortAlarms() { alarms.sort(by: alarmSort) }

    private func alarmSort(_ lhs: AlarmEntry, _ rhs: AlarmEntry) -> Bool {
        if lhs.enabled != rhs.enabled { return lhs.enabled && !rhs.enabled }
        let leftNext = nextAlarmMillis(lhs) ?? Int64.max
        let rightNext = nextAlarmMillis(rhs) ?? Int64.max
        if leftNext != rightNext { return leftNext < rightNext }
        if lhs.hour != rhs.hour { return lhs.hour < rhs.hour }
        if lhs.minute != rhs.minute { return lhs.minute < rhs.minute }
        return lhs.id < rhs.id
    }

    func nextAlarmMillis(_ alarm: AlarmEntry) -> Int64? {
        if alarm.isSnoozed(), let snooze = alarm.snoozeUntilMillis { return snooze }
        guard alarm.enabled else { return nil }
        return AlarmMath.nextTriggerMillis(for: alarm)
    }

    private func cleanClock(_ clock: ClockEntry) -> ClockEntry {
        var clean = clock
        clean.label = Validate.label(clean.label)
        clean.zoneId = Validate.zoneId(clean.zoneId)
        return clean
    }

    private func cleanAlarm(_ alarm: AlarmEntry) -> AlarmEntry {
        var clean = alarm
        clean.label = Validate.label(clean.label)
        clean.zoneId = Validate.zoneId(clean.zoneId)
        clean.hour = Validate.hour(clean.hour)
        clean.minute = Validate.minute(clean.minute)
        clean.daysMask = DayMask.sanitize(clean.daysMask)
        clean.repeatInterval = Validate.repeatInterval(clean.repeatInterval)
        clean.monthlyDay = Validate.monthlyDay(clean.monthlyDay)
        clean.monthlyOrdinal = Validate.monthlyOrdinal(clean.monthlyOrdinal)
        clean.monthlyWeekday = Validate.monthlyWeekday(clean.monthlyWeekday)
        return clean
    }

    private func cleanTimer(_ timer: ChronoTimer) -> ChronoTimer {
        var clean = timer
        clean.label = Validate.label(clean.label)
        clean.durationMillis = max(0, clean.durationMillis)
        return clean
    }

    private func cleanStopwatch(_ stopwatch: StopwatchEntry) -> StopwatchEntry {
        var clean = stopwatch
        clean.label = Validate.label(clean.label)
        clean.accumulatedMillis = max(0, clean.accumulatedMillis)
        return clean
    }
}

enum BackupError: LocalizedError {
    case unsupportedVersion

    var errorDescription: String? {
        switch self {
        case .unsupportedVersion:
            return "This backup was created by a newer ZoneAnchor version."
        }
    }
}

private extension AlarmEntry {
    func detachedFromClock() -> AlarmEntry {
        var copy = self
        copy.clockId = nil
        return copy
    }

    func withGroup(_ groupId: Int64?) -> AlarmEntry {
        var copy = self
        copy.groupId = groupId
        return copy
    }
}

private extension ClockEntry {
    func withGroup(_ groupId: Int64?) -> ClockEntry {
        var copy = self
        copy.groupId = groupId
        return copy
    }
}

private extension ChronoTimer {
    func withGroup(_ groupId: Int64?) -> ChronoTimer {
        var copy = self
        copy.groupId = groupId
        return copy
    }
}

private extension StopwatchEntry {
    func withGroup(_ groupId: Int64?) -> StopwatchEntry {
        var copy = self
        copy.groupId = groupId
        return copy
    }
}

private extension String {
    func ifBlank(_ fallback: String) -> String {
        trimmed.isEmpty ? fallback : self
    }

    var cityLabel: String {
        components(separatedBy: "/").last?.replacingOccurrences(of: "_", with: " ") ?? self
    }
}
