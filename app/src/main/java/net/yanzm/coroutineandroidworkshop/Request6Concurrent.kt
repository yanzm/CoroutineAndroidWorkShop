package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

suspend fun loadContributorsConcurrent(req: RequestData): List<User> = coroutineScope {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    val repos = service.listOrgRepos(req.org).await()

    Timber.i("${req.org}: loaded ${repos.size} repos")

    val contribs = repos.map { repo ->
        async {
            val users = service.listRepoContributors(req.org, repo.name).await()

            Timber.i("${repo.name}: loaded ${users.size} contributors")

            users
        }
    }.awaitAll().flatten().aggregate()


    Timber.i("Total: ${contribs.size} contributors")

    contribs
}
