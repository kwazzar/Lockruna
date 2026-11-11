import SwiftUI
import Shared

struct ContentView: View {
    @ObservedObject var model: AppModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var route: String = "list"
    @State private var selectedId: String? = nil
    @State private var revealed: String? = nil

    var body: some View {
        ZStack {
            Group {
                if model.screen == "unlock" {
                    UnlockView(model: model)
                } else if model.screen == "login" {
                    LoginView(model: model)
                } else {
                    VaultContainer(model: model, route: $route, selectedId: $selectedId, revealed: $revealed)
                }
            }
            if model.showBlur {
                Color.black.opacity(0.85).ignoresSafeArea()
                    .overlay(Text("PassKeeper").foregroundColor(.white))
            }
        }
        .onChange(of: scenePhase) { _, newPhase in
            if newPhase == .background {
                model.showBlur = true
                model.backgroundedAt = Date()
            } else if newPhase == .active {
                model.showBlur = false
                if let ts = model.backgroundedAt {
                    let mono = Int64(ts.timeIntervalSince1970 * 1000)
                    Task { await model.sessionStore.dispatch(intent: SessionIntentResumed(backgroundedAtMonotonicMillis: mono)) }
                }
            }
        }
    }
}

struct UnlockView: View {
    @ObservedObject var model: AppModel
    @State private var email: String = ""
    @State private var password: String = ""

    var body: some View {
        VStack(spacing: 12) {
            Text("Unlock").font(.title)
            TextField("Email", text: $email).textFieldStyle(.roundedBorder)
            SecureField("Password", text: $password).textFieldStyle(.roundedBorder)
            Button("Unlock") {
                Task {
                    await model.unlockStore.dispatch(intent: UnlockIntentEmailChanged(value: email))
                    await model.unlockStore.dispatch(intent: UnlockIntentPasswordChanged(value: password))
                    await model.unlockStore.dispatch(intent: UnlockIntentSubmitPassword())
                }
            }
            if model.unlockStore.state.biometricAvailable {
                Button("Use biometrics") {
                    Task { await model.unlockStore.dispatch(intent: UnlockIntentSubmitBiometrics()) }
                }
            }
            if let err = model.unlockStore.state.error {
                Text("Invalid credentials").foregroundColor(.red)
            }
        }.padding()
    }
}

struct LoginView: View {
    @ObservedObject var model: AppModel
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var deviceName: String = "iPhone"

    var body: some View {
        VStack(spacing: 12) {
            Text("Sign in").font(.title)
            TextField("Email", text: $email).textFieldStyle(.roundedBorder)
            SecureField("Password", text: $password).textFieldStyle(.roundedBorder)
            TextField("Device name", text: $deviceName).textFieldStyle(.roundedBorder)
            Button("Sign in") {
                Task {
                    await model.loginStore.dispatch(intent: LoginIntentEmailChanged(value: email))
                    await model.loginStore.dispatch(intent: LoginIntentPasswordChanged(value: password))
                    await model.loginStore.dispatch(intent: LoginIntentDeviceNameChanged(value: deviceName))
                    await model.loginStore.dispatch(intent: LoginIntentSubmit())
                }
            }
        }.padding()
    }
}

struct VaultContainer: View {
    @ObservedObject var model: AppModel
    @Binding var route: String
    @Binding var selectedId: String?
    @Binding var revealed: String?

    var body: some View {
        if route == "list" {
            VaultListView(model: model, route: $route, selectedId: $selectedId)
        } else if route == "detail" {
            ItemDetailView(model: model, route: $route)
        } else {
            ItemEditView(model: model, route: $route)
        }
    }
}

struct VaultListView: View {
    @ObservedObject var model: AppModel
    @Binding var route: String
    @Binding var selectedId: String?

    var body: some View {
        VStack {
            Text("Vault").font(.title)
            List(model.vaultListStore.state.items, id: \.id) { item in
                Button(item.title) {
                    selectedId = item.id
                    Task { await model.vaultListStore.dispatch(intent: VaultListIntentItemClicked(id: item.id)) }
                    route = "detail"
                }
            }
            Button("Lock") {
                Task { await model.sessionStore.dispatch(intent: SessionIntentLock()) }
                model.screen = "unlock"
            }
            Button("New item") { route = "edit" }
        }.padding()
    }
}

struct ItemDetailView: View {
    @ObservedObject var model: AppModel
    @Binding var route: String

    var body: some View {
        VStack {
            Text(model.itemDetailStore.state.item?.title ?? "").font(.title)
            Text(model.itemDetailStore.state.item?.username ?? "")
            Button("Reveal") { Task { await model.itemDetailStore.dispatch(intent: ItemDetailIntentReveal()) } }
            Button("Hide") { Task { await model.itemDetailStore.dispatch(intent: ItemDetailIntentHide()) } }
            Button("Copy") { Task { await model.itemDetailStore.dispatch(intent: ItemDetailIntentCopy()) } }
            Button("Edit") { Task { await model.itemDetailStore.dispatch(intent: ItemDetailIntentEdit()) } }
            Button("Delete") { Task { await model.itemDetailStore.dispatch(intent: ItemDetailIntentDeleteConfirmed()) } }
            Button("Back") { route = "list" }
        }.padding()
    }
}

struct ItemEditView: View {
    @ObservedObject var model: AppModel
    @Binding var route: String
    @State private var title: String = ""

    var body: some View {
        VStack {
            Text("Edit").font(.title)
            TextField("Title", text: $title).textFieldStyle(.roundedBorder)
            Button("Save") {
                Task {
                    await model.itemEditStore.dispatch(intent: ItemEditIntentTitleChanged(value: title))
                    await model.itemEditStore.dispatch(intent: ItemEditIntentSave())
                }
            }
            Button("Back") { route = "list" }
        }.padding()
    }
}
