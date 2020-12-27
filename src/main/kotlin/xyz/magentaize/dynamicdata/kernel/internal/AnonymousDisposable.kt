package xyz.magentaize.dynamicdata.kernel.internal

import io.reactivex.rxjava3.annotations.NonNull
import io.reactivex.rxjava3.internal.util.ExceptionHelper

internal class AnonymousDisposable<T>(private val _state: T, value: (T) -> Unit) :
    ReferenceDisposable<(T) -> Unit>(value) {
    override fun onDisposed(value: @NonNull (T) -> Unit) {
        try {
            value(_state)
        } catch (ex: Throwable) {
            throw ExceptionHelper.wrapOrThrow(ex)
        }
    }

    override fun toString(): String {
        return "AnonymousDisposable(disposed=$isDisposed, ${get()})"
    }
}
