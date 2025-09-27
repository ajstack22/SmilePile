package com.smilepile.security

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Circuit breaker pattern implementation for file operations.
 * Prevents cascading failures by temporarily blocking operations after repeated failures.
 *
 * States:
 * - CLOSED: Normal operation, requests pass through
 * - OPEN: Circuit is tripped, requests fail fast
 * - HALF_OPEN: Testing if service has recovered
 */
class CircuitBreaker(
    private val failureThreshold: Int = 3,
    private val resetTimeoutMs: Long = 60_000L,
    private val halfOpenMaxAttempts: Int = 1
) {
    private val failures = AtomicInteger(0)
    private val lastFailureTime = AtomicLong(0)
    private val state = AtomicReference(State.CLOSED)
    private val halfOpenAttempts = AtomicInteger(0)
    private val mutex = Mutex()

    enum class State {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    /**
     * Execute a block of code with circuit breaker protection
     */
    suspend fun <T> execute(
        operation: String = "unknown",
        block: suspend () -> T
    ): T {
        return when (state.get()) {
            State.OPEN -> {
                if (shouldAttemptReset()) {
                    mutex.withLock {
                        if (state.get() == State.OPEN && shouldAttemptReset()) {
                            state.set(State.HALF_OPEN)
                            halfOpenAttempts.set(0)
                        }
                    }
                    executeInHalfOpen(operation, block)
                } else {
                    throw CircuitBreakerOpenException(
                        "Circuit breaker is OPEN for operation: $operation. " +
                        "Will retry after ${getRemainingTimeout()}ms"
                    )
                }
            }
            State.HALF_OPEN -> executeInHalfOpen(operation, block)
            State.CLOSED -> executeInClosed(operation, block)
        }
    }

    private suspend fun <T> executeInClosed(
        operation: String,
        block: suspend () -> T
    ): T {
        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw CircuitBreakerException(
                "Operation failed in CLOSED state: $operation",
                e
            )
        }
    }

    private suspend fun <T> executeInHalfOpen(
        operation: String,
        block: suspend () -> T
    ): T {
        if (halfOpenAttempts.incrementAndGet() > halfOpenMaxAttempts) {
            throw CircuitBreakerOpenException(
                "Circuit breaker is HALF_OPEN but max attempts reached for: $operation"
            )
        }

        return try {
            val result = block()
            onSuccess()
            result
        } catch (e: Exception) {
            onFailure()
            throw CircuitBreakerException(
                "Operation failed in HALF_OPEN state: $operation",
                e
            )
        }
    }

    private fun onSuccess() {
        failures.set(0)
        state.set(State.CLOSED)
        halfOpenAttempts.set(0)
    }

    private fun onFailure() {
        lastFailureTime.set(System.currentTimeMillis())
        if (failures.incrementAndGet() >= failureThreshold) {
            state.set(State.OPEN)
        }
    }

    private fun shouldAttemptReset(): Boolean {
        val timeSinceLastFailure = System.currentTimeMillis() - lastFailureTime.get()
        return timeSinceLastFailure >= resetTimeoutMs
    }

    private fun getRemainingTimeout(): Long {
        val elapsed = System.currentTimeMillis() - lastFailureTime.get()
        return (resetTimeoutMs - elapsed).coerceAtLeast(0)
    }

    /**
     * Get current circuit breaker state
     */
    fun getState(): State = state.get()

    /**
     * Get current failure count
     */
    fun getFailureCount(): Int = failures.get()

    /**
     * Manually reset the circuit breaker
     */
    fun reset() {
        failures.set(0)
        state.set(State.CLOSED)
        halfOpenAttempts.set(0)
        lastFailureTime.set(0)
    }

    /**
     * Force the circuit breaker open (for testing)
     */
    fun forceOpen() {
        state.set(State.OPEN)
        lastFailureTime.set(System.currentTimeMillis())
    }
}

/**
 * Exception thrown when circuit breaker is open
 */
class CircuitBreakerOpenException(message: String) : Exception(message)

/**
 * Exception wrapper for circuit breaker failures
 */
class CircuitBreakerException(message: String, cause: Throwable) : Exception(message, cause)