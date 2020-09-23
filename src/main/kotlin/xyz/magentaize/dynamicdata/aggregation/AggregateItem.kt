package xyz.magentaize.dynamicdata.aggregation

data class AggregateItem<T>(
    val type: AggregateType,
    val item: T
){
    override fun equals(other: Any?): Boolean {
        return if(other is AggregateItem<*>)
            item == other.item
        else
            false
    }
}
