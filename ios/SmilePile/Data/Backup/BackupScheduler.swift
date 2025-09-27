import Foundation
import BackgroundTasks
import os.log
import Combine

// MARK: - Backup Scheduler
@MainActor
final class BackupScheduler: ObservableObject {

    // MARK: - Singleton
    static let shared = BackupScheduler()

    // MARK: - Constants
    static let backgroundTaskIdentifier = "com.smilepile.backup.automatic"
    static let refreshTaskIdentifier = "com.smilepile.backup.refresh"

    // MARK: - Published Properties
    @Published var isScheduleEnabled = false
    @Published var scheduleFrequency: BackupScheduleFrequency = .weekly
    @Published var nextScheduledBackup: Date?
    @Published var lastAutomaticBackup: Date?
    @Published var requiresWiFi = true
    @Published var requiresCharging = false

    // MARK: - Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "BackupScheduler")
    private let backupManager: BackupManager
    private var cancellables = Set<AnyCancellable>()
    private let defaults = UserDefaults.standard

    // MARK: - Initialization
    private init() {
        self.backupManager = BackupManager.shared

        // Load saved schedule
        loadScheduleSettings()

        // Register background tasks
        registerBackgroundTasks()

        // Setup observers
        setupObservers()
    }

    // MARK: - Public Methods

    func enableSchedule(frequency: BackupScheduleFrequency) {
        logger.info("Enabling backup schedule with frequency: \(frequency.rawValue)")

        isScheduleEnabled = true
        scheduleFrequency = frequency

        // Calculate next backup time
        if let interval = frequency.interval {
            nextScheduledBackup = Date().addingTimeInterval(interval)
        }

        // Save settings
        saveScheduleSettings()

        // Schedule background task
        scheduleBackgroundTask()
    }

    func disableSchedule() {
        logger.info("Disabling backup schedule")

        isScheduleEnabled = false
        nextScheduledBackup = nil

        // Save settings
        saveScheduleSettings()

        // Cancel scheduled tasks
        cancelScheduledTasks()
    }

    func performImmediateBackup() async {
        logger.info("Performing immediate backup")

        do {
            let options = BackupOptions(
                includePhotos: true,
                includeSettings: true,
                includeCategories: true,
                compressionLevel: .normal,
                dateRange: nil,
                categoryFilter: nil
            )

            let backupFile = try await backupManager.exportToZip(options: options)

            lastAutomaticBackup = Date()
            saveScheduleSettings()

            // Upload to iCloud if enabled
            if UserDefaults.standard.bool(forKey: "enableiCloudBackup") {
                await uploadToiCloud(backupFile)
            }

            // Schedule next backup
            if isScheduleEnabled {
                scheduleNextBackup()
            }

            logger.info("Automatic backup completed successfully")

        } catch {
            logger.error("Automatic backup failed: \(error.localizedDescription)")
        }
    }

    // MARK: - Background Tasks

    private func registerBackgroundTasks() {
        // Register automatic backup task
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.backgroundTaskIdentifier,
            using: nil
        ) { task in
            self.handleBackgroundTask(task)
        }

        // Register refresh task
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: Self.refreshTaskIdentifier,
            using: nil
        ) { task in
            self.handleRefreshTask(task)
        }
    }

    private func handleBackgroundTask(_ task: BGTask) {
        logger.info("Handling background backup task")

        // Schedule next backup
        scheduleBackgroundTask()

        // Create operation
        let operation = BackupOperation(backupManager: backupManager) {
            task.setTaskCompleted(success: true)
        }

        // Set expiration handler
        task.expirationHandler = {
            operation.cancel()
            self.logger.warning("Background task expired")
        }

        // Start backup
        operation.start()
    }

    private func handleRefreshTask(_ task: BGTask) {
        logger.info("Handling refresh task")

        // Update schedule if needed
        if isScheduleEnabled {
            scheduleBackgroundTask()
        }

        task.setTaskCompleted(success: true)
    }

    private func scheduleBackgroundTask() {
        guard isScheduleEnabled,
              let interval = scheduleFrequency.interval else { return }

        let request = BGProcessingTaskRequest(identifier: Self.backgroundTaskIdentifier)
        request.requiresNetworkConnectivity = requiresWiFi
        request.requiresExternalPower = requiresCharging
        request.earliestBeginDate = Date().addingTimeInterval(interval)

        do {
            try BGTaskScheduler.shared.submit(request)
            logger.info("Scheduled background backup task for \(request.earliestBeginDate?.description ?? "unknown")")
        } catch {
            logger.error("Failed to schedule background task: \(error.localizedDescription)")
        }
    }

    private func cancelScheduledTasks() {
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.backgroundTaskIdentifier)
        BGTaskScheduler.shared.cancel(taskRequestWithIdentifier: Self.refreshTaskIdentifier)
        logger.info("Cancelled all scheduled backup tasks")
    }

    // MARK: - iCloud Backup

    private func uploadToiCloud(_ file: URL) async {
        logger.info("Uploading backup to iCloud: \(file.lastPathComponent)")

        do {
            // Get iCloud container
            guard let containerURL = FileManager.default.url(
                forUbiquityContainerIdentifier: nil
            ) else {
                logger.warning("iCloud container not available")
                return
            }

            // Create Backups directory in iCloud
            let iCloudBackupsDir = containerURL
                .appendingPathComponent("Documents", isDirectory: true)
                .appendingPathComponent("Backups", isDirectory: true)

            try FileManager.default.createDirectory(
                at: iCloudBackupsDir,
                withIntermediateDirectories: true,
                attributes: nil
            )

            // Copy file to iCloud
            let destinationURL = iCloudBackupsDir.appendingPathComponent(file.lastPathComponent)

            // Remove existing file if it exists
            try? FileManager.default.removeItem(at: destinationURL)

            // Copy new file
            try FileManager.default.copyItem(at: file, to: destinationURL)

            // Start downloading if needed
            var isDownloaded: AnyObject?
            try destinationURL.getResourceValue(&isDownloaded, forKey: .ubiquitousItemDownloadingStatusKey)

            logger.info("Successfully uploaded backup to iCloud")

            // Clean old iCloud backups
            await cleanOldiCloudBackups(in: iCloudBackupsDir)

        } catch {
            logger.error("Failed to upload to iCloud: \(error.localizedDescription)")
        }
    }

    private func cleanOldiCloudBackups(in directory: URL) async {
        do {
            let files = try FileManager.default.contentsOfDirectory(
                at: directory,
                includingPropertiesForKeys: [.creationDateKey],
                options: []
            )

            // Sort by creation date
            let sortedFiles = try files.sorted { url1, url2 in
                let date1 = try url1.resourceValues(forKeys: [.creationDateKey]).creationDate ?? Date.distantPast
                let date2 = try url2.resourceValues(forKeys: [.creationDateKey]).creationDate ?? Date.distantPast
                return date1 > date2
            }

            // Keep only last 5 backups
            let maxBackups = 5
            if sortedFiles.count > maxBackups {
                let filesToDelete = sortedFiles.dropFirst(maxBackups)
                for file in filesToDelete {
                    try FileManager.default.removeItem(at: file)
                    logger.info("Deleted old iCloud backup: \(file.lastPathComponent)")
                }
            }
        } catch {
            logger.error("Failed to clean old iCloud backups: \(error.localizedDescription)")
        }
    }

    // MARK: - Schedule Management

    private func scheduleNextBackup() {
        guard isScheduleEnabled,
              let interval = scheduleFrequency.interval else { return }

        nextScheduledBackup = Date().addingTimeInterval(interval)
        saveScheduleSettings()

        // Schedule background task
        scheduleBackgroundTask()
    }

    private func setupObservers() {
        // Observe schedule changes
        $isScheduleEnabled
            .combineLatest($scheduleFrequency)
            .sink { [weak self] enabled, frequency in
                if enabled {
                    self?.scheduleBackgroundTask()
                } else {
                    self?.cancelScheduledTasks()
                }
            }
            .store(in: &cancellables)

        // Observe network requirements
        $requiresWiFi
            .combineLatest($requiresCharging)
            .sink { [weak self] _, _ in
                self?.saveScheduleSettings()
                if self?.isScheduleEnabled == true {
                    self?.scheduleBackgroundTask()
                }
            }
            .store(in: &cancellables)
    }

    // MARK: - Persistence

    private func saveScheduleSettings() {
        defaults.set(isScheduleEnabled, forKey: "backupScheduleEnabled")
        defaults.set(scheduleFrequency.rawValue, forKey: "backupScheduleFrequency")
        defaults.set(nextScheduledBackup, forKey: "nextScheduledBackup")
        defaults.set(lastAutomaticBackup, forKey: "lastAutomaticBackup")
        defaults.set(requiresWiFi, forKey: "backupRequiresWiFi")
        defaults.set(requiresCharging, forKey: "backupRequiresCharging")
    }

    private func loadScheduleSettings() {
        isScheduleEnabled = defaults.bool(forKey: "backupScheduleEnabled")

        if let frequencyString = defaults.string(forKey: "backupScheduleFrequency"),
           let frequency = BackupScheduleFrequency(rawValue: frequencyString) {
            scheduleFrequency = frequency
        }

        nextScheduledBackup = defaults.object(forKey: "nextScheduledBackup") as? Date
        lastAutomaticBackup = defaults.object(forKey: "lastAutomaticBackup") as? Date
        requiresWiFi = defaults.bool(forKey: "backupRequiresWiFi")
        requiresCharging = defaults.bool(forKey: "backupRequiresCharging")
    }

    // MARK: - Backup History

    func getAutomaticBackupHistory() async -> [BackupHistoryEntry] {
        return await backupManager.backupHistory.filter { $0.automatic }
    }

    func getNextBackupDescription() -> String {
        guard isScheduleEnabled else {
            return "Automatic backups disabled"
        }

        guard let nextDate = nextScheduledBackup else {
            return "No backup scheduled"
        }

        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return "Next backup \(formatter.localizedString(for: nextDate, relativeTo: Date()))"
    }

    func getLastBackupDescription() -> String {
        guard let lastDate = lastAutomaticBackup else {
            return "No automatic backups yet"
        }

        let formatter = RelativeDateTimeFormatter()
        formatter.unitsStyle = .full
        return "Last backup \(formatter.localizedString(for: lastDate, relativeTo: Date()))"
    }
}

// MARK: - Backup Operation

private class BackupOperation: Operation {
    private let backupManager: BackupManager
    private let completion: () -> Void
    private let logger = Logger(subsystem: "com.smilepile", category: "BackupOperation")

    init(backupManager: BackupManager, completion: @escaping () -> Void) {
        self.backupManager = backupManager
        self.completion = completion
        super.init()
    }

    override func main() {
        guard !isCancelled else {
            completion()
            return
        }

        Task { @MainActor in
            do {
                let options = BackupOptions(
                    includePhotos: true,
                    includeSettings: true,
                    includeCategories: true,
                    compressionLevel: .normal,
                    dateRange: nil,
                    categoryFilter: nil
                )

                let _ = try await backupManager.exportToZip(options: options)
                logger.info("Background backup completed")

            } catch {
                logger.error("Background backup failed: \(error.localizedDescription)")
            }

            completion()
        }
    }
}

// MARK: - Scheduled Backup Monitor

class ScheduledBackupMonitor: ObservableObject {
    @Published var isMonitoring = false
    @Published var nextBackupCountdown: String = ""

    private var timer: Timer?
    private let scheduler = BackupScheduler.shared

    func startMonitoring() {
        isMonitoring = true
        timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { _ in
            self.updateCountdown()
        }
    }

    func stopMonitoring() {
        isMonitoring = false
        timer?.invalidate()
        timer = nil
    }

    private func updateCountdown() {
        guard let nextBackup = scheduler.nextScheduledBackup else {
            nextBackupCountdown = "No backup scheduled"
            return
        }

        let timeInterval = nextBackup.timeIntervalSinceNow

        if timeInterval <= 0 {
            nextBackupCountdown = "Backup starting..."
        } else {
            let hours = Int(timeInterval) / 3600
            let minutes = (Int(timeInterval) % 3600) / 60
            let seconds = Int(timeInterval) % 60

            if hours > 0 {
                nextBackupCountdown = String(format: "%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                nextBackupCountdown = String(format: "%02d:%02d", minutes, seconds)
            }
        }
    }
}

// MARK: - Network Monitor for WiFi-only Backups

import Network

class BackupNetworkMonitor {
    private let monitor = NWPathMonitor()
    private let queue = DispatchQueue(label: "BackupNetworkMonitor")
    private(set) var isWiFiAvailable = false
    var onWiFiStatusChange: ((Bool) -> Void)?

    init() {
        monitor.pathUpdateHandler = { [weak self] path in
            let isWiFi = path.usesInterfaceType(.wifi)
            self?.isWiFiAvailable = isWiFi
            DispatchQueue.main.async {
                self?.onWiFiStatusChange?(isWiFi)
            }
        }
        monitor.start(queue: queue)
    }

    deinit {
        monitor.cancel()
    }
}