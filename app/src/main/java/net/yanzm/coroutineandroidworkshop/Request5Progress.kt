package net.yanzm.coroutineandroidworkshop

import timber.log.Timber

suspend fun loadContributorsProgress(req: RequestData, callback: (List<User>) -> Unit) {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    val repos = service.listOrgRepos(req.org).await()

    Timber.i("${req.org}: loaded ${repos.size} repos")

    var contribs = listOf<User>()

    for (repo in repos) {
        val users = service.listRepoContributors(req.org, repo.name).await()

        Timber.i("${repo.name}: loaded ${users.size} contributors")

        contribs = (contribs + users).aggregateSlow()
        callback(contribs)
    }

    Timber.i("Total: ${contribs.size} contributors")
}
