import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: ChronoplexStore
    @State private var showingAbout = false

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

                Section("About") {
                    Button("About Chronoplex") {
                        showingAbout = true
                    }
                }
            }
            .navigationTitle("Settings")
            .alert("Chronoplex", isPresented: $showingAbout) {
                Button("OK", role: .cancel) {}
            } message: {
                Text("Version 0.1.0\nA timezone-aware time-management app for clocks, alarms, timers, and stopwatches.\nCopyright 2026 Chronoplex.")
            }
        }
    }
}
