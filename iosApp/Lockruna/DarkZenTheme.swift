import SwiftUI

// MARK: - Dark Zen Design Tokens

public enum DarkZen {
    public static let background = Color(red: 0.06, green: 0.08, blue: 0.14)
    public static let accent = Color(red: 0.859, green: 0.204, blue: 0.949)
    public static let textPrimary = Color.white
    public static let textSecondary = Color.white.opacity(0.60)
    public static let card = Color.white.opacity(0.06)
    public static let panel = Color.white.opacity(0.05)
    public static let toolbar = Color.white.opacity(0.10)
    public static let divider = Color.white.opacity(0.10)
    public static let danger = Color.red.opacity(0.80)
    public static let warning = Color.orange.opacity(0.75)
    public static let fieldRadius: CGFloat = 8
    public static let cardRadius: CGFloat = 12
    public static let panelRadius: CGFloat = 16
}

// MARK: - Dark Zen Background with Starfield

public struct DarkZenBackground: View {
    @State private var stars: [UnitPoint] = []

    public init() {}

    public var body: some View {
        ZStack {
            Color(red: 0.06, green: 0.08, blue: 0.14)
                .ignoresSafeArea()

            Canvas { context, size in
                ForEach(stars, id: \.self) { point in
                    Circle()
                        .frame(width: 2, height: 2)
                        .foregroundColor(.white.opacity(0.08))
                        .position(CGPoint(x: point.x * size.width, y: point.y * size.height))
                }
            }
            .onAppear {
                stars = (0..<40).map { _ in UnitPoint(x: CGFloat.random(in: 0...1), y: CGFloat.random(in: 0...1)) }
            }
            .accessibilityHidden(true)
            .allowsHitTesting(false)
        }
    }
}

// MARK: - Dark Zen TextField Style

public struct DarkZenTextFieldStyle: TextFieldStyle {
    public func _body(configuration: TextField<_Label>) -> some View {
        configuration
            .padding(10)
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: DarkZen.fieldRadius))
            .overlay(
                RoundedRectangle(cornerRadius: DarkZen.fieldRadius)
                    .stroke(DarkZen.divider, lineWidth: 2)
                    .foregroundColor(.white.opacity(0.10))
                    .padding(.init(top: 1, leading: 1, bottom: 1, trailing: 1))
            )
    }
}

// MARK: - Dark Zen Primary Button Style

public struct DarkZenPrimaryButtonStyle: ButtonStyle {
    let cornerRadius: CGFloat

    public init(cornerRadius: CGFloat = DarkZen.cardRadius) {
        self.cornerRadius = cornerRadius
    }

    public func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.headline)
            .foregroundColor(DarkZen.textPrimary)
            .frame(maxWidth: .infinity, minHeight: 50)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(DarkZen.accent)
            )
            .opacity(configuration.isPressed ? 0.85 : 1.0)
    }
}

// MARK: - Preview

#Preview {
    DarkZenBackground()
        .previewLayout(.sizeThatFits)
}