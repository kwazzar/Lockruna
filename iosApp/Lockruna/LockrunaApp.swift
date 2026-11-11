import SwiftUI
import Shared

@MainActor
final class AppModel: ObservableObject {
    @Published var screen: String = "unlock"
    @Published var backgroundedAt: Date? = nil
    @Published var showBlur: Bool = false

    let sessionStore: SessionStore
    let loginStore: LoginStore
    let unlockStore: UnlockStore
    let vaultListStore: VaultListStore
    let itemDetailStore: ItemDetailStore
    let itemEditStore: ItemEditStore

    private var sessionTask: Task<Void, Never>?

    init() {
        LockrunaIos.shared.start(baseUrl: "http://localhost:8080")
        sessionStore = LockrunaIos.shared.sessionStore()
        loginStore = LockrunaIos.shared.loginStore()
        unlockStore = LockrunaIos.shared.unlockStore()
        vaultListStore = LockrunaIos.shared.vaultListStore()
        itemDetailStore = LockrunaIos.shared.itemDetailStore()
        itemEditStore = LockrunaIos.shared.itemEditStore()

        sessionTask = Task { @MainActor in
            for await effect in sessionStore.effects {
                if effect is SessionEffectLocked {
                    screen = "unlock"
                }
            }
        }
    }

    deinit { sessionTask?.cancel() }
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
