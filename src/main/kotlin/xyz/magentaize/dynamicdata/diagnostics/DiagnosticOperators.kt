package xyz.magentaize.dynamicdata.diagnostics

import io.reactivex.rxjava3.core.Observable
import xyz.magentaize.dynamicdata.cache.ChangeSet

fun <K, V> Observable<ChangeSet<K, V>>.collectUpdateStats(): Observable<ChangeSummary> =
    this.scan(ChangeSummary.empty()) { seed, next ->
        val index = seed.overall.index + 1
        val adds = seed.overall.adds + next.adds
        val updates = seed.overall.update + next.updates
        val removes = seed.overall.removes + next.removes
        val evaluates = seed.overall.refreshes + next.refreshes
        val moves = seed.overall.moves + next.moves
        val total = seed.overall.size + next.size

        val latest =
            ChangeStatistics(index, next.adds, next.updates, next.removes, next.refreshes, next.moves, next.size)
        val overall = ChangeStatistics(index, adds, updates, removes, evaluates, moves, total)

        return@scan ChangeSummary(index, latest, overall)
    }