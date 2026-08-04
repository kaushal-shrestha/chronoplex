import SwiftUI

@main
struct ZoneAnchorApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var store = ZoneAnchorStore()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(store)
                .tint(store.settings.palette.accent)
                .preferredColorScheme(store.settings.appearance.colorScheme)
        }
    }
}
