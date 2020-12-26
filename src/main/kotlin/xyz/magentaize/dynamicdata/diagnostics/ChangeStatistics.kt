package xyz.magentaize.dynamicdata.diagnostics

data class ChangeStatistics(
    val index: Int,
    val adds: Int,
    val update: Int,
    val removes: Int,
    val refreshes: Int,
    val moves: Int,
    val size: Int
) {
    companion object {
        @JvmStatic
        private val INSTANCE = ChangeStatistics(-1, -1, -1, -1, -1, -1, -1)

        @JvmStatic
        fun empty(): ChangeStatistics = INSTANCE
    }
}