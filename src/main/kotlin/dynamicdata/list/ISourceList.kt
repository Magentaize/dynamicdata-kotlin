package dynamicdata.list

interface ISourceList<T> : ObservableList<T> {
    fun edit(action: (IExtendedList<T>) -> Unit)
}
