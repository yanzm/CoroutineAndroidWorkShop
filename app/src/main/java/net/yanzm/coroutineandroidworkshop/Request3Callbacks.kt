package net.yanzm.coroutineandroidworkshop

import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

fun loadContributorsCallbacks(req: RequestData, callback: (List<User>) -> Unit) {
    TODO()
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
