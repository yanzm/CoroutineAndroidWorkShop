package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.coroutineScope

suspend fun loadContributorsConcurrent(req: RequestData): List<User> = coroutineScope {
    TODO()
}
