import SwiftUI

struct StopwatchesView: View {
    @EnvironmentObject private var store: ZoneAnchorStore
    let now: Date
    @State private var showingGroups = false
    @State private var showingClearConfirmation = false
    @State private var renameTarget: StopwatchEntry?
    @State private var renameText = ""
    @State private var deletedStopwatch: (StopwatchEntry, [StopwatchLap])?

    var body: some View {
        NavigationStack {
            List {
                if deletedStopwatch != nil {
                    Section {
                        HStack {
                            Text("Stopwatch deleted")
                            Spacer()
                            Button("Undo") {
                                if let deletedStopwatch {
                                    store.stopwatches.append(deletedStopwatch.0)
                                    store.laps.append(contentsOf: deletedStopwatch.1)
                                }
                                deletedStopwatch = nil
                            }
                        }
                    }
                }

                if store.stopwatches.isEmpty {
                    Section {
                        EmptyStateView(title: "No stopwatches", detail: "Track multiple elapsed-time streams with laps.", systemImage: "stopwatch")
                    }
                } else if store.settings.groupedStopwatches {
                    groupedStopwatchList
                } else {
                    Section("Stopwatches") {
                        ForEach(store.stopwatches) { stopwatch in
                            StopwatchRow(stopwatch: stopwatch, now: now, onRename: beginRename, onDelete: deleteStopwatch)
                        }
                        .onMove(perform: store.reorderStopwatches)
                    }
                }
            }
            .navigationTitle("Stopwatch")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { showingGroups = true } label: { Image(systemName: "folder") }
                    Button { store.addStopwatch() } label: { Image(systemName: "plus") }
                }
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        Button("Clear all stopwatches", role: .destructive) { showingClearConfirmation = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showingGroups) {
                ManageGroupsSheet(kind: .stopwatches)
            }
            .confirmationDialog("Clear all stopwatches?", isPresented: $showingClearConfirmation, titleVisibility: .visible) {
                Button("Clear all stopwatches", role: .destructive) { store.deleteAllStopwatches() }
            }
            .alert("Rename stopwatch", isPresented: Binding(
                get: { renameTarget != nil },
                set: { if !$0 { renameTarget = nil } }
            )) {
                TextField("Name", text: $renameText)
                Button("Save") {
                    if let renameTarget {
                        store.renameStopwatch(id: renameTarget.id, label: renameText)
                    }
                    renameTarget = nil
                }
                Button("Cancel", role: .cancel) { renameTarget = nil }
            }
        }
    }

    private var groupedStopwatchList: some View {
        ForEach(stopwatchGroupBuckets(), id: \.id) { bucket in
            Section {
                if !bucket.collapsed {
                    ForEach(bucket.items) { stopwatch in
                        StopwatchRow(stopwatch: stopwatch, now: now, onRename: beginRename, onDelete: deleteStopwatch)
                    }
                }
            } header: {
                Button {
                    if bucket.id != 0 {
                        store.setGroupCollapsed(kind: .stopwatches, id: bucket.id, collapsed: !bucket.collapsed)
                    }
                } label: {
                    SectionHeader(title: bucket.name, subtitle: "\(bucket.items.count) stopwatches")
                }
            }
        }
    }

    private func stopwatchGroupBuckets() -> [StopwatchBucket] {
        var result = store.stopwatchGroups.sorted { $0.sortOrder < $1.sortOrder }.map { group in
            StopwatchBucket(id: group.id, name: group.name, collapsed: group.collapsed, items: store.stopwatches.filter { $0.groupId == group.id })
        }
        let groupedIds = Set(store.stopwatchGroups.map(\.id))
        let ungrouped = store.stopwatches.filter { stopwatch in
            guard let id = stopwatch.groupId else { return true }
            return !groupedIds.contains(id)
        }
        result.append(StopwatchBucket(id: 0, name: "Ungrouped", collapsed: false, items: ungrouped))
        return result.filter { !$0.items.isEmpty || $0.id != 0 }
    }

    private func beginRename(_ stopwatch: StopwatchEntry) {
        renameTarget = stopwatch
        renameText = stopwatch.label
    }

    private func deleteStopwatch(_ stopwatch: StopwatchEntry) {
        deletedStopwatch = (stopwatch, store.laps.filter { $0.stopwatchId == stopwatch.id })
        store.deleteStopwatch(stopwatch.id)
    }
}

private struct StopwatchBucket {
    let id: Int64
    let name: String
    let collapsed: Bool
    let items: [StopwatchEntry]
}

struct StopwatchRow: View {
    @EnvironmentObject private var store: ZoneAnchorStore
    let stopwatch: StopwatchEntry
    let now: Date
    let onRename: (StopwatchEntry) -> Void
    let onDelete: (StopwatchEntry) -> Void

    private var laps: [StopwatchLap] {
        store.laps
            .filter { $0.stopwatchId == stopwatch.id }
            .sorted { $0.lapNumber > $1.lapNumber }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                VStack(alignment: .leading, spacing: 2) {
                    Text(stopwatch.label.trimmed.isEmpty ? "Stopwatch \(stopwatch.id)" : stopwatch.label)
                        .font(.headline)
                    Text(stopwatch.state.rawValue.capitalized)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Text(TimeFormat.duration(stopwatch.elapsedMillis(nowMillis: Int64(now.timeIntervalSince1970 * 1000))))
                    .font(.system(size: 32, weight: .semibold, design: .rounded))
                    .monospacedDigit()
            }

            HStack {
                switch stopwatch.state {
                case .idle, .paused:
                    Button { store.startStopwatch(stopwatch.id) } label: { Label("Start", systemImage: "play.fill") }
                case .running:
                    Button { store.pauseStopwatch(stopwatch.id) } label: { Label("Pause", systemImage: "pause.fill") }
                }
                Button { store.resetStopwatch(stopwatch.id) } label: { Label("Reset", systemImage: "arrow.counterclockwise") }
                Button { store.addLap(stopwatchId: stopwatch.id) } label: { Label("Lap", systemImage: "flag") }
                    .disabled(stopwatch.state != .running)
            }
            .buttonStyle(.bordered)
            .labelStyle(.iconOnly)

            ForEach(laps.prefix(5)) { lap in
                HStack {
                    Text("Lap \(lap.lapNumber)")
                    Spacer()
                    Text(TimeFormat.duration(lap.totalElapsedMillis))
                        .monospacedDigit()
                }
                .font(.caption)
                .foregroundStyle(.secondary)
            }
        }
        .padding(.vertical, 4)
        .swipeActions {
            Button(role: .destructive) { onDelete(stopwatch) } label: {
                Label("Delete", systemImage: "trash")
            }
            Button { onRename(stopwatch) } label: {
                Label("Rename", systemImage: "pencil")
            }
            .tint(.blue)
        }
        .contextMenu {
            Button("Rename") { onRename(stopwatch) }
            Button("Move to Ungrouped") { store.assignToGroup(kind: .stopwatches, itemId: stopwatch.id, groupId: nil) }
            Button("Delete", role: .destructive) { onDelete(stopwatch) }
        }
    }
}
