package dynamicdata.aggregation

import dynamicdata.list.IChangeSet
import dynamicdata.list.ListChangeReason

internal class AggregateEnumerator<T>(
    private val _source: IChangeSet<T>
) : AggregateChangeSet<T> {
    override fun iterator(): Iterator<AggregateItem<T>> =
        iterator {
            _source.forEach { change ->
                when (change.reason) {
                    ListChangeReason.Add ->
                        yield(AggregateItem(AggregateType.Add, change.item.current))

                    ListChangeReason.AddRange ->
                        yieldAll(change.range.map { AggregateItem(AggregateType.Add, it) })

                    ListChangeReason.Replace -> {
                        yield(AggregateItem(AggregateType.Remove, change.item.previous.value))
                        yield(AggregateItem(AggregateType.Remove, change.item.current))
                    }
                    ListChangeReason.Remove ->
                        yield(AggregateItem(AggregateType.Remove, change.item.current))

                    ListChangeReason.RemoveRange, ListChangeReason.Clear ->
                        yieldAll(change.range.map { AggregateItem(AggregateType.Remove, it) })
                }
            }
        }

}
