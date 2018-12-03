package net.yanzm.coroutineandroidworkshop

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import timber.log.Timber
import kotlin.coroutines.*

fun CoroutineScope.aggregatorActor(
    uiUpdateActor: SendChannel<List<User>>
) = actor<List<User>> {
    var contribs: List<User> = emptyList() // STATE
    for (users in channel) {
        contribs = (contribs + users).aggregateSlow()
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
        val users = req.service.listRepoContributors(req.org, req.repo).await()

        Timber.i("${req.repo}: loaded ${users.size} contributors")

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
    workers.joinAll()
    aggregator.close()
}
