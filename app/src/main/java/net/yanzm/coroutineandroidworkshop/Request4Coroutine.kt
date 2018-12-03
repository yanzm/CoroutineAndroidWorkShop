package net.yanzm.coroutineandroidworkshop

import timber.log.Timber
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

suspend fun loadContributors(req: RequestData) : List<User> {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    val repos = service.listOrgRepos(req.org).await()

    Timber.i("${req.org}: loaded ${repos.size} repos")

    val contribs = repos.flatMap { repo ->
        val users = service.listRepoContributors(req.org, repo.name).await()

        Timber.i("${repo.name}: loaded ${users.size} contributors")

        users
    }.aggregate()

    Timber.i("Total: ${contribs.size} contributors")

    return contribs
}
