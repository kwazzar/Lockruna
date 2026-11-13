import SwiftUI
import Shared
import UIKit

/// Registration and the one-time recovery key (US-01, US-03).
///
/// The 12 words live in a view-local state only: shown once, typed back, and
/// dropped when the view disappears. They are never written to `AppModel`.
struct RegisterView: View {
    @ObservedObject var model: AppModel
    @State private var email: String = ""
    @State private var password: String = ""
    @State private var deviceName: String = UIDevice.current.name
    @State private var recoveryWords: [String] = []
    @State private var confirmation: String = ""
    @State private var task: Task<Void, Error>?

    private var store: PresentationRegisterStore { model.registerStore }

    var body: some View {
        Group {
            if recoveryWords.isEmpty {
                form
            } else {
                recovery
            }
        }
        // Only when the whole screen leaves: SwiftUI calls onDisappear on the branch it
        // replaces, so clearing there would throw the words away the moment they appear.
        .onDisappear {
            forget()
            store.close()
        }
        .onAppear { watchEffects() }
        .onChange(of: model.vaultUnlocked) { _, unlocked in
            if unlocked && model.screen == "register" {
                forget()
                store.close()
                model.screen = "vault"
                Task { await model.sync() }
            }
        }
    }

    private var form: some View {
        VStack(spacing: 12) {
            Text("Create account").font(.title)
            TextField("Email", text: $email).textFieldStyle(.roundedBorder)
            SecureField("Master password", text: $password).textFieldStyle(.roundedBorder)
            Text("At least 12 characters").font(.caption)
            if let s = store.state.value.strength ?? store.state.value.passwordStrength {
                Text("Strength: \(s.level)/4").font(.caption)
                if !s.warnings.isEmpty {
                    Text(s.warnings.joined(separator: ", ")).font(.caption).foregroundColor(.orange)
                }
            }
            TextField("Device name", text: $deviceName).textFieldStyle(.roundedBorder)
            if store.state.value.error != nil {
                Text("Registration failed").foregroundColor(.red)
            }
            if store.state.value.isSubmitting {
                ProgressView()
            } else {
                Button("Create account") { submit() }
            }
        }.padding()
        .onAppear { watchEffects() }
    }

    private var recovery: some View {
        Form {
            Section("Your recovery key") {
                Text("Write these 12 words down. Without them a forgotten password cannot be restored.")
                ForEach(Array(recoveryWords.enumerated()), id: \.offset) { index, word in
                    Text("\(index + 1). \(word)")
                }
            }
            Section {
                TextField("Type the 12 words back", text: $confirmation)
                if store.state.value.error != nil {
                    Text("The words do not match").foregroundColor(.red)
                }
                Button("I saved them") { confirm() }
            }
        }
    }

    private func submit() {
        task?.cancel()
        task = Task {
            do {
                try await store.dispatch(intent: PresentationRegisterIntentEmailChanged(value: email))
                try await store.dispatch(intent: PresentationRegisterIntentPasswordChanged(value: password))
                try await store.dispatch(intent: PresentationRegisterIntentDeviceNameChanged(value: deviceName))
                try await store.dispatch(intent: PresentationRegisterIntentSubmit())
            } catch {
                print("register failed: \(error)")
            }
        }
    }

    private func confirm() {
        task?.cancel()
        task = Task {
            try await store.dispatch(intent: PresentationRegisterIntentConfirmationChanged(value: confirmation))
            try await store.dispatch(intent: PresentationRegisterIntentConfirmRecoveryWords())
        }
    }

    private func watchEffects() {
        task?.cancel()
        task = Task {
            for await effect in store.effects {
                if let ready = effect as? PresentationRegisterEffectRecoveryKeyReady {
                    recoveryWords = ready.words
                } else if effect is PresentationRegisterEffectRegistered {
                    forget()
                    store.close()
                    model.screen = "vault"
                }
            }
        }
    }

    private func forget() {
        recoveryWords = []
        confirmation = ""
        password = ""
    }
}

/// Restoring access with the recovery key and a new master password (US-03).
struct RecoveryView: View {
    @ObservedObject var model: AppModel
    @State private var email: String = ""
    @State private var words: String = ""
    @State private var password: String = ""
    @State private var deviceName: String = UIDevice.current.name
    @State private var task: Task<Void, Error>?

    private var store: PresentationRecoveryStore { model.recoveryStore }

    var body: some View {
        VStack(spacing: 12) {
            Text("Recover access").font(.title)
            TextField("Email", text: $email).textFieldStyle(.roundedBorder)
            TextField("Recovery key (12 words)", text: $words).textFieldStyle(.roundedBorder)
            SecureField("New master password", text: $password).textFieldStyle(.roundedBorder)
            if let s = store.state.value.strength ?? store.state.value.passwordStrength {
                Text("Strength: \(s.level)/4").font(.caption)
                if !s.warnings.isEmpty {
                    Text(s.warnings.joined(separator: ", ")).font(.caption).foregroundColor(.orange)
                }
            }
            TextField("Device name", text: $deviceName).textFieldStyle(.roundedBorder)
            if store.state.value.error != nil {
                Text("Recovery failed").foregroundColor(.red)
            }
            if store.state.value.isSubmitting {
                ProgressView()
            } else {
                Button("Restore access") { submit() }
            }
        }.padding()
        .onAppear { watchEffects() }
        .onDisappear {
            words = ""
            password = ""
            store.close()
            task?.cancel()
        }
    }

    private func submit() {
        task?.cancel()
        task = Task {
            try await store.dispatch(intent: PresentationRecoveryIntentEmailChanged(value: email))
            try await store.dispatch(intent: PresentationRecoveryIntentRecoveryWordsChanged(
                words: words.split(separator: " ").map { String($0) }.filter { !$0.isEmpty }
            ))
            try await store.dispatch(intent: PresentationRecoveryIntentNewPasswordChanged(value: password))
            try await store.dispatch(intent: PresentationRecoveryIntentDeviceNameChanged(value: deviceName))
            try await store.dispatch(intent: PresentationRecoveryIntentSubmit())
        }
    }

    private func watchEffects() {
        task?.cancel()
        task = Task {
            for await _ in store.effects {
                words = ""
                password = ""
                model.screen = "vault"
            }
        }
    }
}

/// The devices of the account (US-10).
struct DevicesView: View {
    @ObservedObject var model: AppModel
    @State private var revoking: String?

    private var store: PresentationDevicesStore { model.devicesStore }

    var body: some View {
        List {
            if store.state.value.isLoading {
                ProgressView()
            }
            ForEach(store.state.value.devices, id: \.id) { device in
                HStack {
                    VStack(alignment: .leading) {
                        Text(device.name)
                        Text(device.platform).font(.caption)
                        if device.current {
                            Text("This device").font(.caption)
                        }
                    }
                    Spacer()
                    if !device.current {
                        Button("Revoke") { revoking = device.id }
                    }
                }
            }
            if let error = store.state.value.error {
                // The access token lives 15 minutes; a refresh is a later wave, so an
                // expired session surfaces as invalid credentials here.
                Text("Could not load the devices. \(describe(error))")
                    .foregroundColor(.red)
            }
        }
        .navigationTitle("Devices")
        .onAppear {
            Task {
                do {
                    try await store.dispatch(intent: PresentationDevicesIntentLoad())
                } catch {
                    print("devices load failed: \(error)")
                }
            }
        }
        .alert("Revoke this device?", isPresented: Binding(
            get: { revoking != nil },
            set: { if !$0 { revoking = nil } },
        )) {
            Button("Revoke", role: .destructive) {
                if let id = revoking {
                    Task { try await store.dispatch(intent: PresentationDevicesIntentRevoke(deviceId: id)) }
                }
                revoking = nil
            }
            Button("Cancel", role: .cancel) { revoking = nil }
        } message: {
            Text("Its tokens stop working right away.")
        }
    }
}

/// Auto-lock timeout (US-08), biometric unlock (US-07) and account deletion (US-11).
struct SettingsView: View {
    @ObservedObject var model: AppModel
    @State private var deletionPassword: String = ""
    @State private var showDeleteForm = false
    @State private var task: Task<Void, Error>?

    private var store: PresentationSettingsStore { model.settingsStore }
    private var state: PresentationSettingsState { store.state.value }

    var body: some View {
        Form {
            Section("Auto-lock") {
                ForEach(Array(DomainAutoLockPolicy.companion.ALL.enumerated()), id: \.offset) { _, policy in
                    Button {
                        Task { try await store.dispatch(intent: PresentationSettingsIntentPolicyChanged(policy: policy)) }
                    } label: {
                        HStack {
                            Text(label(for: policy))
                            Spacer()
                            if state.policy == policy {
                                Image(systemName: "checkmark")
                            }
                        }
                    }
                }
            }
            Section("Biometric unlock") {
                Toggle("Unlock with biometrics", isOn: Binding(
                    get: { state.biometricsEnabled },
                    set: { enabled in
                        Task { try await store.dispatch(intent: PresentationSettingsIntentBiometricsToggled(enabled: enabled)) }
                    },
                ))
                .disabled(!state.biometricsAvailable)
                if !state.biometricsAvailable {
                    Text("Not available on this device").font(.caption)
                }
            }
            Section("Delete account") {
                Text("The account and every record are erased. This cannot be undone.")
                if showDeleteForm {
                    SecureField("Master password", text: $deletionPassword)
                        .textFieldStyle(.roundedBorder)
                    if state.error != nil {
                        Text("Something went wrong").foregroundColor(.red)
                    }
                    Button("Delete forever", role: .destructive) { deleteAccount() }
                        .disabled(deletionPassword.isEmpty)
                } else {
                    Button("Delete account", role: .destructive) { showDeleteForm = true }
                }
            }
        }
        .onAppear {
            Task { try await store.dispatch(intent: PresentationSettingsIntentLoad()) }
            watchEffects()
        }
        .onDisappear {
            task?.cancel()
            forget()
        }
    }

    private func deleteAccount() {
        let password = deletionPassword
        forget()
        Task {
            try await store.dispatch(intent: PresentationSettingsIntentDeletionPasswordChanged(value: password))
            try await store.dispatch(intent: PresentationSettingsIntentDeleteAccount())
        }
    }

    private func forget() {
        deletionPassword = ""
        showDeleteForm = false
    }

    private func watchEffects() {
        task?.cancel()
        task = Task {
            for await effect in store.effects {
                if effect is PresentationSettingsEffectAccountDeleted {
                    forget()
                    model.screen = "login"
                }
            }
        }
    }

    private func label(for policy: DomainAutoLockPolicy) -> String {        switch policy.timeoutMillis {
        case DomainAutoLockPolicy.companion.IMMEDIATE_MILLIS: return "Immediately"
        case DomainAutoLockPolicy.companion.ONE_MINUTE_MILLIS: return "After 1 minute"
        case DomainAutoLockPolicy.companion.FIVE_MINUTES_MILLIS: return "After 5 minutes"
        case DomainAutoLockPolicy.companion.FIFTEEN_MINUTES_MILLIS: return "After 15 minutes"
        default: return "After \(policy.timeoutMillis / 1000) seconds"
        }
    }
}

/// A Kotlin error as text the user can read: the SDK prints `AppErrorInvalidCredentials`.
private func describe(_ error: Any) -> String {
    String(describing: error).replacingOccurrences(of: "AppError", with: "")
}
