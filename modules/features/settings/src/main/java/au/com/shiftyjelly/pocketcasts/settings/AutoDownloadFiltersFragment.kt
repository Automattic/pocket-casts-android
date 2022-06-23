package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.adapter.FilterAutoDownloadAdapter
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.observers.DisposableCompletableObserver
import io.reactivex.observers.DisposableSingleObserver
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.ui.R as UR
import au.com.shiftyjelly.pocketcasts.views.R as VR

@AndroidEntryPoint
class AutoDownloadFiltersFragment : androidx.fragment.app.Fragment(), FilterAutoDownloadAdapter.ClickListener {

    @Inject lateinit var playlistManager: PlaylistManager
    @Inject lateinit var theme: Theme

    private val filters = mutableListOf<Playlist>()
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

        return recyclerView
    }

    private fun loadFilters() {
        playlistManager.observeAll()
            .firstOrError()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableSingleObserver<List<Playlist>>() {
                override fun onSuccess(list: List<Playlist>) {
                    filters.clear()
                    filters.addAll(list)
                    adapter.notifyDataSetChanged()
                }

                override fun onError(throwable: Throwable) {
                    Timber.e(throwable)
                }
            }).addTo(disposables)
    }

    override fun onAutoDownloadChanged(filter: Playlist, on: Boolean) {
        playlistManager.rxUpdateAutoDownloadStatus(filter, on, true, false)
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

    override fun onSettingsClicked(filter: Playlist) {
//        val intent = Intent(activity, PlaylistEditActivity::class.java)
//        intent.putExtra(PlaylistEditActivity.EXTRA_PLAYLIST_ID, filter.id)
//        intent.putExtra(PlaylistEditActivity.EXTRA_PLAYLIST_TITLE, filter.title)
//        activity?.startActivity(intent)
    }
}
