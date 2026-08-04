import SwiftUI
import UniformTypeIdentifiers

struct SettingsView: View {
    @EnvironmentObject private var store: ZoneAnchorStore
    @State private var showingAbout = false
    @State private var showingExporter = false
    @State private var showingImporter = false
    @State private var backupDocument = BackupFileDocument()
    @State private var backupStatus: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Appearance") {
                    Picker("Mode", selection: $store.settings.appearance) {
                        ForEach(AppearanceMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                    Picker("Palette", selection: $store.settings.palette) {
                        ForEach(ThemePalette.allCases) { palette in
                            Label(palette.title, systemImage: "circle.fill")
                                .foregroundStyle(palette.accent)
                                .tag(palette)
                        }
                    }
                }

                Section("Alarms") {
                    Picker("Default zone source", selection: $store.settings.defaultZoneSource) {
                        ForEach(ZoneSource.allCases) { source in
                            Text(source.title).tag(source)
                        }
                    }
                    Picker("Zone display", selection: $store.settings.alarmZoneDisplay) {
                        ForEach(AlarmZoneDisplay.allCases) { display in
                            Text(display.title).tag(display)
                        }
                    }
                    Picker("First day of week", selection: $store.settings.firstDayOfWeek) {
                        ForEach(FirstDayOfWeek.allCases) { firstDay in
                            Text(firstDay.title).tag(firstDay)
                        }
                    }
                    Button("Refresh Notification Status") {
                        store.refreshNotificationStatus()
                    }
                    if !store.notificationsAuthorized {
                        Button("Enable Notifications") {
                            store.requestNotifications()
                        }
                    }
                }

                Section("Timers") {
                    Picker("Default finish behavior", selection: $store.settings.defaultTimerFinishMode) {
                        ForEach(TimerFinishMode.allCases) { mode in
                            Text(mode.title).tag(mode)
                        }
                    }
                }

                Section("Grouping") {
                    Toggle("Group clocks", isOn: $store.settings.groupedClocks)
                    Toggle("Group alarms", isOn: $store.settings.groupedAlarms)
                    Toggle("Group timers", isOn: $store.settings.groupedTimers)
                    Toggle("Group stopwatches", isOn: $store.settings.groupedStopwatches)
                }

                Section("Backup") {
                    Button("Export Backup") {
                        do {
                            backupDocument = BackupFileDocument(data: try store.exportBackupData())
                            showingExporter = true
                        } catch {
                            backupStatus = error.localizedDescription
                        }
                    }
                    Button("Import Backup") {
                        showingImporter = true
                    }
                    if let backupStatus {
                        Text(backupStatus)
                            .foregroundStyle(.secondary)
                    }
                }

                Section("About") {
                    Button("About ZoneAnchor") {
                        showingAbout = true
                    }
                }
            }
            .navigationTitle("Settings")
            .alert("ZoneAnchor", isPresented: $showingAbout) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Version 0.1.0\nA timezone-aware time-management app for clocks, alarms, timers, and stopwatches.\nCopyright 2026 ZoneAnchor.")
            }
            .fileExporter(
                isPresented: $showingExporter,
                document: backupDocument,
                contentType: .json,
                defaultFilename: "zoneanchor-backup-\(Self.todayString).json"
            ) { result in
                switch result {
                case .success:
                    backupStatus = "Backup saved."
                case .failure(let error):
                    backupStatus = error.localizedDescription
                }
            }
            .fileImporter(isPresented: $showingImporter, allowedContentTypes: [.json]) { result in
                switch result {
                case .success(let url):
                    let scoped = url.startAccessingSecurityScopedResource()
                    defer {
                        if scoped { url.stopAccessingSecurityScopedResource() }
                    }
                    do {
                        backupStatus = try store.importBackupData(Data(contentsOf: url))
                    } catch {
                        backupStatus = error.localizedDescription
                    }
                case .failure(let error):
                    backupStatus = error.localizedDescription
                }
            }
        }
    }

    private static var todayString: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: Date())
    }
}
