package dynamicdata.cache

import dynamicdata.cache.internal.StatusMonitor
import dynamicdata.kernel.ConnectionStatus
import io.reactivex.rxjava3.core.Observable

fun <T> Observable<T>.monitorStatus(): Observable<ConnectionStatus> =
    StatusMonitor(this).run()