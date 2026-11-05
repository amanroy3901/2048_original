import SwiftUI
import Shared

/// Hosts the shared Compose Multiplatform game (the same Kotlin UI as Android).
struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        // Store screenshots: `simctl launch … -route timeAttack` opens that screen directly.
        let args = ProcessInfo.processInfo.arguments
        if let i = args.firstIndex(of: "-route"), i + 1 < args.count {
            Nav.shared.navigate(r: args[i + 1])
        }
        return MainViewControllerKt.MainViewController()
    }
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

@main
struct iOSApp: App {
    init() {
        FirebaseTelemetry.start()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
                .preferredColorScheme(.dark)
        }
    }
}
