import SwiftUI

// MARK: - Settings Section Container
struct SettingsSection<Content: View>: View {
    let title: String
    let titleColor: Color
    @ViewBuilder let content: Content

    init(
        title: String,
        titleColor: Color = .primary,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.titleColor = titleColor
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.headline)
                .foregroundColor(titleColor)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)

            VStack(spacing: 0) {
                content
            }
            .background(Color(UIColor.secondarySystemBackground))
            .cornerRadius(12)
            .padding(.horizontal, 16)
        }
    }
}

// MARK: - Settings Action Item (Outlined Button)
struct SettingsActionItem: View {
    let title: String
    let subtitle: String
    let icon: String
    let iconColor: Color
    let action: () -> Void

    init(
        title: String,
        subtitle: String,
        icon: String,
        iconColor: Color = .accentColor,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.iconColor = iconColor
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(iconColor)
                    .frame(width: 24)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.primary)

                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer()
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(iconColor.opacity(0.5), lineWidth: 1)
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

// MARK: - Settings Switch Item (Toggle)
struct SettingsSwitchItem: View {
    let title: String
    let subtitle: String
    let icon: String
    let iconColor: Color
    @Binding var isOn: Bool
    let isEnabled: Bool

    init(
        title: String,
        subtitle: String,
        icon: String,
        iconColor: Color = .accentColor,
        isOn: Binding<Bool>,
        isEnabled: Bool = true
    ) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.iconColor = iconColor
        self._isOn = isOn
        self.isEnabled = isEnabled
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(iconColor.opacity(isEnabled ? 1 : 0.5))
                    .frame(width: 24)

                VStack(alignment: .leading, spacing: 4) {
                    Text(title)
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(.primary.opacity(isEnabled ? 1 : 0.5))

                    Text(subtitle)
                        .font(.system(size: 12))
                        .foregroundColor(.secondary.opacity(isEnabled ? 1 : 0.5))
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer()

                Toggle("", isOn: $isOn)
                    .labelsHidden()
                    .tint(iconColor)
                    .disabled(!isEnabled)
            }
            .padding(16)
            .frame(maxWidth: .infinity)
            .background(
                RoundedRectangle(cornerRadius: 8)
                    .fill(isEnabled ? Color(UIColor.systemBackground) : Color(UIColor.systemBackground).opacity(0.6))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 8)
                    .stroke(iconColor.opacity(0.5), lineWidth: 1)
            )
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}