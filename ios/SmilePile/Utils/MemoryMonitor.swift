import Foundation
import UIKit
import SwiftUI
import os.log

/// Centralized memory monitoring and management
@MainActor
class MemoryMonitor: ObservableObject {
    static let shared = MemoryMonitor()

    // MARK: - Published Properties
    @Published var currentMemoryUsageMB: Int = 0
    @Published var memoryPressureLevel: MemoryPressureLevel = .normal
    @Published var isUnderMemoryPressure: Bool = false

    // MARK: - Configuration
    struct Thresholds {
        static let warningMB: Int = 80
        static let criticalMB: Int = 100
        static let dangerMB: Int = 150
    }

    // MARK: - Memory Pressure Levels
    enum MemoryPressureLevel {
        case normal     // < 80MB
        case warning    // 80-100MB
        case critical   // 100-150MB
        case danger     // > 150MB

        var color: UIColor {
            switch self {
            case .normal:
                return .systemGreen
            case .warning:
                return .systemYellow
            case .critical:
                return .systemOrange
            case .danger:
                return .systemRed
            }
        }

        var description: String {
            switch self {
            case .normal:
                return "Normal"
            case .warning:
                return "Warning"
            case .critical:
                return "Critical"
            case .danger:
                return "Danger"
            }
        }

        var shouldReduceQuality: Bool {
            switch self {
            case .normal, .warning:
                return false
            case .critical, .danger:
                return true
            }
        }
    }

    // MARK: - Private Properties
    private let logger = Logger(subsystem: "com.smilepile", category: "MemoryMonitor")
    private var timer: Timer?
    private var memoryWarningObserver: NSObjectProtocol?
    private var callbacks: [(MemoryPressureLevel) -> Void] = []

    // Statistics
    private var peakMemoryUsage: Int = 0
    private var memoryWarningCount: Int = 0
    private var lastMemoryWarning: Date?

    // MARK: - Initialization
    private init() {
        startMonitoring()
        observeMemoryWarnings()
    }

    deinit {
        Task { @MainActor in
            stopMonitoring()
        }
    }

    // MARK: - Public Methods

    /// Start monitoring memory usage
    func startMonitoring(interval: TimeInterval = 1.0) {
        stopMonitoring()

        timer = Timer.scheduledTimer(withTimeInterval: interval, repeats: true) { [weak self] _ in
            Task { @MainActor in
                self?.updateMemoryUsage()
            }
        }

        // Initial update
        updateMemoryUsage()

        logger.info("Memory monitoring started")
    }

    /// Stop monitoring memory usage
    func stopMonitoring() {
        timer?.invalidate()
        timer = nil
        logger.info("Memory monitoring stopped")
    }

    /// Register callback for memory pressure changes
    func onMemoryPressureChange(_ callback: @escaping (MemoryPressureLevel) -> Void) {
        callbacks.append(callback)
    }

    /// Get current memory info
    func getCurrentMemoryInfo() -> MemoryInfo {
        return MemoryInfo(
            currentMB: currentMemoryUsageMB,
            peakMB: peakMemoryUsage,
            pressureLevel: memoryPressureLevel,
            warningCount: memoryWarningCount,
            lastWarning: lastMemoryWarning
        )
    }

    /// Force memory cleanup
    func requestMemoryCleanup() {
        logger.info("Memory cleanup requested")

        // Post notification for components to clean up
        NotificationCenter.default.post(
            name: .memoryCleanupRequested,
            object: nil
        )

        // Force a collection cycle
        autoreleasepool {
            // This helps release autoreleased objects
        }
    }

    /// Check if safe to perform memory-intensive operation
    func isSafeToPerformOperation(estimatedMB: Int) -> Bool {
        let projectedUsage = currentMemoryUsageMB + estimatedMB
        return projectedUsage < Thresholds.warningMB
    }

    // MARK: - Private Methods

    private func updateMemoryUsage() {
        let usage = getCurrentMemoryUsage()
        currentMemoryUsageMB = usage

        // Update peak
        if usage > peakMemoryUsage {
            peakMemoryUsage = usage
        }

        // Determine pressure level
        let oldLevel = memoryPressureLevel
        memoryPressureLevel = determinePressureLevel(usage: usage)
        isUnderMemoryPressure = memoryPressureLevel != .normal

        // Notify if level changed
        if oldLevel != memoryPressureLevel {
            logger.info("Memory pressure changed: \(oldLevel.description) -> \(self.memoryPressureLevel.description)")
            notifyPressureChange()
        }

        // Log if concerning
        if memoryPressureLevel == .critical || memoryPressureLevel == .danger {
            logger.warning("High memory usage: \(usage)MB (\(self.memoryPressureLevel.description))")
        }
    }

    private func getCurrentMemoryUsage() -> Int {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size) / 4

        let result = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(
                    mach_task_self_,
                    task_flavor_t(MACH_TASK_BASIC_INFO),
                    $0,
                    &count
                )
            }
        }

        if result == KERN_SUCCESS {
            return Int(info.resident_size / (1024 * 1024))
        }

        return 0
    }

    private func determinePressureLevel(usage: Int) -> MemoryPressureLevel {
        if usage < Thresholds.warningMB {
            return .normal
        } else if usage < Thresholds.criticalMB {
            return .warning
        } else if usage < Thresholds.dangerMB {
            return .critical
        } else {
            return .danger
        }
    }

    private func observeMemoryWarnings() {
        memoryWarningObserver = NotificationCenter.default.addObserver(
            forName: UIApplication.didReceiveMemoryWarningNotification,
            object: nil,
            queue: .main
        ) { [weak self] _ in
            self?.handleMemoryWarning()
        }
    }

    private func handleMemoryWarning() {
        memoryWarningCount += 1
        lastMemoryWarning = Date()

        logger.critical("Memory warning received! (Count: \(self.memoryWarningCount))")

        // Force to danger level
        memoryPressureLevel = .danger
        isUnderMemoryPressure = true

        // Request immediate cleanup
        requestMemoryCleanup()

        // Notify callbacks
        notifyPressureChange()
    }

    private func notifyPressureChange() {
        for callback in callbacks {
            callback(memoryPressureLevel)
        }
    }
}

// MARK: - Memory Info
struct MemoryInfo {
    let currentMB: Int
    let peakMB: Int
    let pressureLevel: MemoryMonitor.MemoryPressureLevel
    let warningCount: Int
    let lastWarning: Date?

    var description: String {
        """
        Current: \(currentMB)MB
        Peak: \(peakMB)MB
        Level: \(pressureLevel.description)
        Warnings: \(warningCount)
        """
    }

    var isHealthy: Bool {
        return pressureLevel == .normal && warningCount == 0
    }
}

// MARK: - Notification Names
extension Notification.Name {
    static let memoryCleanupRequested = Notification.Name("com.smilepile.memoryCleanupRequested")
    static let memoryPressureChanged = Notification.Name("com.smilepile.memoryPressureChanged")
}

// MARK: - Memory Pressure Handler Protocol
protocol MemoryPressureHandler: AnyObject {
    func handleMemoryPressure(level: MemoryMonitor.MemoryPressureLevel)
}

// MARK: - SwiftUI View Modifier
struct MemoryMonitorViewModifier: ViewModifier {
    @StateObject private var monitor = MemoryMonitor.shared
    let showDebugInfo: Bool

    func body(content: Content) -> some View {
        content
            .overlay(alignment: .topTrailing) {
                if showDebugInfo {
                    MemoryDebugView(monitor: monitor)
                        .padding()
                }
            }
    }
}

// MARK: - Memory Debug View
struct MemoryDebugView: View {
    @ObservedObject var monitor: MemoryMonitor

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Label("\(monitor.currentMemoryUsageMB)MB", systemImage: "memorychip")
                .font(.caption.bold())
                .foregroundColor(Color(monitor.memoryPressureLevel.color))

            Text(monitor.memoryPressureLevel.description)
                .font(.caption2)
                .foregroundColor(.secondary)
        }
        .padding(8)
        .background(
            RoundedRectangle(cornerRadius: 8)
                .fill(Color(UIColor.secondarySystemBackground))
                .shadow(radius: 2)
        )
    }
}

// MARK: - View Extension
extension View {
    func memoryMonitoring(showDebugInfo: Bool = false) -> some View {
        modifier(MemoryMonitorViewModifier(showDebugInfo: showDebugInfo))
    }
}