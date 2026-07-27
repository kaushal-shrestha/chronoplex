import SwiftUI

@main
struct ChronoplexApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var store = ChronoplexStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
                .tint(store.settings.palette.accent)
                .preferredColorScheme(store.settings.appearance.colorScheme)
        }
    }
}
