package dynamicdata.kernel

class OptionalElse internal constructor(private val _shouldRunAction: Boolean = true) {
    companion object {
        private val INSTANCE = OptionalElse()

        fun empty(): OptionalElse {
            return INSTANCE
        }
    }

    fun `else`(action: () -> Unit) {
        if (_shouldRunAction)
            action()
    }
}
