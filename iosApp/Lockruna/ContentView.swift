import SwiftUI
import Shared
import UIKit

struct ContentView: View {
    @ObservedObject var model: AppModel
    @Environment(\.scenePhase) private var scenePhase
    @State private var route: String = "list"
    @State private var selectedId: String? = nil
    @State private var revealed: String? = nil

    var body: some View {
        ZStack {
            Group {
                switch model.screen {
                case "":
                    ProgressView()
                case "unlock":
                    UnlockView(model: model)
                case "login":
                    LoginView(model: model)
                case "register":
                    RegisterView(model: model)
                case "recovery":
                    RecoveryView(model: model)
                case "devices":
                    DevicesView(model: model)
                case "settings":
                    SettingsView(model: model)
                default:
                    VaultContainer(model: model, route: $route, selectedId: $selectedId)
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
                model.sync()
                if let ts = model.backgroundedAt {
                    let mono = Int64(ts.timeIntervalSince1970 * 1000)
                    Task { try await model.sessionStore.dispatch(intent: PresentationSessionIntentResumed(backgroundedAtMonotonicMillis: KotlinLong(value: mono))) }
                    model.backgroundedAt = nil
                }
            }
        }
    }
}

struct UnlockView: View {
    @ObservedObject var model: AppModel
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var task: Task<Void, Error>?

    var body: some View {
        VStack(spacing: 12) {
            Text("Unlock").font(.title)
            TextField("Email", text: $email).textFieldStyle(.roundedBorder)
            SecureField("Password", text: $password).textFieldStyle(.roundedBorder)
            Button("Unlock") {
                task?.cancel()
                task = Task {
                    do {
                        try await model.unlockStore.dispatch(intent: PresentationUnlockIntentEmailChanged(value: email))
                        try await model.unlockStore.dispatch(intent: PresentationUnlockIntentPasswordChanged(value: password))
                        try await model.unlockStore.dispatch(intent: PresentationUnlockIntentSubmitPassword())
                    } catch {
                        print("unlock failed: \(error)")
                    }
                }
            }
            if model.unlockStore.state.value.biometricAvailable {
                Button("Use biometrics") {
                    Task {
                        do {
                            try await model.unlockStore.dispatch(intent: PresentationUnlockIntentSubmitBiometrics())
                        } catch {
                            print("biometric unlock failed: \(error)")
                        }
                    }
                }
            }
            if model.unlockStore.state.value.error != nil {
                Text("Invalid credentials").foregroundColor(.red)
            }
        }.padding()
        .onDisappear { task?.cancel() }
        // The vault key opened: leave the unlock screen.
        .onChange(of: model.vaultUnlocked) { _, unlocked in
            if unlocked && model.screen == "unlock" {
                password = ""
                model.screen = "vault"
                model.sync()
            }
        }
    }
}

struct LoginView: View {
    @ObservedObject var model: AppModel
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var deviceName: String = UIDevice.current.name
    @State private var task: Task<Void, Error>?

    var body: some View {
        VStack(spacing: 12) {
            Text("Sign in").font(.title)
            TextField("Email", text: $email).textFieldStyle(.roundedBorder)
            SecureField("Password", text: $password).textFieldStyle(.roundedBorder)
            TextField("Device name", text: $deviceName).textFieldStyle(.roundedBorder)
            Button("Sign in") {
                task?.cancel()
                task = Task {
                    do {
                        try await model.loginStore.dispatch(intent: PresentationLoginIntentEmailChanged(value: email))
                        try await model.loginStore.dispatch(intent: PresentationLoginIntentPasswordChanged(value: password))
                        try await model.loginStore.dispatch(intent: PresentationLoginIntentDeviceNameChanged(value: deviceName))
                        try await model.loginStore.dispatch(intent: PresentationLoginIntentSubmit())
                    } catch {
                        print("sign in failed: \(error)")
                    }
                }
            }
            Button("Create account") { model.screen = "register" }
            Button("Lost access") { model.screen = "recovery" }
            if model.loginStore.state.value.error != nil {
                Text("Login failed").foregroundColor(.red)
            }
        }.padding()
        .onDisappear { task?.cancel() }
        .onChange(of: model.vaultUnlocked) { _, unlocked in
            if unlocked && model.screen == "login" {
                password = ""
                model.screen = "vault"
                model.sync()
            }
        }
    }
}

struct VaultContainer: View {
    @ObservedObject var model: AppModel
    @Binding var route: String
    @Binding var selectedId: String?

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
            List(model.vaultListStore.state.value.items, id: \.id) { item in
                Button(item.title) {
                    selectedId = item.id
                    Task {
                        try await model.vaultListStore.dispatch(intent: PresentationVaultListIntentItemClicked(id: item.id))
                    }
                    route = "detail"
                }
            }
            HStack {
                Button("Lock") {
                    Task {
                        try await model.sessionStore.dispatch(intent: PresentationSessionIntentLock())
                        model.screen = "unlock"
                    }
                }
                Button("Sync") { model.sync() }
                Button("Devices") { model.screen = "devices" }
                Button("Settings") { model.screen = "settings" }
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
            Text(model.itemDetailStore.state.value.item?.title ?? "").font(.title)
            Text(model.itemDetailStore.state.value.item?.username ?? "")
            Button("Reveal") { Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentReveal()) } }
            Button("Hide") { Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentHide()) } }
            Button("Copy") { Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentCopy()) } }
            Button("Edit") { Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentEdit()) } }
            Button("Delete") { Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentDeleteConfirmed()) } }
            Button("Back") { route = "list" }
        }.padding()
    }
}

struct ItemEditView: View {
    @ObservedObject var model: AppModel
    @Binding var route: String
    @State private var title: String = ""
    @State private var typeIndex: Int = 0

    private let types: [(String, DomainVaultItemType)] = [("Login", .login), ("Card", .card), ("Note", .note)]

    var body: some View {
        VStack {
            Text("Edit").font(.title)
            Picker("Type", selection: $typeIndex) {
                ForEach(types.indices, id: \.self) { index in
                    Text(types[index].0).tag(index)
                }
            }
            .onChange(of: typeIndex) { _, index in
                Task {
                    try await model.itemEditStore.dispatch(
                        intent: PresentationItemEditIntentTypeChanged(type: types[index].1)
                    )
                }
            }
            TextField("Title", text: $title).textFieldStyle(.roundedBorder)
            Button("Save") {
                Task {
                    try await model.itemEditStore.dispatch(intent: PresentationItemEditIntentTitleChanged(value: title))
                    try await model.itemEditStore.dispatch(intent: PresentationItemEditIntentSave())
                    route = "list"
                    model.sync()
                }
            }
            Button("Back") { route = "list" }
        }.padding()
    }
}
