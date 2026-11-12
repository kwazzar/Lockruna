import SwiftUI
import Network
import Shared

@MainActor
final class AppModel: ObservableObject {
    @Published var screen: String = ""
    @Published var backgroundedAt: Date? = nil
    @Published var showBlur: Bool = false
    /// Sampled store state. SKIE does not deliver Flow events to SwiftUI here, so the
    /// UI reacts to sampled values instead of an AsyncSequence over a Kotlin channel.
    @Published var vaultUnlocked: Bool = false

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
    private var routeTask: Task<Void, Error>?
    private var samplerTask: Task<Void, Never>?
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
        // A session is stored → unlock; none → sign in, which is where a new user creates
        // an account or restores access (US-01, US-03).
        routeTask = Task { @MainActor in
            let hasSession = (try? await LockrunaIos.shared.hasStoredSession()) ?? false
            screen = hasSession.boolValue ? "unlock" : "login"
        }
        samplerTask = Task { @MainActor [weak self] in
            while !(Task.isCancelled) {
                guard let self else { return }
                self.sample()
                try? await Task.sleep(nanoseconds: 250_000_000)
            }
        }
        startNetworkSync()
    }

    @MainActor private func sample() {
        vaultUnlocked = sessionStore.state.value.isLocked == false
    }

    deinit {
        sessionTask?.cancel()
        routeTask?.cancel()
        samplerTask?.cancel()
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
                Task { @MainActor in self?.sync() }
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
