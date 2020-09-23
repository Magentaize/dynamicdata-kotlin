package xyz.magentaize.dynamicdata.kernel

import java.util.*
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import kotlin.NoSuchElementException

class Optional<T> {
    private val _value: T?

    private constructor() {
        _value = null
    }

    private constructor(value: T) {
        this._value = Objects.requireNonNull(value)
    }

    val value: T
    get(){
        if (_value == null) {
            throw NoSuchElementException("No value present")
        }
        return _value
    }

    val hasValue: Boolean
        get() = _value != null

    fun ifPresent(consumer: Consumer<in T>) {
        if (_value != null) consumer.accept(_value)
    }

    fun filter(predicate: Predicate<in T?>): Optional<T> {
        Objects.requireNonNull(predicate)
        return if (!hasValue) this else if (predicate.test(_value)) this else empty()
    }

    fun <U> map(mapper: Function<in T?, out U>): Optional<U> {
        Objects.requireNonNull(mapper)
        return if (!hasValue) empty() else {
            ofNullable(mapper.apply(_value))
        }
    }

    fun <U> flatMap(mapper: Function<in T?, Optional<U>?>): Optional<U> {
        Objects.requireNonNull(mapper)
        return if (!hasValue) empty() else {
            Objects.requireNonNull(mapper.apply(_value))!!
        }
    }

    fun orElse(other: T): T {
        return _value ?: other
    }

    fun orElseGet(other: Supplier<out T>): T {
        return _value ?: other.get()
    }

    override fun equals(obj: Any?): Boolean {
        if (this === obj) {
            return true
        }
        if (obj !is Optional<*>) {
            return false
        }
        return _value == obj._value
    }

    override fun hashCode(): Int {
        return Objects.hashCode(_value)
    }

    override fun toString(): String {
        return if (_value != null) String.format("Optional[%s]", _value) else "Optional.empty"
    }

    companion object {
        private val EMPTY: Optional<*> = Optional<Any>()

        fun <T> empty(): Optional<T> {
            return EMPTY as Optional<T>
        }

        fun <T> of(value: T): Optional<T> {
            return Optional(value)
        }

        fun <T> ofNullable(value: T?): Optional<T> {
            return if (value == null) empty() else of(value)
        }
    }
}
