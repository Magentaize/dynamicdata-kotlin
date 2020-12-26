package xyz.magentaize.dynamicdata.kernel

@Suppress("UNCHECKED_CAST")
class Stub {
    companion object {
        @JvmStatic
        fun <T> emptyFallback(): () -> T = EMPTY_FALLBACK as () -> T

        @JvmStatic
        private val EMPTY_FALLBACK: () -> Any = { throw IllegalStateException() }

        @JvmStatic
        val EMPTY_COMSUMER: (Any) -> Nothing = { throw IllegalStateException() }

        @JvmStatic
        fun <T> emptyFilter(): (T) -> Boolean = EMPTY_FILTER as (T) -> Boolean

        @JvmStatic
        private val EMPTY_FILTER: (Any) -> Boolean = { throw IllegalStateException() }

        @JvmStatic
        fun <K, V> emptyMapper(): (V) -> K = EMPTY_MAPPER as (V) -> K

        @JvmStatic
        private val EMPTY_MAPPER: (Any) -> (Any) = { throw IllegalStateException("keySelector is not specified.") }
    }
}