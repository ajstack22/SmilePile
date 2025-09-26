import Foundation
import CoreData
import os.log

/// Manages resumable photo import sessions with persistence
/// Prevents data loss on app termination or crashes
final class PhotoImportSession: NSManagedObject {

    // MARK: - Core Data Properties
    @NSManaged var sessionId: String
    @NSManaged var startedAt: Date
    @NSManaged var lastUpdatedAt: Date
    @NSManaged var status: String
    @NSManaged var totalPhotos: Int32
    @NSManaged var processedPhotos: Int32
    @NSManaged var failedPhotos: Int32
    @NSManaged var pendingPhotoURLs: String? // JSON array of URLs
    @NSManaged var processedPhotoIds: String? // JSON array of photo IDs
    @NSManaged var failedPhotoURLs: String? // JSON array of failed URLs
    @NSManaged var categoryId: Int64
    @NSManaged var errorMessage: String?

    // MARK: - Computed Properties

    var sessionStatus: SessionStatus {
        get { SessionStatus(rawValue: status) ?? .pending }
        set { status = newValue.rawValue }
    }

    var progress: Double {
        guard totalPhotos > 0 else { return 0 }
        return Double(processedPhotos) / Double(totalPhotos)
    }

    var isResumable: Bool {
        return sessionStatus == .inProgress || sessionStatus == .paused
    }

    var pendingURLs: [URL] {
        guard let jsonString = pendingPhotoURLs,
              let data = jsonString.data(using: .utf8),
              let paths = try? JSONDecoder().decode([String].self, from: data) else {
            return []
        }
        return paths.compactMap { URL(string: $0) }
    }

    var processedIds: [String] {
        guard let jsonString = processedPhotoIds,
              let data = jsonString.data(using: .utf8),
              let ids = try? JSONDecoder().decode([String].self, from: data) else {
            return []
        }
        return ids
    }

    var failedURLs: [URL] {
        guard let jsonString = failedPhotoURLs,
              let data = jsonString.data(using: .utf8),
              let paths = try? JSONDecoder().decode([String].self, from: data) else {
            return []
        }
        return paths.compactMap { URL(string: $0) }
    }

    // MARK: - Session Status

    enum SessionStatus: String {
        case pending = "pending"
        case inProgress = "in_progress"
        case paused = "paused"
        case completed = "completed"
        case failed = "failed"
        case cancelled = "cancelled"
    }
}

// MARK: - Session Manager

/// Manages photo import sessions with persistence
final class PhotoImportSessionManager: ObservableObject {

    // MARK: - Properties

    private let logger = Logger(subsystem: "com.smilepile", category: "PhotoImportSession")
    private let coreDataStack: CoreDataStack
    @Published var activeSession: PhotoImportSession?
    @Published var recentSessions: [PhotoImportSession] = []

    // Session cleanup configuration
    private let maxSessionAge: TimeInterval = 7 * 24 * 60 * 60 // 7 days
    private let maxSessionCount: Int = 10

    // MARK: - Initialization

    init(coreDataStack: CoreDataStack = .shared) {
        self.coreDataStack = coreDataStack
        Task {
            await loadSessions()
            await cleanupOldSessions()
        }
    }

    // MARK: - Public Methods

    /// Create a new import session
    func createSession(
        photoURLs: [URL],
        categoryId: Int64
    ) async throws -> PhotoImportSession {
        let context = coreDataStack.newBackgroundContext()

        return try await context.perform {
            let session = PhotoImportSession(context: context)
            session.sessionId = UUID().uuidString
            session.startedAt = Date()
            session.lastUpdatedAt = Date()
            session.sessionStatus = .pending
            session.totalPhotos = Int32(photoURLs.count)
            session.processedPhotos = 0
            session.failedPhotos = 0
            session.categoryId = categoryId

            // Store URLs as JSON
            let urlStrings = photoURLs.map { $0.absoluteString }
            if let jsonData = try? JSONEncoder().encode(urlStrings) {
                session.pendingPhotoURLs = String(data: jsonData, encoding: .utf8)
            }

            session.processedPhotoIds = "[]"
            session.failedPhotoURLs = "[]"

            try context.save()

            self.logger.info("Created import session: \(session.sessionId) with \(photoURLs.count) photos")

            return session
        }
    }

    /// Resume an existing session
    func resumeSession(_ sessionId: String) async throws -> PhotoImportSession? {
        let context = coreDataStack.viewContext
        let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "sessionId == %@", sessionId)
        fetchRequest.fetchLimit = 1

        guard let session = try context.fetch(fetchRequest).first else {
            logger.error("Session not found: \(sessionId)")
            return nil
        }

        if !session.isResumable {
            logger.warning("Session \(sessionId) is not resumable (status: \(session.status))")
            return nil
        }

        session.sessionStatus = .inProgress
        session.lastUpdatedAt = Date()
        try context.save()

        logger.info("Resumed session: \(sessionId)")
        activeSession = session

        return session
    }

    /// Update session progress
    func updateProgress(
        sessionId: String,
        processedCount: Int,
        failedCount: Int,
        processedIds: [String],
        failedURLs: [URL]
    ) async throws {
        let context = coreDataStack.newBackgroundContext()

        try await context.perform {
            let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "sessionId == %@", sessionId)
            fetchRequest.fetchLimit = 1

            guard let session = try context.fetch(fetchRequest).first else {
                throw ImportError.sessionNotFound
            }

            session.processedPhotos = Int32(processedCount)
            session.failedPhotos = Int32(failedCount)
            session.lastUpdatedAt = Date()

            // Update processed IDs
            if let jsonData = try? JSONEncoder().encode(processedIds) {
                session.processedPhotoIds = String(data: jsonData, encoding: .utf8)
            }

            // Update failed URLs
            let failedUrlStrings = failedURLs.map { $0.absoluteString }
            if let jsonData = try? JSONEncoder().encode(failedUrlStrings) {
                session.failedPhotoURLs = String(data: jsonData, encoding: .utf8)
            }

            // Update pending URLs (remove processed ones)
            let remainingURLs = session.pendingURLs.filter { url in
                !processedIds.contains(url.absoluteString) && !failedURLs.contains(url)
            }
            let remainingUrlStrings = remainingURLs.map { $0.absoluteString }
            if let jsonData = try? JSONEncoder().encode(remainingUrlStrings) {
                session.pendingPhotoURLs = String(data: jsonData, encoding: .utf8)
            }

            try context.save()

            self.logger.debug("Updated session \(sessionId): \(processedCount)/\(session.totalPhotos) processed, \(failedCount) failed")
        }
    }

    /// Mark session as completed
    func completeSession(_ sessionId: String) async throws {
        let context = coreDataStack.newBackgroundContext()

        try await context.perform {
            let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "sessionId == %@", sessionId)
            fetchRequest.fetchLimit = 1

            guard let session = try context.fetch(fetchRequest).first else {
                throw ImportError.sessionNotFound
            }

            session.sessionStatus = .completed
            session.lastUpdatedAt = Date()
            try context.save()

            self.logger.info("Completed session \(sessionId): \(session.processedPhotos)/\(session.totalPhotos) imported")
        }

        activeSession = nil
        await loadSessions()
    }

    /// Pause session
    func pauseSession(_ sessionId: String) async throws {
        let context = coreDataStack.newBackgroundContext()

        try await context.perform {
            let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "sessionId == %@", sessionId)
            fetchRequest.fetchLimit = 1

            guard let session = try context.fetch(fetchRequest).first else {
                throw ImportError.sessionNotFound
            }

            session.sessionStatus = .paused
            session.lastUpdatedAt = Date()
            try context.save()

            self.logger.info("Paused session \(sessionId)")
        }

        activeSession = nil
    }

    /// Cancel session
    func cancelSession(_ sessionId: String) async throws {
        let context = coreDataStack.newBackgroundContext()

        try await context.perform {
            let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
            fetchRequest.predicate = NSPredicate(format: "sessionId == %@", sessionId)
            fetchRequest.fetchLimit = 1

            guard let session = try context.fetch(fetchRequest).first else {
                throw ImportError.sessionNotFound
            }

            session.sessionStatus = .cancelled
            session.lastUpdatedAt = Date()
            try context.save()

            self.logger.info("Cancelled session \(sessionId)")
        }

        activeSession = nil
        await loadSessions()
    }

    /// Get resumable sessions
    func getResumableSessions() async throws -> [PhotoImportSession] {
        let context = coreDataStack.viewContext
        let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
        fetchRequest.predicate = NSPredicate(format: "status == %@ OR status == %@",
                                            PhotoImportSession.SessionStatus.inProgress.rawValue,
                                            PhotoImportSession.SessionStatus.paused.rawValue)
        fetchRequest.sortDescriptors = [NSSortDescriptor(key: "lastUpdatedAt", ascending: false)]

        return try context.fetch(fetchRequest)
    }

    // MARK: - Private Methods

    private func loadSessions() async {
        do {
            let context = coreDataStack.viewContext
            let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
            fetchRequest.sortDescriptors = [NSSortDescriptor(key: "startedAt", ascending: false)]
            fetchRequest.fetchLimit = maxSessionCount

            recentSessions = try context.fetch(fetchRequest)
        } catch {
            logger.error("Failed to load sessions: \(error.localizedDescription)")
        }
    }

    private func cleanupOldSessions() async {
        do {
            let context = coreDataStack.newBackgroundContext()
            let cutoffDate = Date().addingTimeInterval(-maxSessionAge)

            try await context.perform {
                let fetchRequest: NSFetchRequest<PhotoImportSession> = PhotoImportSession.fetchRequest()
                fetchRequest.predicate = NSPredicate(format: "startedAt < %@ AND (status == %@ OR status == %@)",
                                                    cutoffDate as NSDate,
                                                    PhotoImportSession.SessionStatus.completed.rawValue,
                                                    PhotoImportSession.SessionStatus.cancelled.rawValue)

                let oldSessions = try context.fetch(fetchRequest)
                for session in oldSessions {
                    context.delete(session)
                }

                if !oldSessions.isEmpty {
                    try context.save()
                    self.logger.info("Cleaned up \(oldSessions.count) old sessions")
                }
            }
        } catch {
            logger.error("Failed to cleanup old sessions: \(error.localizedDescription)")
        }
    }

    // MARK: - Error Types

    enum ImportError: LocalizedError {
        case sessionNotFound
        case invalidSession

        var errorDescription: String? {
            switch self {
            case .sessionNotFound:
                return "Import session not found"
            case .invalidSession:
                return "Invalid import session"
            }
        }
    }
}

// MARK: - Core Data Extension

extension PhotoImportSession {
    @nonobjc public class func fetchRequest() -> NSFetchRequest<PhotoImportSession> {
        return NSFetchRequest<PhotoImportSession>(entityName: "PhotoImportSession")
    }
}