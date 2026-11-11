import SwiftUI
import Shared

struct ContentView: View {
    @ObservedObject var model: AppModel

    var body: some View {
        VStack(spacing: 12) {
            Text("PassKeeper")
                .font(.largeTitle)
            Text(model.status)
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
    }
}
