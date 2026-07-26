import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var store: ChronoplexStore
    @State private var now = Date()

    var body: some View {
        TabView {
            ClocksView(now: now)
                .tabItem { Label("Clocks", systemImage: "clock") }
            AlarmsView(now: now)
                .tabItem { Label("Alarms", systemImage: "alarm") }
            TimersView(now: now)
                .tabItem { Label("Timers", systemImage: "hourglass") }
            StopwatchesView(now: now)
                .tabItem { Label("Stopwatch", systemImage: "stopwatch") }
            SettingsView()
                .tabItem { Label("Settings", systemImage: "gearshape") }
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { date in
            now = date
            store.finishExpiredTimers(nowMillis: Date.millis)
        }
    }
}

struct SectionHeader: View {
    var title: String
    var subtitle: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.headline)
            if let subtitle {
                Text(subtitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

struct EmptyStateView: View {
    var title: String
    var detail: String
    var systemImage: String

    var body: some View {
        ContentUnavailableView(title, systemImage: systemImage, description: Text(detail))
    }
}

struct GroupPicker: View {
    @EnvironmentObject private var store: ChronoplexStore
    let kind: EntityKind
    @Binding var groupId: Int64?

    var body: some View {
        Picker("Group", selection: Binding(
            get: { groupId ?? 0 },
            set: { groupId = $0 == 0 ? nil : $0 }
        )) {
            Text("Ungrouped").tag(Int64(0))
            ForEach(store.groups(for: kind)) { group in
                Text(group.name).tag(group.id)
            }
        }
    }
}

struct ManageGroupsSheet: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var store: ChronoplexStore
    let kind: EntityKind
    @State private var name = ""
    @State private var renameTarget: ItemGroup?
    @State private var renameText = ""

    var body: some View {
        NavigationStack {
            List {
                Section("New group") {
                    HStack {
                        TextField("Name", text: $name)
                        Button("Add") {
                            store.createGroup(kind: kind, name: name)
                            name = ""
                        }
                        .disabled(name.trimmed.isEmpty)
                    }
                }

                Section("Groups") {
                    ForEach(store.groups(for: kind)) { group in
                        HStack {
                            Button {
                                store.setGroupCollapsed(kind: kind, id: group.id, collapsed: !group.collapsed)
                            } label: {
                                Image(systemName: group.collapsed ? "chevron.right" : "chevron.down")
                            }
                            .buttonStyle(.plain)
                            Text(group.name)
                            Spacer()
                            Button("Rename") {
                                renameTarget = group
                                renameText = group.name
                            }
                        }
                        .swipeActions {
                            Button(role: .destructive) {
                                store.deleteGroup(kind: kind, id: group.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                }
            }
            .navigationTitle("Groups")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Done") { dismiss() }
                }
            }
            .alert("Rename group", isPresented: Binding(
                get: { renameTarget != nil },
                set: { if !$0 { renameTarget = nil } }
            )) {
                TextField("Name", text: $renameText)
                Button("Save") {
                    if let renameTarget {
                        store.renameGroup(kind: kind, id: renameTarget.id, name: renameText)
                    }
                    renameTarget = nil
                }
                Button("Cancel", role: .cancel) { renameTarget = nil }
            }
        }
    }
}
