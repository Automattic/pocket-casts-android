package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.adapter.FilterAutoDownloadAdapter
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class AutoDownloadFiltersFragment :
    androidx.fragment.app.Fragment(),
    FilterAutoDownloadAdapter.ClickListener {

    @Inject lateinit var smartPlaylistManager: SmartPlaylistManager

    @Inject lateinit var theme: Theme

    @Inject lateinit var settings: Settings

    private val filters = mutableListOf<PlaylistEntity>()
    private val adapter = FilterAutoDownloadAdapter(filters, this, theme.isDarkTheme)
    private val disposables = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadFilters()
    }

    override fun onDestroy() {
        super.onDestroy()
        disposables.clear()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val recyclerView = inflater.inflate(VR.layout.fragment_recyclerview, container, false) as androidx.recyclerview.widget.RecyclerView
        recyclerView.setHasFixedSize(true)
        recyclerView.adapter = adapter

        val columns = resources.getInteger(UR.integer.podcast_list_column_num)
        val layoutManager = androidx.recyclerview.widget.GridLayoutManager(activity, columns)
        recyclerView.layoutManager = layoutManager

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                settings.bottomInset.collect {
                    recyclerView.updatePadding(bottom = it)
                }
            }
        }

        return recyclerView
    }

    private fun loadFilters() {
        smartPlaylistManager.findAllRxFlowable()
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableSingleObserver<List<PlaylistEntity>>() {
                override fun onSuccess(list: List<PlaylistEntity>) {
                    filters.clear()
                    filters.addAll(list)
                    adapter.notifyDataSetChanged()
                }

                override fun onError(throwable: Throwable) {
                    Timber.e(throwable)
                }
            }).addTo(disposables)
    }

    override fun onAutoDownloadChanged(filter: PlaylistEntity, on: Boolean) {
        smartPlaylistManager.updateAutoDownloadStatusRxCompletable(filter, on)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableCompletableObserver() {
                override fun onComplete() {
                    Timber.i("Playlist updated")
                }

                override fun onError(throwable: Throwable) {
                    Timber.e(throwable)
                }
            })
            .addTo(disposables)
    }
}
