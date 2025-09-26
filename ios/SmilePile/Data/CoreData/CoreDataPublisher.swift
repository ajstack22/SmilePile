import CoreData
import Combine

// MARK: - Core Data Publisher Extension

extension NSManagedObjectContext {
    func publisher<T: NSManagedObject>(for fetchRequest: NSFetchRequest<T>) -> CoreDataPublisher<T> {
        CoreDataPublisher(fetchRequest: fetchRequest, context: self)
    }
}

// MARK: - Core Data Publisher

struct CoreDataPublisher<Entity: NSManagedObject>: Publisher {
    typealias Output = [Entity]
    typealias Failure = Error

    let fetchRequest: NSFetchRequest<Entity>
    let context: NSManagedObjectContext

    func receive<S>(subscriber: S) where S: Subscriber, Self.Failure == S.Failure, Self.Output == S.Input {
        let subscription = CoreDataSubscription(
            subscriber: subscriber,
            fetchRequest: fetchRequest,
            context: context
        )
        subscriber.receive(subscription: subscription)
    }
}

// MARK: - Core Data Subscription

private final class CoreDataSubscription<S: Subscriber, Entity: NSManagedObject>: NSObject, Subscription, NSFetchedResultsControllerDelegate
where S.Input == [Entity], S.Failure == Error {

    private var subscriber: S?
    private let fetchRequest: NSFetchRequest<Entity>
    private let context: NSManagedObjectContext
    private var fetchedResultsController: NSFetchedResultsController<Entity>?

    init(subscriber: S, fetchRequest: NSFetchRequest<Entity>, context: NSManagedObjectContext) {
        self.subscriber = subscriber
        self.fetchRequest = fetchRequest
        self.context = context
        super.init()

        setupFetchedResultsController()
    }

    func request(_ demand: Subscribers.Demand) {
        // Initial fetch
        performFetch()
    }

    func cancel() {
        subscriber = nil
        fetchedResultsController = nil
    }

    private func setupFetchedResultsController() {
        // Ensure we have at least one sort descriptor
        if fetchRequest.sortDescriptors?.isEmpty ?? true {
            fetchRequest.sortDescriptors = [NSSortDescriptor(key: "objectID", ascending: true)]
        }

        fetchedResultsController = NSFetchedResultsController(
            fetchRequest: fetchRequest,
            managedObjectContext: context,
            sectionNameKeyPath: nil,
            cacheName: nil
        )

        fetchedResultsController?.delegate = self
    }

    private func performFetch() {
        do {
            try fetchedResultsController?.performFetch()
            sendResults()
        } catch {
            subscriber?.receive(completion: .failure(error))
        }
    }

    private func sendResults() {
        guard let objects = fetchedResultsController?.fetchedObjects else { return }
        _ = subscriber?.receive(objects)
    }

    // MARK: - NSFetchedResultsControllerDelegate

    @objc func controllerDidChangeContent(_ controller: NSFetchedResultsController<NSFetchRequestResult>) {
        sendResults()
    }
}

// MARK: - Fetch Request Builder

struct FetchRequestBuilder<Entity: NSManagedObject> {
    private var request: NSFetchRequest<Entity>

    init(_ entityType: Entity.Type) {
        self.request = NSFetchRequest<Entity>(entityName: String(describing: entityType))
    }

    func predicate(_ predicate: NSPredicate) -> FetchRequestBuilder {
        var builder = self
        builder.request.predicate = predicate
        return builder
    }

    func sortDescriptors(_ descriptors: [NSSortDescriptor]) -> FetchRequestBuilder {
        var builder = self
        builder.request.sortDescriptors = descriptors
        return builder
    }

    func limit(_ limit: Int) -> FetchRequestBuilder {
        var builder = self
        builder.request.fetchLimit = limit
        return builder
    }

    func batchSize(_ size: Int) -> FetchRequestBuilder {
        var builder = self
        builder.request.fetchBatchSize = size
        return builder
    }

    func build() -> NSFetchRequest<Entity> {
        request
    }
}

// MARK: - Predicate Helpers

extension NSPredicate {
    static func photoSearch(query: String) -> NSPredicate {
        NSPredicate(format: "uri CONTAINS[cd] %@", query)
    }

    static func photoCategory(id: Int64) -> NSPredicate {
        NSPredicate(format: "categoryId == %ld", id)
    }

    static func photoDateRange(start: Int64, end: Int64) -> NSPredicate {
        NSPredicate(format: "timestamp BETWEEN %@", [start, end] as NSArray)
    }

    static func photoFavorites() -> NSPredicate {
        NSPredicate(format: "isFavorite == TRUE")
    }

    static func photosWithEncryptedData() -> NSPredicate {
        NSPredicate(format: "encryptedChildName != nil OR encryptedChildAge != nil OR encryptedNotes != nil OR encryptedTags != nil OR encryptedMilestone != nil OR encryptedLocation != nil OR encryptedMetadata != nil")
    }

    static func categoryByName(_ name: String) -> NSPredicate {
        NSPredicate(format: "displayName ==[c] %@", name)
    }

    static func categorySearch(query: String) -> NSPredicate {
        NSPredicate(format: "displayName CONTAINS[cd] %@", query)
    }

    static func and(_ predicates: [NSPredicate]) -> NSPredicate {
        NSCompoundPredicate(andPredicateWithSubpredicates: predicates)
    }

    static func or(_ predicates: [NSPredicate]) -> NSPredicate {
        NSCompoundPredicate(orPredicateWithSubpredicates: predicates)
    }
}