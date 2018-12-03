package net.yanzm.coroutineandroidworkshop

import retrofit2.Call
import retrofit2.Response
import timber.log.Timber

fun loadContributorsBlocking(req: RequestData) : List<User> {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    val repos = service.listOrgRepos(req.org).responseBodyBlocking()

    Timber.i("${req.org}: loaded ${repos.size} repos")

    val contribs = repos.flatMap { repo ->
        val users = service.listRepoContributors(req.org, repo.name).responseBodyBlocking()

        Timber.i("${repo.name}: loaded ${users.size} contributors")

        users
    }.aggregate()

    Timber.i("Total: ${contribs.size} contributors")

    return contribs
}

fun <T> Call<T>.responseBodyBlocking(): T {
    val response = execute() // Executes requests and blocks current thread
    checkResponse(response)
    return response.body()!!
}

fun checkResponse(response: Response<*>) {
    check(response.isSuccessful) {
        "Failed with ${response.code()}: ${response.message()}\n${response.errorBody()?.string()}"
    }
}
