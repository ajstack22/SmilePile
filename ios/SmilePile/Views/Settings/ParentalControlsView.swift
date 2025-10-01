import SwiftUI

// DEPRECATED: This view is no longer used in iOS.
// iOS over-implemented parental controls with features not in Android spec (content filtering, time restrictions, schedules, etc).
// Android's parental controls are simpler: just Kid-Safe Mode and Delete Protection toggles in ParentalSettingsScreen.
// This file is kept for reference only.

struct ParentalControlsView: View {
    @State private var kidsModeEnabled = false
    @State private var contentFilteringEnabled = false
    @State private var timeRestrictionsEnabled = false
    @State private var dailyTimeLimit: Double = 60  // minutes
    @State private var selectedCategories: Set<String> = []
    @State private var showAuthenticationRequired = false

    @AppStorage("kids_mode_enabled") private var savedKidsModeEnabled = false
    @AppStorage("content_filtering_enabled") private var savedContentFilteringEnabled = false
    @AppStorage("time_restrictions_enabled") private var savedTimeRestrictionsEnabled = false
    @AppStorage("daily_time_limit") private var savedDailyTimeLimit: Double = 60

    var body: some View {
        List {
            // Kids Mode Section
            Section(header: Text("Kids Mode")) {
                Toggle(isOn: $kidsModeEnabled) {
                    HStack {
                        Image(systemName: "face.smiling")
                            .foregroundColor(.orange)
                        VStack(alignment: .leading) {
                            Text("Enable Kids Mode")
                            Text("Simplified interface for children")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .onChange(of: kidsModeEnabled) { newValue in
                    savedKidsModeEnabled = newValue
                }

                if kidsModeEnabled {
                    NavigationLink(destination: KidsModeSettingsView()) {
                        HStack {
                            Image(systemName: "gear")
                            Text("Kids Mode Settings")
                        }
                    }
                }
            }

            // Content Filtering Section
            Section(header: Text("Content Filtering")) {
                Toggle(isOn: $contentFilteringEnabled) {
                    HStack {
                        Image(systemName: "eye.slash")
                            .foregroundColor(.blue)
                        VStack(alignment: .leading) {
                            Text("Filter Content")
                            Text("Hide inappropriate content")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .onChange(of: contentFilteringEnabled) { newValue in
                    savedContentFilteringEnabled = newValue
                }

                if contentFilteringEnabled {
                    NavigationLink(destination: ParentalCategorySelectionView(selectedCategories: $selectedCategories)) {
                        HStack {
                            Text("Allowed Categories")
                            Spacer()
                            Text("\(selectedCategories.count) selected")
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }

            // Time Restrictions Section
            Section(header: Text("Time Restrictions")) {
                Toggle(isOn: $timeRestrictionsEnabled) {
                    HStack {
                        Image(systemName: "clock")
                            .foregroundColor(.green)
                        VStack(alignment: .leading) {
                            Text("Set Time Limits")
                            Text("Control app usage time")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .onChange(of: timeRestrictionsEnabled) { newValue in
                    savedTimeRestrictionsEnabled = newValue
                }

                if timeRestrictionsEnabled {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Daily Time Limit")
                            .font(.subheadline)

                        HStack {
                            Slider(value: $dailyTimeLimit, in: 15...180, step: 15)
                            Text("\(Int(dailyTimeLimit)) min")
                                .frame(width: 60)
                                .foregroundColor(.secondary)
                        }
                    }
                    .onChange(of: dailyTimeLimit) { newValue in
                        savedDailyTimeLimit = newValue
                    }

                    NavigationLink(destination: TimeScheduleView()) {
                        HStack {
                            Image(systemName: "calendar")
                            Text("Set Schedule")
                        }
                    }
                }
            }

            // Quick Actions Section
            Section(header: Text("Quick Actions")) {
                Button(action: lockNow) {
                    HStack {
                        Image(systemName: "lock.fill")
                            .foregroundColor(.red)
                        Text("Lock App Now")
                    }
                }

                Button(action: resetSettings) {
                    HStack {
                        Image(systemName: "arrow.counterclockwise")
                            .foregroundColor(.orange)
                        Text("Reset All Settings")
                    }
                }
            }

            // Information Section
            Section(header: Text("Information")) {
                HStack {
                    Image(systemName: "info.circle")
                        .foregroundColor(.blue)
                    Text("Parental controls help you manage your child's app experience")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }

                NavigationLink(destination: ParentalControlsHelpView()) {
                    HStack {
                        Image(systemName: "questionmark.circle")
                        Text("Help & Guidelines")
                    }
                }
            }
        }
        .navigationTitle("Parental Controls")
        .navigationBarTitleDisplayMode(.large)
        .onAppear {
            loadSettings()
        }
    }

    private func loadSettings() {
        kidsModeEnabled = savedKidsModeEnabled
        contentFilteringEnabled = savedContentFilteringEnabled
        timeRestrictionsEnabled = savedTimeRestrictionsEnabled
        dailyTimeLimit = savedDailyTimeLimit
    }

    private func lockNow() {
        // Implement immediate lock functionality
        showAuthenticationRequired = true
    }

    private func resetSettings() {
        kidsModeEnabled = false
        contentFilteringEnabled = false
        timeRestrictionsEnabled = false
        dailyTimeLimit = 60
        selectedCategories.removeAll()

        savedKidsModeEnabled = false
        savedContentFilteringEnabled = false
        savedTimeRestrictionsEnabled = false
        savedDailyTimeLimit = 60
    }
}

// Supporting Views

struct KidsModeSettingsView: View {
    @State private var showSmileFaces = true
    @State private var simplifiedUI = true
    @State private var autoExitAfterInactivity = false

    var body: some View {
        List {
            Toggle("Show Smile Faces", isOn: $showSmileFaces)
            Toggle("Simplified UI", isOn: $simplifiedUI)
            Toggle("Auto-exit after inactivity", isOn: $autoExitAfterInactivity)
        }
        .navigationTitle("Kids Mode Settings")
    }
}

private struct ParentalCategorySelectionView: View {
    @Binding var selectedCategories: Set<String>
    let allCategories = ["Family", "Nature", "Animals", "Fun", "Educational", "Art", "Sports"]

    var body: some View {
        List {
            ForEach(allCategories, id: \.self) { category in
                HStack {
                    Text(category)
                    Spacer()
                    if selectedCategories.contains(category) {
                        Image(systemName: "checkmark.circle.fill")
                            .foregroundColor(.blue)
                    }
                }
                .contentShape(Rectangle())
                .onTapGesture {
                    if selectedCategories.contains(category) {
                        selectedCategories.remove(category)
                    } else {
                        selectedCategories.insert(category)
                    }
                }
            }
        }
        .navigationTitle("Allowed Categories")
    }
}

struct TimeScheduleView: View {
    @State private var weekdayStart = Date()
    @State private var weekdayEnd = Date()
    @State private var weekendStart = Date()
    @State private var weekendEnd = Date()

    var body: some View {
        Form {
            Section(header: Text("Weekdays")) {
                DatePicker("Start Time", selection: $weekdayStart, displayedComponents: .hourAndMinute)
                DatePicker("End Time", selection: $weekdayEnd, displayedComponents: .hourAndMinute)
            }

            Section(header: Text("Weekends")) {
                DatePicker("Start Time", selection: $weekendStart, displayedComponents: .hourAndMinute)
                DatePicker("End Time", selection: $weekendEnd, displayedComponents: .hourAndMinute)
            }
        }
        .navigationTitle("Time Schedule")
    }
}

struct ParentalControlsHelpView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 20) {
                Group {
                    Text("Parental Controls Guide")
                        .font(.title2)
                        .fontWeight(.bold)

                    Text("Kids Mode")
                        .font(.headline)
                    Text("Enable a simplified interface designed for children with larger buttons and friendly animations.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    Text("Content Filtering")
                        .font(.headline)
                    Text("Control which photo categories are visible to your child. Only selected categories will appear in Kids Mode.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)

                    Text("Time Restrictions")
                        .font(.headline)
                    Text("Set daily time limits and schedules for when the app can be used. The app will lock automatically when limits are reached.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }

                Group {
                    Text("Security Tips")
                        .font(.headline)
                        .padding(.top)

                    VStack(alignment: .leading, spacing: 10) {
                        Label("Always set a strong PIN or pattern", systemImage: "1.circle.fill")
                        Label("Enable biometric authentication for quick access", systemImage: "2.circle.fill")
                        Label("Regularly review your parental control settings", systemImage: "3.circle.fill")
                        Label("Test Kids Mode before giving device to children", systemImage: "4.circle.fill")
                    }
                    .font(.subheadline)
                }
            }
            .padding()
        }
        .navigationTitle("Help & Guidelines")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct ParentalControlsView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            ParentalControlsView()
        }
    }
}