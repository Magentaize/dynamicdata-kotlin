package dynamicdata.list.internal

import java.time.Instant
import java.time.LocalDateTime

internal data class ExpirableItem<T>(
    val item: T,
    val expireAt: Instant,
    val index: Int
)
