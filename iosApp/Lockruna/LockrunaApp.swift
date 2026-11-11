import SwiftUI
import Shared

@MainActor
final class AppModel: ObservableObject {
    @Published var screen: String = "unlock"
    @Published var backgroundedAt: Date? = nil
    @Published var showBlur: Bool = false

    let sessionStore: PresentationSessionStore
    let loginStore: PresentationLoginStore
    let unlockStore: PresentationUnlockStore
    let vaultListStore: PresentationVaultListStore
    let itemDetailStore: PresentationItemDetailStore
    let itemEditStore: PresentationItemEditStore

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
                if effect is PresentationSessionEffectLocked {
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
