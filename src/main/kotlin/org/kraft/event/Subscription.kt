package org.kraft.event

/**
 * Handle representing an active event subscription that can be cancelled to avoid memory leaks.
 */
interface Subscription {
    /**
     * Unsubscribes from the event bus.
     */
    fun unsubscribe()
}
