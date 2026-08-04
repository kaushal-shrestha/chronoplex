import SwiftUI

struct ZonePickerSheet: View {
    @Environment(\.dismiss) private var dismiss
    let zones: [String]
    @Binding var selection: String
    @State private var query = ""

    private var filtered: [String] {
        let source = zones.isEmpty ? TimeZones.all : zones
        let clean = query.trimmed.lowercased()
        let values = clean.isEmpty ? source : source.filter {
            $0.lowercased().contains(clean) ||
            $0.replacingOccurrences(of: "_", with: " ").lowercased().contains(clean)
        }
        return Array(values.prefix(150))
    }

    var body: some View {
        NavigationStack {
            List(filtered, id: \.self) { zone in
                Button {
                    selection = zone
                    dismiss()
                } label: {
                    HStack {
                        VStack(alignment: .leading) {
                            Text(zone.replacingOccurrences(of: "_", with: " "))
                            Text(TimeFormat.clock(Date(), zoneId: zone, seconds: false))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                        }
                        Spacer()
                        if zone == selection {
                            Image(systemName: "checkmark")
                        }
                    }
                }
            }
            .searchable(text: $query, prompt: "City or time zone")
            .navigationTitle("Time Zone")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
            }
        }
    }
}

struct ClocksView: View {
    @EnvironmentObject private var store: ZoneAnchorStore
    let now: Date
    @State private var editingClock: ClockEntry?
    @State private var showingAdd = false
    @State private var showingGroups = false
    @State private var showingClearConfirmation = false
    @State private var converterDate = Date()
    @State private var converterReferenceId: Int64 = 0

    var body: some View {
        NavigationStack {
            List {
                deviceSection
                converterSection
                savedClockSections
            }
            .navigationTitle("Clocks")
            .toolbar {
                ToolbarItemGroup(placement: .topBarTrailing) {
                    Button { showingGroups = true } label: { Image(systemName: "folder") }
                    Button { showingAdd = true } label: { Image(systemName: "plus") }
                }
                ToolbarItem(placement: .topBarLeading) {
                    Menu {
                        Button("Reset to suggested clocks") { store.resetSuggestedClocks() }
                        Button("Clear all clocks", role: .destructive) { showingClearConfirmation = true }
                    } label: {
                        Image(systemName: "ellipsis.circle")
                    }
                }
            }
            .sheet(isPresented: $showingAdd) {
                ClockEditorSheet(clock: ClockEntry(id: 0, label: "", zoneId: TimeZone.current.identifier))
            }
            .sheet(item: $editingClock) { clock in
                ClockEditorSheet(clock: clock)
            }
            .sheet(isPresented: $showingGroups) {
                ManageGroupsSheet(kind: .clocks)
            }
            .confirmationDialog("Clear all clocks?", isPresented: $showingClearConfirmation, titleVisibility: .visible) {
                Button("Clear all clocks", role: .destructive) { store.deleteAllClocks() }
            }
        }
    }

    private var deviceSection: some View {
        Section {
            VStack(alignment: .leading, spacing: 8) {
                Text(TimeFormat.clock(now, zoneId: TimeZone.current.identifier))
                    .font(.system(size: 42, weight: .semibold, design: .rounded))
                    .monospacedDigit()
                Text(TimeFormat.weekdayDate(now, zoneId: TimeZone.current.identifier))
                    .font(.headline)
                Text(TimeZone.current.identifier)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.vertical, 6)
        } header: {
            SectionHeader(title: "Device Time")
        }
    }

    private var converterSection: some View {
        Section {
            Picker("Reference", selection: $converterReferenceId) {
                Text("Device").tag(Int64(0))
                ForEach(store.clocks) { clock in
                    Text(clock.displayLabel).tag(clock.id)
                }
            }
            HStack {
                Button("-1h") { converterDate = converterDate.addingTimeInterval(-3600) }
                Button("+1h") { converterDate = converterDate.addingTimeInterval(3600) }
                Button("Yesterday") { converterDate = converterDate.addingTimeInterval(-86_400) }
                Button("Today") { converterDate = now }
                Button("Tomorrow") { converterDate = converterDate.addingTimeInterval(86_400) }
            }
            .buttonStyle(.bordered)
            .font(.caption)
            .lineLimit(1)
            .minimumScaleFactor(0.7)

            DatePicker("Instant", selection: $converterDate)

            ForEach(store.clocks) { clock in
                Button {
                    converterReferenceId = clock.id
                } label: {
                    HStack {
                        Text(clock.displayLabel)
                        Spacer()
                        Text(TimeFormat.clock(converterDate, zoneId: clock.zoneId, seconds: false))
                            .monospacedDigit()
                    }
                }
                .foregroundStyle(.primary)
            }
        } header: {
            SectionHeader(title: "Time Converter", subtitle: "Compare a selected instant across saved clocks.")
        }
    }

    @ViewBuilder
    private var savedClockSections: some View {
        if store.clocks.isEmpty {
            Section {
                EmptyStateView(title: "No saved clocks", detail: "Add a city, office, trip, or routine clock.", systemImage: "clock.badge.plus")
            }
        } else if store.settings.groupedClocks {
            groupedClockList
        } else {
            Section("Saved Clocks") {
                ForEach(store.clocks) { clock in
                    ClockRow(clock: clock, now: now, onEdit: { editingClock = clock })
                }
                .onMove(perform: store.reorderClocks)
            }
        }
    }

    private var groupedClockList: some View {
        ForEach(clockGroupBuckets(), id: \.id) { bucket in
            Section {
                if !bucket.collapsed {
                    ForEach(bucket.items) { clock in
                        ClockRow(clock: clock, now: now, onEdit: { editingClock = clock })
                    }
                }
            } header: {
                Button {
                    if bucket.id != 0 {
                        store.setGroupCollapsed(kind: .clocks, id: bucket.id, collapsed: !bucket.collapsed)
                    }
                } label: {
                    SectionHeader(title: bucket.name, subtitle: "\(bucket.items.count) clocks")
                }
            }
        }
    }

    private func clockGroupBuckets() -> [ClockBucket] {
        var result = store.clockGroups.sorted { $0.sortOrder < $1.sortOrder }.map {
            ClockBucket(id: $0.id, name: $0.name, collapsed: $0.collapsed, items: store.clocks.filter { $0.groupId == $0.id })
        }
        let groupedIds = Set(store.clockGroups.map(\.id))
        let ungrouped = store.clocks.filter { clock in
            guard let id = clock.groupId else { return true }
            return !groupedIds.contains(id)
        }
        result.append(ClockBucket(id: 0, name: "Ungrouped", collapsed: false, items: ungrouped))
        return result.filter { !$0.items.isEmpty || $0.id != 0 }
    }
}

private struct ClockBucket {
    let id: Int64
    let name: String
    let collapsed: Bool
    let items: [ClockEntry]
}

struct ClockRow: View {
    @EnvironmentObject private var store: ZoneAnchorStore
    let clock: ClockEntry
    let now: Date
    let onEdit: () -> Void

    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 3) {
                Text(clock.displayLabel)
                    .font(.headline)
                Text("\(TimeFormat.weekdayDate(now, zoneId: clock.zoneId)) - \(clock.zoneId)")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer()
            Text(TimeFormat.clock(now, zoneId: clock.zoneId, seconds: false))
                .font(.title3)
                .monospacedDigit()
        }
        .swipeActions {
            Button(role: .destructive) { store.deleteClock(clock.id) } label: {
                Label("Delete", systemImage: "trash")
            }
            Button { onEdit() } label: {
                Label("Edit", systemImage: "pencil")
            }
            .tint(.blue)
        }
    }
}

struct ClockEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: ZoneAnchorStore
    @State private var draft: ClockEntry
    @State private var showingZonePicker = false

    init(clock: ClockEntry) {
        _draft = State(initialValue: clock)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Clock") {
                    TextField("Label", text: $draft.label)
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
                }

                Section("Organization") {
                    GroupPicker(kind: .clocks, groupId: $draft.groupId)
                }
            }
            .navigationTitle(draft.id == 0 ? "Add Clock" : "Edit Clock")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Cancel") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Save") {
                        store.upsertClock(draft)
                        dismiss()
                    }
                }
            }
            .sheet(isPresented: $showingZonePicker) {
                ZonePickerSheet(zones: TimeZones.all, selection: $draft.zoneId)
            }
        }
    }
}
