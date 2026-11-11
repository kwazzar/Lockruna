import SwiftUI
import Shared

@MainActor
final class AppModel: ObservableObject {
    @Published var status: String = "PassKeeper \(SharedCore.shared.VERSION)"

    init() {
        LockrunaIos.shared.start(baseUrl: "http://localhost:8080")
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
