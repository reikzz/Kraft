package org.kraft.core.event

import kotlin.reflect.KClass

/**
 * A lightweight, type-safe event bus for decoupling game systems.
 * Supports multiple instances to isolate client and server events.
 */
class EventBus {
    private val handlers = mutableMapOf<KClass<*>, MutableList<(Any) -> Unit>>()

    /**
     * Subscribes to events of type [T]. Returns a [Subscription] to unsubscribe later.
     */
    fun <T : Any> subscribe(eventType: KClass<T>, handler: (T) -> Unit): Subscription {
        val wrapper: (Any) -> Unit = {
            @Suppress("UNCHECKED_CAST")
            handler(it as T)
        }
        handlers.getOrPut(eventType) { mutableListOf() }.add(wrapper)
        return object : Subscription {
            override fun unsubscribe() {
                handlers[eventType]?.remove(wrapper)
            }
        }
    }

    /**
     * Reified helper for subscribing to events.
     */
    inline fun <reified T : Any> subscribe(noinline handler: (T) -> Unit): Subscription {
        return subscribe(T::class, handler)
    }

    /**
     * Publishes an event to all active subscribers.
     */
    fun publish(event: Any) {
        handlers[event::class]?.toList()?.forEach { it(event) }
    }
}
