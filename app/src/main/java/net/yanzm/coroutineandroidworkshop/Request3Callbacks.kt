package net.yanzm.coroutineandroidworkshop

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

fun loadContributorsCallbacks(req: RequestData, callback: (List<User>) -> Unit) {
    val service = createGitHubService(req.username, req.password)

    Timber.i("Loading ${req.org} repos")

    service.listOrgRepos(req.org).responseCallback { repos ->
        Timber.i("${req.org}: loaded ${repos.size} repos")

        val out = mutableListOf<List<User>>()

        fun readUsers(index: Int, out: MutableList<List<User>>) {
            if (index < repos.size) {
                val repo = repos[index]
                service.listRepoContributors(req.org, repo.name).responseCallback { users ->
                    Timber.i("${repo.name}: loaded ${users.size} contributors")

                    out.add(users)

                    readUsers(index + 1, out)
                }
            } else {
                val contribs = out.flatten().aggregate()

                Timber.i("Total: ${contribs.size} contributors")

                callback(contribs)
            }
        }

        readUsers(0, out)
    }
}

inline fun <T> Call<T>.responseCallback(crossinline callback: (T) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            checkResponse(response)
            callback(response.body()!!)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            Timber.e(t)
        }
    })
}
