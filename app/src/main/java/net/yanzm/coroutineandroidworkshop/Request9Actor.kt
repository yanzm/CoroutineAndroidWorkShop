package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

fun CoroutineScope.aggregatorActor(
    uiUpdateActor: SendChannel<List<User>>
) = actor<List<User>> {
    var contribs: List<User> = emptyList() // STATE
    for (users in channel) {
        // TODO: :UPDATE STATE:
        uiUpdateActor.send(contribs)
    }
}

class WorkerRequest(
    val service: GitHubService,
    val org: String,
    val repo: String
)

fun CoroutineScope.workerJob(
    requests: ReceiveChannel<WorkerRequest>,
    aggregator: SendChannel<List<User>>
) = launch {
    for (req in requests) {
        val users = TODO()
        aggregator.send(users)
    }
}

suspend fun loadContributorsActor(req: RequestData, uiUpdateActor: SendChannel<List<User>>) = coroutineScope<Unit> {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    val repos = service.listOrgRepos(req.org).await()

    Timber.i("${req.org}: loaded ${repos.size} repos")

    val aggregator = aggregatorActor(uiUpdateActor)

    val requests = Channel<WorkerRequest>()
    val workers = List(4) {
        workerJob(requests, aggregator)
    }
    for (repo in repos) {
        requests.send(WorkerRequest(service, req.org, repo.name))
    }
    requests.close()
    // TOOD: join workers & complete
}
