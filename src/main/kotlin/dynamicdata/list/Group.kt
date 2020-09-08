package dynamicdata.list

interface Group<T, out K> {
    val key: K
    val list: IObservableList<T>
}
