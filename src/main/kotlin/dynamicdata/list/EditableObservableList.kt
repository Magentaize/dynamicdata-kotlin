package dynamicdata.list

interface EditableObservableList<T> : ObservableList<T> {
    fun edit(action: (ExtendedList<T>) -> Unit)
}
