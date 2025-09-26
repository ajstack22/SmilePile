import Foundation

/// Dependency Injection Container for managing service instances
public final class DIContainer {

    /// Singleton instance
    public static let shared = DIContainer()

    /// Service registration scope
    public enum Scope {
        case singleton
        case transient
    }

    /// Service registration entry
    private struct Registration {
        let factory: () -> Any
        let scope: Scope
        var instance: Any?
    }

    /// Thread-safe storage for registrations
    private var registrations: [String: Registration] = [:]
    private let queue = DispatchQueue(label: "com.smilepile.di", attributes: .concurrent)

    private init() {}

    /// Register a service with the container
    public func register<T>(_ type: T.Type, scope: Scope = .singleton, factory: @escaping () -> T) {
        let key = String(describing: type)

        queue.async(flags: .barrier) {
            self.registrations[key] = Registration(
                factory: factory,
                scope: scope,
                instance: nil
            )
        }
    }

    /// Resolve a service from the container
    public func resolve<T>(_ type: T.Type) -> T? {
        let key = String(describing: type)

        return queue.sync {
            guard var registration = registrations[key] else {
                return nil
            }

            switch registration.scope {
            case .singleton:
                if registration.instance == nil {
                    // Create singleton instance in barrier block
                    var newInstance: Any?
                    queue.async(flags: .barrier) {
                        if registration.instance == nil {
                            registration.instance = registration.factory()
                            self.registrations[key] = registration
                        }
                        newInstance = registration.instance
                    }
                    queue.sync {
                        registration = self.registrations[key]!
                    }
                }
                return registration.instance as? T

            case .transient:
                return registration.factory() as? T
            }
        }
    }

    /// Check if a service is registered
    public func isRegistered<T>(_ type: T.Type) -> Bool {
        let key = String(describing: type)
        return queue.sync { registrations[key] != nil }
    }

    /// Reset all registrations
    public func reset() {
        queue.async(flags: .barrier) {
            self.registrations.removeAll()
        }
    }
}

/// Property wrapper for dependency injection
@propertyWrapper
public class Injected<T> {
    private var service: T?

    public init() {}

    public var wrappedValue: T {
        get {
            if service == nil {
                service = DIContainer.shared.resolve(T.self)
            }
            guard let service = service else {
                fatalError("Service of type \(T.self) not registered")
            }
            return service
        }
        set {
            service = newValue
        }
    }
}