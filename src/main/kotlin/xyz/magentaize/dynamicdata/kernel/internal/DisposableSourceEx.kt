package xyz.magentaize.dynamicdata.kernel.internal

import io.reactivex.rxjava3.disposables.Disposable

class DisposableEx {
    companion object {
        @JvmStatic
        fun <T> fromAction(state: T, action: (T) -> Unit): Disposable =
            AnonymousDisposable(state, action)
    }
}