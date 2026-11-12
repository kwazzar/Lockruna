import SwiftUI
import Network
import Shared

@MainActor
final class AppModel: ObservableObject {
    @Published var screen: String = "unlock"
    @Published var backgroundedAt: Date? = nil
    @Published var showBlur: Bool = false

    let sessionStore: PresentationSessionStore
    let loginStore: PresentationLoginStore
    let unlockStore: PresentationUnlockStore
    let registerStore: PresentationRegisterStore
    let recoveryStore: PresentationRecoveryStore
    let vaultListStore: PresentationVaultListStore
    let itemDetailStore: PresentationItemDetailStore
    let itemEditStore: PresentationItemEditStore
    let devicesStore: PresentationDevicesStore
    let settingsStore: PresentationSettingsStore

    private var sessionTask: Task<Void, Never>?
    private let monitor = NWPathMonitor()
    private let monitorQueue = DispatchQueue(label: "lockruna.network")

    init() {
        LockrunaIos.shared.start(baseUrl: "http://localhost:8080")
        sessionStore = LockrunaIos.shared.sessionStore()
        loginStore = LockrunaIos.shared.loginStore()
        unlockStore = LockrunaIos.shared.unlockStore()
        registerStore = LockrunaIos.shared.registerStore()
        recoveryStore = LockrunaIos.shared.recoveryStore()
        vaultListStore = LockrunaIos.shared.vaultListStore()
        itemDetailStore = LockrunaIos.shared.itemDetailStore()
        itemEditStore = LockrunaIos.shared.itemEditStore()
        devicesStore = LockrunaIos.shared.devicesStore()
        settingsStore = LockrunaIos.shared.settingsStore()

        sessionTask = Task { @MainActor in
            for await effect in sessionStore.effects {
                if effect is PresentationSessionEffectLocked {
                    screen = "unlock"
                }
            }
        }
        startNetworkSync()
    }

    deinit {
        sessionTask?.cancel()
        monitor.cancel()
    }

    /// Sync on unlock, resume, after a local save, by hand and when the network comes back (US-09).
    func sync() {
        Task.detached(priority: .utility) {
            _ = try? await LockrunaIos.shared.sync()
        }
    }

    private func startNetworkSync() {
        monitor.pathUpdateHandler = { [weak self] path in
            if path.status == .satisfied {
                self?.sync()
            }
        }
        monitor.start(queue: monitorQueue)
    }
}

@main
struct LockrunaApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            ContentView(model: model)
        }
    }
}
