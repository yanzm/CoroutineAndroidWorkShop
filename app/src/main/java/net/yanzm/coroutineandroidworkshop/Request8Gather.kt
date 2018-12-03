package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

suspend fun loadContributorsGather(req: RequestData, callback: suspend (List<User>) -> Unit) = coroutineScope {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    val repos = service.listOrgRepos(req.org).await()

    Timber.i("${req.org}: loaded ${repos.size} repos")

    val channel = Channel<List<User>>()

    for (repo in repos) {
        launch {
            val users = service.listRepoContributors(req.org, repo.name).await()
            Timber.i("${repo.name}: loaded ${users.size} contributors")
            channel.send(users)
        }
    }

    var contribs = emptyList<User>()
    repeat(repos.size) {
        val users = channel.receive()
        contribs = (contribs + users).aggregateSlow()
        callback(contribs)
    }

    Timber.i("Total: ${contribs.size} contributors")
}
