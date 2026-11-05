import FirebaseAnalytics
import FirebaseCore
import FirebaseCrashlytics
import Shared

/// Sends the shared game's crash reports and gameplay stats to Firebase.
/// The shared `Telemetry` object decides what may be sent; this class only forwards it.
/// Info.plist starts both collectors off, so nothing is sent before the player's saved choice is applied.
final class FirebaseTelemetry: NSObject, TelemetryBackend {
    static func start() {
        FirebaseApp.configure()
        Telemetry.shared.install(backend: FirebaseTelemetry())
    }

    func setAnalyticsEnabled(enabled: Bool) {
        // No ads in this game: ad storage and ad signals stay denied either way.
        Analytics.setConsent([
            .analyticsStorage: enabled ? .granted : .denied,
            .adStorage: .denied,
            .adUserData: .denied,
            .adPersonalization: .denied,
        ])
        Analytics.setAnalyticsCollectionEnabled(enabled)
    }

    func setCrashReportsEnabled(enabled: Bool) {
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(enabled)
    }

    func logEvent(name: String, params: [String: String]) {
        Analytics.logEvent(name, parameters: params)
    }

    func log(message: String) {
        Crashlytics.crashlytics().log(message)
    }

    func recordException(name: String, reason: String, stack: [String]) {
        let model = ExceptionModel(name: name, reason: reason)
        model.stackTrace = stack.map { StackFrame(symbol: $0, file: "", line: 0) }
        Crashlytics.crashlytics().record(exceptionModel: model)
    }
}
