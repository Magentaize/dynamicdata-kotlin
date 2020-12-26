package xyz.magentaize.dynamicdata.diagnostics

data class ChangeSummary(
    private val _index: Int,
    val latest: ChangeStatistics,
    val overall: ChangeStatistics
) {
    companion object {
        @JvmStatic
        private val INSTANCE = ChangeSummary(
            -1,
            ChangeStatistics(-1, 0, 0, 0, 0, 0, 0),
            ChangeStatistics(-1, 0, 0, 0, 0, 0, 0)
        )

        @JvmStatic
        fun empty(): ChangeSummary = INSTANCE
    }

    override fun toString(): String =
        "CurrentIndex=${_index}, Latest Size=${latest.size}, Overall Size=${overall.size}"
}