package dynamicdata.kernel

fun <T, R> Optional<T>.convert(converter: (T) -> R): Optional<R> =
    if (hasValue) Optional.of(converter(value)) else Optional.empty<R>()

fun <T> Optional<T>.ifHasValue(action: (T) -> Unit): OptionalElse {
    action(value)
    return OptionalElse.empty()
}
