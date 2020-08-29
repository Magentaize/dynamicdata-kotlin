package dynamicdata.list

import org.reactivestreams.Processor
import reactor.core.Disposable
import reactor.core.publisher.EmitterProcessor

class SourceList<T> {
    private val changes = EmitterProcessor.create<IChangeSet<T>>()
    private val changesPreview = EmitterProcessor.create<IChangeSet<T>>()
    private val countChange  = lazy { EmitterProcessor.create<Int>() }

    //private var cleanUp: Disposable
}
