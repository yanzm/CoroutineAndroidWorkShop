package net.yanzm.coroutineandroidworkshop

import android.util.Base64
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface GitHubService {
    @GET("orgs/{org}/repos?per_page=100")
    fun listOrgRepos(
        @Path("org") org: String
    ): Call<List<Repo>>

    @GET("repos/{owner}/{repo}/contributors?per_page=100")
    fun listRepoContributors(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): Call<List<User>>
}

data class Repo(
    val id: Long,
    val name: String
)

data class User(
    val login: String,
    val contributions: Int
)

data class RequestData(
    val username: String,
    val password: String,
    val org: String
)

fun createGitHubService(username: String, password: String): GitHubService {
    val authToken = "Basic " + Base64.encode(
        "$username:$password".toByteArray(),
        Base64.NO_PADDING or Base64.NO_WRAP
    ).toString(Charsets.UTF_8)

    val httpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val builder = original.newBuilder()
                .header("Accept", "application/vnd.github.v3+json")
                .header("Authorization", authToken)
            val request = builder.build()
            chain.proceed(request)
        }.build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com")
        .addConverterFactory(MoshiConverterFactory.create())
        .client(httpClient)
        .build()

    return retrofit.create(GitHubService::class.java)
}
