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
            DarkZenBackground()
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
        .preferredColorScheme(.dark)
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
        DarkZenAuthShell(title: "Unlock") {
            VStack(spacing: 12) {
                TextField("Email", text: $email)
                    .textFieldStyle(DarkZenTextFieldStyle())
                SecureField("Password", text: $password)
                    .textFieldStyle(DarkZenTextFieldStyle())
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
                .buttonStyle(DarkZenPrimaryButtonStyle(cornerRadius: 999))
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
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)
                }
                if model.unlockStore.state.value.error != nil {
                    Text("Invalid credentials")
                        .foregroundColor(DarkZen.danger)
                }
            }
        }
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
        DarkZenAuthShell(title: "Sign in") {
            VStack(spacing: 12) {
                TextField("Email", text: $email)
                    .textFieldStyle(DarkZenTextFieldStyle())
                SecureField("Password", text: $password)
                    .textFieldStyle(DarkZenTextFieldStyle())
                TextField("Device name", text: $deviceName)
                    .textFieldStyle(DarkZenTextFieldStyle())
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
                .buttonStyle(DarkZenPrimaryButtonStyle(cornerRadius: 999))
                Button("Create account") { model.screen = "register" }
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)
                Button("Lost access") { model.screen = "recovery" }
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)
                if model.loginStore.state.value.error != nil {
                    Text("Login failed")
                        .foregroundColor(DarkZen.danger)
                }
            }
        }
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
        ZStack(alignment: .topLeading) {
            VStack(alignment: .leading, spacing: 16) {
                Text("Vault").font(.title).bold()
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(model.vaultListStore.state.value.items, id: \.id) { item in
                            Button(action: {
                                selectedId = item.id
                                Task {
                                    try await model.vaultListStore.dispatch(intent: PresentationVaultListIntentItemClicked(id: item.id))
                                }
                                route = "detail"
                            }) {
                                HStack {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text(item.title)
                                            .font(.subheadline)
                                            .foregroundColor(DarkZen.textPrimary)
                                        if let username = item.username {
                                            Text(username)
                                                .font(.caption)
                                                .foregroundColor(DarkZen.textSecondary)
                                        }
                                    }
                                    Spacer()
                                }
                                .padding(14)
                                .background(DarkZen.card)
                                .cornerRadius(DarkZen.cardRadius)
                                .overlay(
                                    RoundedRectangle(cornerRadius: DarkZen.cardRadius)
                                        .stroke(DarkZen.divider, lineWidth: 2)
                                )
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    .padding(.horizontal, 14)
                }
            }

            VStack(alignment: .trailing, spacing: 16) {
                HStack {
                    Button("Lock") {
                        Task {
                            try await model.sessionStore.dispatch(intent: PresentationSessionIntentLock())
                            model.screen = "unlock"
                        }
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)

                    Button("Sync") { model.sync() }
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)

                    Button("Devices") { model.screen = "devices" }
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)

                    Button("Settings") { model.screen = "settings" }
                    .buttonStyle(.plain)
                    .foregroundColor(DarkZen.textSecondary)
                }

                Spacer(minLength: 0)

                Button("New item") { route = "edit" }
                    .buttonStyle(DarkZenPrimaryButtonStyle())
                    .frame(maxWidth: .infinity, minHeight: 50)
            }
            .padding(.horizontal, 14)
            .padding(.trailing, 4)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)
        }
    }
}

struct ItemDetailView: View {
    @ObservedObject var model: AppModel
    @Binding var route: String

    var body: some View {
        VStack(spacing: 20) {
            VStack(alignment: .leading, spacing: 8) {
                Text(model.itemDetailStore.state.value.item?.title ?? "")
                    .font(.title)
                    .bold()
                    .foregroundColor(DarkZen.textPrimary)
                Text(model.itemDetailStore.state.value.item?.username ?? "")
                    .font(.subheadline)
                    .foregroundColor(DarkZen.textSecondary)
            }
            .padding(20)
            .background(DarkZen.panel)
            .cornerRadius(DarkZen.panelRadius)

            HStack(spacing: 16) {
                Button("Reveal") {
                    Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentReveal()) }
                }
                .buttonStyle(DarkZenPrimaryButtonStyle())
                .foregroundColor(DarkZen.textPrimary)

                Button("Copy") {
                    Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentCopy()) }
                }
                .buttonStyle(.bordered)
                .tint(.clear)
                .foregroundColor(DarkZen.textSecondary)

                Button("Edit") {
                    Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentEdit()) }
                }
                .buttonStyle(.bordered)
                .tint(.clear)
                .foregroundColor(DarkZen.textSecondary)

                Button("Hide") {
                    Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentHide()) }
                }
                .buttonStyle(.plain)
                .foregroundColor(DarkZen.textSecondary)

                Button("Delete") {
                    Task { try await model.itemDetailStore.dispatch(intent: PresentationItemDetailIntentDeleteConfirmed()) }
                }
                .buttonStyle(.bordered)
                .tint(DarkZen.danger)
            }

            Button("Back") {
                withAnimation { route = "list" }
            }
            .buttonStyle(.plain)
            .foregroundColor(DarkZen.textSecondary)
            .frame(width: 44, height: 44)
            .accessibilityLabel("Back")
        }.padding(20)
    }
}

struct ItemEditView: View {
    @ObservedObject var model: AppModel
    @Binding var route: String
    @State private var title: String = ""
    @State private var typeIndex: Int = 0

    private let types: [(String, DomainVaultItemType)] = [("Login", .login), ("Card", .card), ("Note", .note), ("Folder", .folder)]

    private var folders: [(String, String)] {
        model.vaultListStore.state.value.items
            .filter { $0.type == .folder }
            .map { ($0.id, $0.title) }
    }

    var body: some View {
        VStack(spacing: 24) {
            Text("Edit").font(.title).bold()

            Picker("Type", selection: $typeIndex) {
                ForEach(types.indices, id: \.self) { index in
                    Text(types[index].0).tag(index)
                }
            }
            .pickerStyle(.menu)
            .accentColor(DarkZen.accent)

            .onChange(of: typeIndex) { _, index in
                Task {
                    try await model.itemEditStore.dispatch(
                        intent: PresentationItemEditIntentTypeChanged(type: types[index].1)
                    )
                }
            }

            TextField("Title", text: $title)
                .textFieldStyle(DarkZenTextFieldStyle())

            VStack(alignment: .leading, spacing: 8) {
                Text("Folder").font(.headline)
                if folders.isEmpty {
                    Text("No folder").font(.caption).foregroundColor(DarkZen.textSecondary)
                } else {
                    ForEach(folders, id: \.0) { folder in
                        Button {
                            Task {
                                try await model.itemEditStore.dispatch(
                                    intent: PresentationItemEditIntentFolderSelected(folderId: folder.0)
                                )
                            }
                        } label: {
                            HStack {
                                Image(systemName: model.itemEditStore.state.value.folderId == folder.0 ? "checkmark" : "")
                                    .foregroundColor(model.itemEditStore.state.value.folderId == folder.0 ? DarkZen.accent : DarkZen.textSecondary)
                                Text(folder.1)
                                    .foregroundColor(model.itemEditStore.state.value.folderId == folder.0 ? DarkZen.accent : DarkZen.textPrimary)
                                Spacer()
                            }
                            .padding(12)
                            .background(model.itemEditStore.state.value.folderId == folder.0 ? DarkZen.accent.opacity(0.15) : DarkZen.card)
                            .cornerRadius(DarkZen.cardRadius)
                            .overlay(
                                RoundedRectangle(cornerRadius: DarkZen.cardRadius)
                                    .stroke(model.itemEditStore.state.value.folderId == folder.0 ? DarkZen.accent : Color.clear, lineWidth: 2)
                            )
                            .accessibilityAddTraits(.isSelected)
                        }
                    }
                    if model.itemEditStore.state.value.folderId != nil {
                        Button {
                            Task {
                                try await model.itemEditStore.dispatch(
                                    intent: PresentationItemEditIntentFolderSelected(folderId: nil)
                                )
                            }
                        } label: {
                            HStack {
                                Text("No folder")
                                    .foregroundColor(DarkZen.textSecondary)
                                Spacer()
                            }
                            .padding(12)
                            .background(DarkZen.card)
                            .cornerRadius(DarkZen.cardRadius)
                            .overlay(
                                RoundedRectangle(cornerRadius: DarkZen.cardRadius)
                                    .stroke(DarkZen.divider, lineWidth: 1)
                            )
                        }
                    }
                }
            }

            Button("Save") {
                Task {
                    try await model.itemEditStore.dispatch(intent: PresentationItemEditIntentTitleChanged(value: title))
                    try await model.itemEditStore.dispatch(intent: PresentationItemEditIntentSave())
                    route = "list"
                    model.sync()
                }
            }
            .buttonStyle(DarkZenPrimaryButtonStyle())

            Button("Back") { route = "list" }
                .buttonStyle(.plain)
                .foregroundColor(DarkZen.textSecondary)
                .frame(width: 44, height: 44)
                .accessibilityLabel("Back")
        }.padding(20)
    }
}
