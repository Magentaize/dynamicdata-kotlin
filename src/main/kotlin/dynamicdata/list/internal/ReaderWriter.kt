package dynamicdata.list.internal

import dynamicdata.list.ChangeAwareList
import dynamicdata.list.IChangeSet
import dynamicdata.list.IExtendedList
import dynamicdata.list.clone

internal class ReaderWriter<T>() {
    private var data = ChangeAwareList<T>()
    private val lock = Any()
    private var updateInProgress = false

    fun write(changes: IChangeSet<T>): IChangeSet<T> =
        synchronized(lock) {
            data.clone(changes)
            data.captureChanges()
        }

    fun write(action: (IExtendedList<T>) -> Unit): IChangeSet<T> =
        synchronized(lock) {
            updateInProgress = true
            action(data)
            updateInProgress = false
            data.captureChanges()
        }

    fun writeWithPreview(action: (IExtendedList<T>) -> Unit, previewHandler: (IChangeSet<T>) -> Unit): IChangeSet<T> =
        synchronized(lock) {
            var copy = ChangeAwareList(data, false)

            updateInProgress = true
            action(data)
            updateInProgress = false

            val ret = data.captureChanges()

            data = copy.also { copy = data }
            previewHandler(ret)
            data = copy.also { copy = data }

            return@synchronized ret
        }

    fun writeNested(action: (IExtendedList<T>) -> Unit) =
        synchronized(lock) {
            if (!updateInProgress)
                throw IllegalStateException("WriteNested can only be used if another write is already in progress.")

            action(data)
        }

    val items: List<T>
        get() = synchronized(lock) {
            data.toList()
        }

    val size: Int
        get() = synchronized(lock) {
            data.size
        }
}
