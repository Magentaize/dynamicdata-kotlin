package dynamicdata.list.internal

import dynamicdata.list.ChangeAwareList

internal class ReaderWriter<T>{
    private val data = ChangeAwareList<T>()
}
