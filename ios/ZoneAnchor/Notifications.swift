import Foundation
import UIKit
import UserNotifications

enum NotificationScheduler {
    private static let alarmCategory = "zoneanchor.alarm"
    private static let timerCategory = "zoneanchor.timer"

    static func configureCategories() {
        let snooze1 = UNNotificationAction(identifier: "alarm.snooze.1", title: "Snooze 1m")
        let snooze5 = UNNotificationAction(identifier: "alarm.snooze.5", title: "Snooze 5m")
        let snooze10 = UNNotificationAction(identifier: "alarm.snooze.10", title: "Snooze 10m")
        let dismiss = UNNotificationAction(identifier: "alarm.dismiss", title: "Dismiss", options: [.destructive])
        let alarm = UNNotificationCategory(
            identifier: alarmCategory,
            actions: [snooze1, snooze5, snooze10, dismiss],
            intentIdentifiers: [],
            options: [.customDismissAction]
        )
        let timer = UNNotificationCategory(identifier: timerCategory, actions: [], intentIdentifiers: [])
        UNUserNotificationCenter.current().setNotificationCategories([alarm, timer])
    }

    static func requestAuthorization(completion: @escaping () -> Void) {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { _, _ in
            completion()
        }
    }

    static func scheduleAlarm(_ alarm: AlarmEntry) {
        cancelAlarm(id: alarm.id)
        guard alarm.enabled else { return }
        if alarm.isSnoozed(), let snooze = alarm.snoozeUntilMillis {
            scheduleAlarmRequest(alarm: alarm, fireMillis: snooze, suffix: "snooze")
            return
        }

        var fromMillis = Date.millis
        for index in 0..<32 {
            guard let fireMillis = AlarmMath.nextTriggerMillis(for: alarm, fromMillis: fromMillis) else { break }
            scheduleAlarmRequest(alarm: alarm, fireMillis: fireMillis, suffix: "\(index)")
            if alarm.effectiveRepeatType == .once { break }
            fromMillis = fireMillis + 60_000
        }
    }

    static func cancelAlarm(id: Int64) {
        let ids = (0..<32).map { "alarm.\(id).\($0)" } + ["alarm.\(id).snooze"]
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ids)
    }

    static func scheduleTimer(_ timer: ChronoTimer) {
        cancelTimer(id: timer.id)
        guard timer.state == .running, let endsAtMillis = timer.endsAtMillis, endsAtMillis > Date.millis else { return }
        let content = UNMutableNotificationContent()
        content.title = timer.label.trimmed.isEmpty ? "Timer finished" : timer.label
        content.body = "Time is up."
        content.categoryIdentifier = timerCategory
        content.sound = .default
        content.userInfo = ["timerId": timer.id]
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .timeSensitive
            content.relevanceScore = 1.0
        }
        let interval = max(1, TimeInterval(endsAtMillis - Date.millis) / 1000)
        let request = UNNotificationRequest(
            identifier: "timer.\(timer.id)",
            content: content,
            trigger: UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        )
        UNUserNotificationCenter.current().add(request)
    }

    static func cancelTimer(id: Int64) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["timer.\(id)"])
    }

    private static func scheduleAlarmRequest(alarm: AlarmEntry, fireMillis: Int64, suffix: String) {
        let content = UNMutableNotificationContent()
        content.title = alarm.label.trimmed.isEmpty ? "ZoneAnchor alarm" : alarm.label
        content.body = "\(String(format: "%02d:%02d", alarm.hour, alarm.minute)) - \(alarm.zoneId)"
        content.categoryIdentifier = alarmCategory
        content.sound = alarm.soundEnabled ? .default : nil
        content.userInfo = ["alarmId": alarm.id]
        if #available(iOS 15.0, *) {
            content.interruptionLevel = .timeSensitive
            content.relevanceScore = 1.0
        }
        let interval = max(1, TimeInterval(fireMillis - Date.millis) / 1000)
        let request = UNNotificationRequest(
            identifier: "alarm.\(alarm.id).\(suffix)",
            content: content,
            trigger: UNTimeIntervalNotificationTrigger(timeInterval: interval, repeats: false)
        )
        UNUserNotificationCenter.current().add(request)
    }
}

@MainActor
final class NotificationRouter: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationRouter()
    weak var store: ZoneAnchorStore?

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        [.banner, .list, .sound]
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse
    ) async {
        let userInfo = response.notification.request.content.userInfo
        guard let alarmId = userInfo["alarmId"] as? Int64 else { return }
        await MainActor.run {
            switch response.actionIdentifier {
            case "alarm.snooze.1":
                store?.snoozeAlarm(id: alarmId, minutes: 1)
            case "alarm.snooze.5":
                store?.snoozeAlarm(id: alarmId, minutes: 5)
            case "alarm.snooze.10":
                store?.snoozeAlarm(id: alarmId, minutes: 10)
            default:
                store?.cancelSnooze(id: alarmId)
            }
        }
    }
}

final class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = NotificationRouter.shared
        return true
    }
}
