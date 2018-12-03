package net.yanzm.coroutineandroidworkshop

import android.os.Bundle
import android.preference.PreferenceManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.list_item.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.launch
import net.yanzm.coroutineandroidworkshop.Variant.ACTOR
import net.yanzm.coroutineandroidworkshop.Variant.BACKGROUND
import net.yanzm.coroutineandroidworkshop.Variant.BLOCKING
import net.yanzm.coroutineandroidworkshop.Variant.CALLBACKS
import net.yanzm.coroutineandroidworkshop.Variant.CANCELLABLE
import net.yanzm.coroutineandroidworkshop.Variant.CONCURRENT
import net.yanzm.coroutineandroidworkshop.Variant.COROUTINE
import net.yanzm.coroutineandroidworkshop.Variant.FUTURE
import net.yanzm.coroutineandroidworkshop.Variant.GATHER
import net.yanzm.coroutineandroidworkshop.Variant.PROGRESS
import timber.log.Timber
import java.util.concurrent.CompletableFuture
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), CoroutineScope {

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    private val adapter = Adapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        job = Job()

        recyclerView.adapter = adapter

        variantSpinner.adapter = ArrayAdapter<Variant>(
            this,
            android.R.layout.simple_dropdown_item_1line,
            Variant.values()
        )

        loadButton.setOnClickListener {
            savePrefs()
            doLoad()
        }

        loadPrefs()
    }

    private fun selectedVariant(): Variant = variantSpinner.selectedItem as Variant

    private fun doLoad() {
        clearResults()

        val req = RequestData(
            userNameEditText.text.toString(),
            tokenEditText.text.toString(),
            organizationEditText.text.toString()
        )

        when (selectedVariant()) {
            BLOCKING -> { // Blocking UI thread
                val users = loadContributorsBlocking(req)
                updateResults(users)
            }
            BACKGROUND -> { // Blocking a background thread
                thread {
                    val users = loadContributorsBlocking(req)
                    runOnUiThread {
                        updateResults(users)
                    }
                }
            }
            CALLBACKS -> { // Using callbacks
                loadContributorsCallbacks(req) { users ->
                    runOnUiThread {
                        updateResults(users)
                    }
                }
            }
            COROUTINE -> { // Using coroutines
                launch {
                    val users = loadContributors(req)
                    updateResults(users)
                }
            }
            PROGRESS -> { // Using coroutines showing progress
                launch {
                    loadContributorsProgress(req) { users ->
                        updateResults(users)
                    }
                }
            }
            CANCELLABLE -> { // Using coroutines with cancellation
                launch {
                    loadContributorsProgress(req) { users ->
                        updateResults(users)
                    }
                }.updateCancelJob()
            }
            CONCURRENT -> {
                launch {
                    updateResults(loadContributorsConcurrent(req))
                }.updateCancelJob()
            }
            FUTURE -> {
                val future = loadContributorsConcurrentAsync(req)
                updateCancelFuture(future)
                future.thenAccept { users ->
                    runOnUiThread {
                        updateResults(users)
                    }
                }
            }
            GATHER -> {
                launch {
                    loadContributorsGather(req) { users ->
                        updateResults(users)
                    }
                }.updateCancelJob()
            }
            ACTOR -> {
                launch {
                    loadContributorsActor(req, uiUpdateActor)
                }.updateCancelJob()
            }
        }
    }

    private val uiUpdateActor by lazy {
        actor<List<User>> {
            for (users in channel) {
                updateResults(users)
            }
        }
    }

    private fun clearResults() {
        updateResults(listOf())
    }

    private fun updateResults(users: List<User>) {
        Timber.i("Updating result with ${users.size} rows")
        adapter.submitList(users)
    }

    private fun Job.updateCancelJob() {
        updateEnabled(false)
        cancelButton.setOnClickListener { cancel() }
        launch {
            join()
            updateEnabled(true)
            cancelButton.setOnClickListener(null)
        }
    }

    private fun updateCancelFuture(future: CompletableFuture<*>) {
        updateEnabled(false)
        cancelButton.setOnClickListener { future.cancel(false) }
        future.whenComplete { _, _ ->
            runOnUiThread {
                updateEnabled(true)
                cancelButton.setOnClickListener(null)
            }
        }
    }

    private fun updateEnabled(enabled: Boolean) {
        loadButton.isEnabled = enabled
        cancelButton.isEnabled = !enabled
    }

    private fun pref() = PreferenceManager.getDefaultSharedPreferences(this)

    private fun loadPrefs() {
        pref().apply {
            userNameEditText.setText(getString("username", ""))
            tokenEditText.setText(getString("password", ""))
            organizationEditText.setText(getString("org", "kotlin"))
            variantSpinner.setSelection(variantOf(getString("variant", "")).ordinal)
        }
    }

    private fun savePrefs() {
        pref().edit {
            putString("username", userNameEditText.text.toString())
            putString("password", tokenEditText.text.toString())
            putString("org", organizationEditText.text.toString())
            putString("variant", selectedVariant().name)
        }
    }

    override fun onDestroy() {
        job.cancel()
        savePrefs()
        super.onDestroy()
    }
}

private class Adapter : ListAdapter<User, ViewHolder>(
    object : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.login == newItem.login
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.create(LayoutInflater.from(parent.context), parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    fun bind(user: User) {
        itemView.textView1.text = user.login
        itemView.textView2.text = user.contributions.toString()
    }

    companion object {
        private const val LAYOUT_ID = R.layout.list_item

        fun create(inflater: LayoutInflater, parent: ViewGroup): ViewHolder {
            return ViewHolder(inflater.inflate(LAYOUT_ID, parent, false))
        }
    }
}

enum class Variant {
    BLOCKING,    // Request1Blocking
    BACKGROUND,  // Request2Background
    CALLBACKS,   // Request3Callbacks
    COROUTINE,   // Request4Coroutine
    PROGRESS,    // Request5Progress
    CANCELLABLE, // Request5Progress (too)
    CONCURRENT,  // Request6Concurrent
    FUTURE,      // Request7Future
    GATHER,      // Request8Gather
    ACTOR        // Request9Actor
}

fun variantOf(str: String): Variant = try {
    Variant.valueOf(str)
} catch (e: IllegalArgumentException) {
    Variant.values()[0]
}
