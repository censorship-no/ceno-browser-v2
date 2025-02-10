package ie.equalit.ceno.ext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

/**
 * Returns a [Flow] containing only values of the original [Flow] that have changed compared to
 * the value emitted before them.
 *
 * Example:
 * ```
 * Original Flow: A, B, B, C, A, A, A, D, A
 * Returned Flow: A, B, C, A, D, A
 * ```
 */
fun <T> Flow<T>.ifChanged(): Flow<T> = ifChanged { it }

/**
 * Returns a [Flow] containing only values of the original [Flow] where the result of calling
 * [transform] has changed from the result of the previous value.
 *
 * Example:
 * ```
 * Block: x -> x[0]  // Map to first character of input
 * Original Flow: "banana", "bus", "apple", "big", "coconut", "circle", "home"
 * Mapped: b, b, a, b, c, c, h
 * Returned Flow: "banana", "apple", "big", "coconut", "home"
 * ```
 */
fun <T, R> Flow<T>.ifChanged(transform: (T) -> R): Flow<T> {
    var observedValueOnce = false
    var lastMappedValue: R? = null

    return filter { value ->
        val mapped = transform(value)
        if (!observedValueOnce || mapped != lastMappedValue) {
            lastMappedValue = mapped
            observedValueOnce = true
            true
        } else {
            false
        }
    }
}