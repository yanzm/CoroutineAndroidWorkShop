package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.withContext

fun List<User>.aggregate(): List<User> = groupingBy { it.login }
    .reduce { login, a, b -> User(login, a.contributions + b.contributions) }
    .values
    .sortedByDescending { it.contributions }

fun List<User>.aggregate2(): List<User> = groupBy { it.login }
    .mapValues { (key, value) -> User(key, value.sumBy { it.contributions }) }
    .values
    .sortedByDescending { it.contributions }

val computation = newFixedThreadPoolContext(2, "Computation")

suspend fun List<User>.aggregateSlow(): List<User> = withContext(computation) {
    aggregate().also {
        Thread.sleep(500)
    }
}
