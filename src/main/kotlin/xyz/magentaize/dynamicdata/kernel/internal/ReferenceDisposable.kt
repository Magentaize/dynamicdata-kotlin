package xyz.magentaize.dynamicdata.kernel.internal

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.disposables.Disposable
import java.util.*
import java.util.concurrent.atomic.AtomicReference

internal abstract class ReferenceDisposable<T>(value: T) :
    AtomicReference<T>(Objects.requireNonNull(value, "value is null")), Disposable {
    protected abstract fun onDisposed(value: @NonNull T)
    override fun dispose() {
        var value = get()
        if (value != null) {
            value = getAndSet(null)
            value?.let { onDisposed(it) }
        }
    }

    override fun isDisposed(): Boolean {
        return get() == null
    }
}
