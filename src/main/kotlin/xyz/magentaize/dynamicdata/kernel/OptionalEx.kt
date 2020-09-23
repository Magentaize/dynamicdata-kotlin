package xyz.magentaize.dynamicdata.kernel

import java.lang.Exception

fun <T, R> Optional<T>.convert(converter: (T) -> R): Optional<R> =
    if (hasValue) Optional.of(converter(value)) else Optional.empty<R>()

fun <T, R> Optional<T>.convertOr(
    converter: (T) -> R,
    fallbackConverter: () -> R
): R =
    if (hasValue) converter(value) else fallbackConverter()

fun <T, R> Map<T, R>.lookup(key: T): Optional<R> {
    val ret = this.getOrDefault(key, null)
    return if (ret != null) Optional.of(ret) else Optional.empty()
}

fun <T> Optional<T>.ifHasValue(action: (T) -> Unit): OptionalElse {
    if (!hasValue) return OptionalElse()

    action(value)
    return OptionalElse.empty()
}

fun <T> Optional<T>.valueOrThrow(factory: () -> Exception): T {
    if (hasValue)
        return value

    throw factory()
}
