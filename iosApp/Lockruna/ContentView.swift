import SwiftUI
import Shared

struct ContentView: View {
    var body: some View {
        VStack(spacing: 12) {
            Text("PassKeeper")
                .font(.largeTitle)
            Text("Shared core \(SharedCore.shared.VERSION)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}
