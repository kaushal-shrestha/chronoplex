import SwiftUI

struct TimersView: View {
    @EnvironmentObject private var store: ChronoplexStore
    let now: Date
    @State private var showingAdd = false
    @State private var editingTimer: ChronoTimer?
    @State private var showingGroups = false
    @State private var showingClearConfirmation = false
    @State private var deletedTimer: ChronoTimer?

    var body: some View {
        NavigationStack {
            List {
                if let deletedTimer {
                    Section {
                        HStack {
                            Text("Timer deleted")
                            Spacer()
                            Button("Undo") {
                                store.upsertTimer(deletedTimer)
                                self.deletedTimer = nil
                            }
                        }
                    }
                }

                if store.timers.isEmpty {
                    Section {
                        EmptyStateView(title: "No timers", detail: "Create reusable timers for routines and workflows.", systemImage: "timer")
                    }
                } else if store.settings.groupedTimers {
                    groupedTimerList
                } else {
                    Section("Timers") {
                        ForEach(store.timers) { timer in
                            TimerRow(timer: timer, now: now, onEdit: { editingTimer = timer }, onDelete: deleteTimer)
                        }
                        .onMove(perform: store.reorderTimers)
                    }
                }
            }
            .navigationTitle("Timers")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { showingGroups = true } label: { Image(systemName: "folder") }
                    Button { showingAdd = true } label: { Image(systemName: "plus") }
                }
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        Button("Clear all timers", role: .destructive) { showingClearConfirmation = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showingAdd) {
                TimerEditorSheet(timer: ChronoTimer(id: 0, durationMillis: 5 * 60_000, finishMode: store.settings.defaultTimerFinishMode))
            }
            .sheet(item: $editingTimer) { timer in
                TimerEditorSheet(timer: timer)
            }
            .sheet(isPresented: $showingGroups) {
                ManageGroupsSheet(kind: .timers)
            }
            .confirmationDialog("Clear all timers?", isPresented: $showingClearConfirmation, titleVisibility: .visible) {
                Button("Clear all timers", role: .destructive) { store.deleteAllTimers() }
            }
        }
    }

    private var groupedTimerList: some View {
        ForEach(timerGroupBuckets(), id: \.id) { bucket in
            Section {
                if !bucket.collapsed {
                    ForEach(bucket.items) { timer in
                        TimerRow(timer: timer, now: now, onEdit: { editingTimer = timer }, onDelete: deleteTimer)
                    }
                }
            } header: {
                Button {
                    if bucket.id != 0 {
                        store.setGroupCollapsed(kind: .timers, id: bucket.id, collapsed: !bucket.collapsed)
                    }
                } label: {
                    SectionHeader(title: bucket.name, subtitle: "\(bucket.items.count) timers")
                }
            }
        }
    }

    private func timerGroupBuckets() -> [TimerBucket] {
        var result = store.timerGroups.sorted { $0.sortOrder < $1.sortOrder }.map { group in
            TimerBucket(id: group.id, name: group.name, collapsed: group.collapsed, items: store.timers.filter { $0.groupId == group.id })
        }
        let groupedIds = Set(store.timerGroups.map(\.id))
        let ungrouped = store.timers.filter { timer in
            guard let id = timer.groupId else { return true }
            return !groupedIds.contains(id)
        }
        result.append(TimerBucket(id: 0, name: "Ungrouped", collapsed: false, items: ungrouped))
        return result.filter { !$0.items.isEmpty || $0.id != 0 }
    }

    private func deleteTimer(_ timer: ChronoTimer) {
        deletedTimer = timer
        store.deleteTimer(timer.id)
    }
}

private struct TimerBucket {
    let id: Int64
    let name: String
    let collapsed: Bool
    let items: [ChronoTimer]
}

struct TimerRow: View {
    @EnvironmentObject private var store: ChronoplexStore
    let timer: ChronoTimer
    let now: Date
    let onEdit: () -> Void
    let onDelete: (ChronoTimer) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(timer.label.trimmed.isEmpty ? "Timer" : timer.label)
                        .font(.headline)
                    Text(timer.finishMode.title)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(TimeFormat.duration(timer.remainingMillis(nowMillis: Int64(now.timeIntervalSince1970 * 1000))))
                    .font(.system(size: 32, weight: .semibold, design: .rounded))
                    .monospacedDigit()
            }

            HStack {
                switch timer.state {
                case .idle, .paused, .finished:
                    Button { store.startTimer(timer.id) } label: { Label("Start", systemImage: "play.fill") }
                case .running:
                    Button { store.pauseTimer(timer.id) } label: { Label("Pause", systemImage: "pause.fill") }
                }
                Button { store.resetTimer(timer.id) } label: { Label("Reset", systemImage: "arrow.counterclockwise") }
                Button { store.addMinuteToTimer(timer.id) } label: { Label("+1m", systemImage: "plus") }
            }
            .buttonStyle(.bordered)
            .labelStyle(.iconOnly)
        }
        .padding(.vertical, 4)
        .swipeActions {
            Button(role: .destructive) { onDelete(timer) } label: {
                Label("Delete", systemImage: "trash")
            }
            Button { onEdit() } label: {
                Label("Edit", systemImage: "pencil")
            }
            .tint(.blue)
        }
    }
}

struct TimerEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: ChronoplexStore
    @State private var draft: ChronoTimer
    @State private var hours: Int
    @State private var minutes: Int
    @State private var seconds: Int

    init(timer: ChronoTimer) {
        _draft = State(initialValue: timer)
        let total = Int(timer.durationMillis / 1000)
        _hours = State(initialValue: total / 3600)
        _minutes = State(initialValue: (total % 3600) / 60)
        _seconds = State(initialValue: total % 60)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Timer") {
                    TextField("Label", text: $draft.label)
                    HStack {
                        Stepper("Hours \(hours)", value: $hours, in: 0...23)
                        Stepper("Minutes \(minutes)", value: $minutes, in: 0...59)
                    }
                    Stepper("Seconds \(seconds)", value: $seconds, in: 0...59)
                    presetButtons
                }

                Section("Finish") {
                    Picker("Behavior", selection: $draft.finishMode) {
                        ForEach(TimerFinishMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                }

                Section("Organization") {
                    GroupPicker(kind: .timers, groupId: $draft.groupId)
                }
            }
            .navigationTitle(draft.id == 0 ? "Add Timer" : "Edit Timer")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        draft.durationMillis = Int64(((hours * 3600) + (minutes * 60) + seconds) * 1000)
                        if draft.durationMillis == 0 { draft.durationMillis = 60_000 }
                        store.upsertTimer(draft)
                        dismiss()
                    }
                }
            }
        }
    }

    private var presetButtons: some View {
        HStack {
            ForEach([60, 300, 600, 900, 1800, 3600], id: \.self) { secondsValue in
                Button(secondsValue == 3600 ? "1h" : "\(secondsValue / 60)m") {
                    hours = secondsValue / 3600
                    minutes = (secondsValue % 3600) / 60
                    seconds = 0
                }
            }
        }
        .buttonStyle(.bordered)
    }
}
