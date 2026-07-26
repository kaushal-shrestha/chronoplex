import SwiftUI

struct AlarmsView: View {
    @EnvironmentObject private var store: ChronoplexStore
    let now: Date
    @State private var showingAdd = false
    @State private var editingAlarm: AlarmEntry?
    @State private var showingGroups = false
    @State private var showingClearConfirmation = false

    var body: some View {
        NavigationStack {
            List {
                permissionSection
                alarmSections
            }
            .navigationTitle("Alarms")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { showingGroups = true } label: { Image(systemName: "folder") }
                    Button { showingAdd = true } label: { Image(systemName: "plus") }
                }
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        Button("Clear all alarms", role: .destructive) { showingClearConfirmation = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showingAdd) {
                AlarmEditorSheet(alarm: AlarmEntry(id: 0), zoneSource: store.settings.defaultZoneSource)
            }
            .sheet(item: $editingAlarm) { alarm in
                AlarmEditorSheet(alarm: alarm, zoneSource: store.settings.defaultZoneSource)
            }
            .sheet(isPresented: $showingGroups) {
                ManageGroupsSheet(kind: .alarms)
            }
            .confirmationDialog("Clear all alarms?", isPresented: $showingClearConfirmation, titleVisibility: .visible) {
                Button("Clear all alarms", role: .destructive) { store.deleteAllAlarms() }
            }
        }
    }

    @ViewBuilder
    private var permissionSection: some View {
        if !store.notificationsAuthorized {
            Section {
                VStack(alignment: .leading, spacing: 8) {
                    Label("Notifications are off", systemImage: "bell.slash")
                        .font(.headline)
                    Text("iOS alarms and timers use local notifications. Enable notifications so Chronoplex can ring outside the app.")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Button("Enable Notifications") {
                        store.requestNotifications()
                    }
                    .buttonStyle(.borderedProminent)
                }
                .padding(.vertical, 4)
            }
        }
    }

    @ViewBuilder
    private var alarmSections: some View {
        if store.alarms.isEmpty {
            Section {
                EmptyStateView(title: "No alarms", detail: "Add a timezone-anchored alarm.", systemImage: "alarm")
            }
        } else if store.settings.groupedAlarms {
            groupedAlarmList
        } else {
            Section("Alarms") {
                ForEach(store.alarms) { alarm in
                    AlarmRow(alarm: alarm, now: now, onEdit: { editingAlarm = alarm })
                }
            }
        }
    }

    private var groupedAlarmList: some View {
        ForEach(alarmGroupBuckets(), id: \.id) { bucket in
            Section {
                if !bucket.collapsed {
                    ForEach(bucket.items) { alarm in
                        AlarmRow(alarm: alarm, now: now, onEdit: { editingAlarm = alarm })
                    }
                }
            } header: {
                Button {
                    if bucket.id != 0 {
                        store.setGroupCollapsed(kind: .alarms, id: bucket.id, collapsed: !bucket.collapsed)
                    }
                } label: {
                    SectionHeader(title: bucket.name, subtitle: "\(bucket.items.count) alarms")
                }
            }
        }
    }

    private func alarmGroupBuckets() -> [AlarmBucket] {
        var result = store.alarmGroups.sorted { $0.sortOrder < $1.sortOrder }.map { group in
            AlarmBucket(id: group.id, name: group.name, collapsed: group.collapsed, items: store.alarms.filter { $0.groupId == group.id })
        }
        let groupedIds = Set(store.alarmGroups.map(\.id))
        let ungrouped = store.alarms.filter { alarm in
            guard let id = alarm.groupId else { return true }
            return !groupedIds.contains(id)
        }
        result.append(AlarmBucket(id: 0, name: "Ungrouped", collapsed: false, items: ungrouped))
        return result.filter { !$0.items.isEmpty || $0.id != 0 }
    }
}

private struct AlarmBucket {
    let id: Int64
    let name: String
    let collapsed: Bool
    let items: [AlarmEntry]
}

struct AlarmRow: View {
    @EnvironmentObject private var store: ChronoplexStore
    let alarm: AlarmEntry
    let now: Date
    let onEdit: () -> Void

    private var nextMillis: Int64? { store.nextAlarmMillis(alarm) }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(String(format: "%02d:%02d", alarm.hour, alarm.minute))
                        .font(.system(size: 34, weight: .semibold, design: .rounded))
                        .monospacedDigit()
                    Text(alarm.label.trimmed.isEmpty ? "Alarm" : alarm.label)
                        .font(.headline)
                }
                Spacer()
                Toggle("", isOn: Binding(
                    get: { alarm.enabled },
                    set: { store.setAlarmEnabled(alarm.id, enabled: $0) }
                ))
                .labelsHidden()
            }

            Text("\(zoneDisplay) - \(TimeFormat.repeatLabel(for: alarm, firstDay: store.settings.firstDayOfWeek))")
                .font(.caption)
                .foregroundStyle(.secondary)

            if let nextMillis {
                let date = Date(millis: nextMillis)
                Text("Next: \(TimeFormat.nextFire(date, zoneId: alarm.zoneId)) reference time - \(TimeFormat.nextFire(date, zoneId: TimeZone.current.identifier)) device - \(TimeFormat.remaining(nextMillis - Date.millis))")
                    .font(.caption)
                    .foregroundStyle(alarm.isSnoozed() ? .orange : .secondary)
            }

            if alarm.isSnoozed() {
                Button("Cancel Snooze") {
                    store.cancelSnooze(id: alarm.id)
                }
                .buttonStyle(.bordered)
            }
        }
        .padding(.vertical, 4)
        .swipeActions {
            Button(role: .destructive) { store.deleteAlarm(alarm.id) } label: {
                Label("Delete", systemImage: "trash")
            }
            Button { onEdit() } label: {
                Label("Edit", systemImage: "pencil")
            }
            .tint(.blue)
        }
        .contextMenu {
            Button("Snooze 1 minute") { store.snoozeAlarm(id: alarm.id, minutes: 1) }
            Button("Snooze 5 minutes") { store.snoozeAlarm(id: alarm.id, minutes: 5) }
            Button("Snooze 10 minutes") { store.snoozeAlarm(id: alarm.id, minutes: 10) }
            Button("Edit") { onEdit() }
            Button("Delete", role: .destructive) { store.deleteAlarm(alarm.id) }
        }
    }

    private var zoneDisplay: String {
        guard store.settings.alarmZoneDisplay == .clockLabel,
              let clockId = alarm.clockId,
              let clock = store.clocks.first(where: { $0.id == clockId }) else {
            return alarm.zoneId
        }
        return clock.displayLabel
    }
}

struct AlarmEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: ChronoplexStore
    @State private var draft: AlarmEntry
    @State private var zoneSource: ZoneSource
    @State private var showingZonePicker = false

    init(alarm: AlarmEntry, zoneSource: ZoneSource) {
        _draft = State(initialValue: alarm)
        _zoneSource = State(initialValue: zoneSource)
    }

    private var zoneChoices: [String] {
        switch zoneSource {
        case .myClocks:
            let values = store.clocks.map(\.zoneId).uniqued()
            return values.isEmpty ? TimeZones.all : values
        case .allZones:
            return TimeZones.all
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Time") {
                    HStack {
                        Picker("Hour", selection: $draft.hour) {
                            ForEach(0..<24) { Text(String(format: "%02d", $0)).tag($0) }
                        }
                        Picker("Minute", selection: $draft.minute) {
                            ForEach(0..<60) { Text(String(format: "%02d", $0)).tag($0) }
                        }
                    }
                    .pickerStyle(.wheel)
                    TextField("Label", text: $draft.label)
                }

                Section("Reference Clock") {
                    Picker("Associated clock", selection: Binding(
                        get: { draft.clockId ?? 0 },
                        set: { newValue in
                            draft.clockId = newValue == 0 ? nil : newValue
                            if let clock = store.clocks.first(where: { $0.id == newValue }) {
                                draft.zoneId = clock.zoneId
                            }
                        }
                    )) {
                        Text("None").tag(Int64(0))
                        ForEach(store.clocks) { clock in
                            Text(clock.displayLabel).tag(clock.id)
                        }
                    }

                    Picker("Zone source", selection: $zoneSource) {
                        ForEach(ZoneSource.allCases) { source in
                            Text(source.title).tag(source)
                        }
                    }
                    .pickerStyle(.segmented)

                    Button {
                        showingZonePicker = true
                    } label: {
                        HStack {
                            Text("Time zone")
                            Spacer()
                            Text(draft.zoneId)
                                .foregroundStyle(.secondary)
                                .multilineTextAlignment(.trailing)
                        }
                    }
                    .disabled(draft.clockId != nil)
                }

                Section("Repeat") {
                    Picker("Repeat", selection: $draft.repeatType) {
                        ForEach(AlarmRepeatType.allCases) { type in
                            Text(type.title).tag(type)
                        }
                    }

                    if draft.repeatType == .weekly {
                        DayOfWeekPicker(daysMask: $draft.daysMask, firstDay: store.settings.firstDayOfWeek)
                    }

                    if draft.repeatType != .once {
                        Stepper("Every \(draft.repeatInterval) \(draft.repeatType == .weekly ? "weeks" : "months")", value: $draft.repeatInterval, in: 1...99)
                        TextField("Start date (yyyy-mm-dd)", text: $draft.repeatStartDate)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.numbersAndPunctuation)
                    }

                    if draft.repeatType == .monthlyDay {
                        Stepper("Day \(draft.monthlyDay)", value: $draft.monthlyDay, in: 1...31)
                    }

                    if draft.repeatType == .monthlyWeekday {
                        Picker("Ordinal", selection: $draft.monthlyOrdinal) {
                            Text("First").tag(1)
                            Text("Second").tag(2)
                            Text("Third").tag(3)
                            Text("Fourth").tag(4)
                            Text("Last").tag(-1)
                        }
                        Picker("Weekday", selection: $draft.monthlyWeekday) {
                            ForEach(1...7, id: \.self) { day in
                                Text(TimeFormat.longWeekdayName(day)).tag(day)
                            }
                        }
                    }

                    HStack {
                        Button("Weekdays") {
                            draft.repeatType = .weekly
                            draft.daysMask = DayMask.weekdays
                        }
                        Button("Weekends") {
                            draft.repeatType = .weekly
                            draft.daysMask = DayMask.weekends
                        }
                        Button("Every day") {
                            draft.repeatType = .weekly
                            draft.daysMask = DayMask.everyDay
                        }
                    }
                    .buttonStyle(.bordered)
                }

                Section("Alert") {
                    Toggle("Sound", isOn: $draft.soundEnabled)
                    Toggle("Vibration", isOn: $draft.vibrationEnabled)
                    Toggle("Enabled", isOn: $draft.enabled)
                }

                Section("Organization") {
                    GroupPicker(kind: .alarms, groupId: $draft.groupId)
                }
            }
            .navigationTitle(draft.id == 0 ? "Add Alarm" : "Edit Alarm")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        store.upsertAlarm(draft)
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showingZonePicker) {
                ZonePickerSheet(zones: zoneChoices, selection: $draft.zoneId)
            }
        }
    }
}

struct DayOfWeekPicker: View {
    @Binding var daysMask: Int
    let firstDay: FirstDayOfWeek

    var body: some View {
        HStack {
            ForEach(TimeFormat.orderedWeekdays(firstDay: firstDay), id: \.self) { day in
                Button {
                    daysMask = DayMask.toggle(daysMask, weekday: day)
                } label: {
                    Text(TimeFormat.shortWeekdayName(day).prefix(1))
                        .font(.caption.weight(.bold))
                        .frame(width: 32, height: 32)
                        .background(DayMask.contains(daysMask, weekday: day) ? Color.accentColor : Color.secondary.opacity(0.14))
                        .foregroundStyle(DayMask.contains(daysMask, weekday: day) ? .white : .primary)
                        .clipShape(Circle())
                }
                .buttonStyle(.plain)
            }
        }
        .padding(.vertical, 4)
    }
}
