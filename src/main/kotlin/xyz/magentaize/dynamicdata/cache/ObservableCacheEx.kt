package xyz.magentaize.dynamicdata.cache

import xyz.magentaize.dynamicdata.cache.internal.StatusMonitor
import xyz.magentaize.dynamicdata.kernel.ConnectionStatus
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<T>.monitorStatus(): Observable<ConnectionStatus> =
    StatusMonitor(this).run()
