package dynamicdata.list

interface ISourceList<T> : IObservableList<T> {
    fun edit(action: (IExtendedList<T>) -> Unit)
}
