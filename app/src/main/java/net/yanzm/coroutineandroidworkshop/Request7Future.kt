package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.*
import kotlinx.coroutines.future.*
import java.util.concurrent.*

fun loadContributorsConcurrentAsync(req: RequestData): CompletableFuture<List<User>> = GlobalScope.future {
    loadContributorsConcurrent(req)
}
