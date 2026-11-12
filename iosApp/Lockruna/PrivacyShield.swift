import SwiftUI
import UIKit

/// Privacy shield for US-06: hides app content while the screen is captured
/// (screenshot / screen recording) and blurs the window in the app switcher.
struct PrivacyShield {
    static let tag = 987_654

    static func protect() {
        let windows: [UIWindow] = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap { $0.windows }
        for window in windows {
            if window.viewWithTag(tag) != nil { continue }
            let shield = UIView(frame: window.bounds)
            shield.tag = tag
            shield.backgroundColor = .black
            shield.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            let label = UILabel()
            label.text = "PassKeeper"
            label.textColor = .white
            label.font = .preferredFont(forTextStyle: .title1)
            label.translatesAutoresizingMaskIntoConstraints = false
            shield.addSubview(label)
            NSLayoutConstraint.activate([
                label.centerXAnchor.constraint(equalTo: shield.centerXAnchor),
                label.centerYAnchor.constraint(equalTo: shield.centerYAnchor),
            ])
            window.addSubview(shield)
            window.bringSubviewToFront(shield)
        }
    }

    static func unprotect() {
        for scene in UIApplication.shared.connectedScenes {
            guard let windowScene = scene as? UIWindowScene else { continue }
            for window in windowScene.windows {
                window.viewWithTag(tag)?.removeFromSuperview()
            }
        }
    }
}

struct PrivacyOverlay: View {
    var body: some View {
        ZStack {
            Color.black.ignoresSafeArea()
            Text("PassKeeper").foregroundColor(.white)
        }
    }
}

struct PrivacyShieldModifier: ViewModifier {
    @Environment(\.scenePhase) private var scenePhase
    @State private var isCaptured: Bool = UIScreen.main.isCaptured

    func body(content: Content) -> some View {
        ZStack {
            content
            if isCaptured {
                PrivacyOverlay()
            }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .background || phase == .inactive {
                PrivacyShield.protect()
            } else if phase == .active {
                PrivacyShield.unprotect()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: UIScreen.capturedDidChangeNotification)) { _ in
            isCaptured = UIScreen.main.isCaptured
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.willResignActiveNotification)) { _ in
            PrivacyShield.protect()
        }
        .onReceive(NotificationCenter.default.publisher(for: UIApplication.didBecomeActiveNotification)) { _ in
            PrivacyShield.unprotect()
        }
    }
}

extension View {
    func privacyShield() -> some View {
        modifier(PrivacyShieldModifier())
    }
}