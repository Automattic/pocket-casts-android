package au.com.shiftyjelly.pocketcasts.search

import android.util.Log
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.ImprovedSearchResultItem
import au.com.shiftyjelly.pocketcasts.models.to.SearchAutoCompleteItem
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.search.ImprovedSearchManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.servers.ServiceManager
import au.com.shiftyjelly.pocketcasts.servers.discover.GlobalServerSearch
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber

class SearchHandler @Inject constructor(
    val serviceManager: ServiceManager,
    val podcastManager: PodcastManager,
    val improvedSearchManager: ImprovedSearchManager,
    val userManager: UserManager,
    val settings: Settings,
    private val cacheServiceManager: PodcastCacheServiceManager,
    private val analyticsTracker: AnalyticsTracker,
    folderManager: FolderManager,
) {
    private var source: SourceView = SourceView.UNKNOWN
    private val searchQuery = BehaviorRelay.create<Query>().apply {
        accept(Query.SearchResults(""))
    }

    private val loadingObservable = BehaviorRelay.create<Boolean>().apply {
        accept(false)
    }
    private val onlySearchRemoteObservable = BehaviorRelay.create<Boolean>().apply {
        accept(false)
    }
    private val resultsFilterObservable = BehaviorRelay.create<ResultsFilters>().apply {
        accept(ResultsFilters.TOP_RESULTS)
    }
    private val signInStateObservable = userManager.getSignInState().startWith(SignInState.SignedOut).toObservable()

    private val localPodcastsResults = Observable
        .combineLatest(searchQuery.filter { it is Query.SearchResults }, onlySearchRemoteObservable, signInStateObservable) { searchQuery, onlySearchRemoteObservable, signInState ->
            Pair(if (onlySearchRemoteObservable) "" else searchQuery.term, signInState)
        }
        .subscribeOn(Schedulers.io())
        .switchMap { (query, signInState) ->
            if (query.isEmpty()) {
                Observable.just(emptyList())
            } else {
                // search folders
                val folderSearch =
                    if (signInState.isSignedInAsPlusOrPatron) {
                        // only show folders if the user has Plus
                        folderManager.findFoldersSingle()
                            .subscribeOn(Schedulers.io())
                            .flatMapObservable { Observable.fromIterable(it) }
                            .filter { it.name.contains(query, ignoreCase = true) }
                            .switchMapSingle { folder ->
                                podcastManager
                                    .findPodcastsInFolderRxSingle(folderUuid = folder.uuid)
                                    .map { podcasts -> FolderItem.Folder(folder = folder, podcasts = podcasts) }
                            }
                            .toList()
                    } else {
                        Single.just(emptyList())
                    }

                // search podcasts
                val podcastSearch = podcastManager.findSubscribedRxSingle()
                    .subscribeOn(Schedulers.io())
                    .flatMapObservable { Observable.fromIterable(it) }
                    .filter { it.title.contains(query, ignoreCase = true) || it.author.contains(query, ignoreCase = true) }
                    .map { podcast ->
                        podcast.isSubscribed = true
                        FolderItem.Podcast(podcast)
                    }
                    .toList()

                podcastSearch
                    .zipWith(folderSearch) { podcasts, folders ->
                        (podcasts + folders).sortedBy { PodcastsSortType.cleanStringForSort(it.title) }
                    }
                    .toObservable()
            }
        }

    private val subscribedPodcastUuids = podcastManager
        .findSubscribedRxSingle()
        .subscribeOn(Schedulers.io())
        .map { podcasts -> podcasts.map(Podcast::uuid).toHashSet() }
        .toObservable()

    @OptIn(ExperimentalCoroutinesApi::class)
    private val autoCompleteResults = searchQuery.filter { it is Query.Suggestions }.asFlow()
        .map { it.term.trim() }
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(
                    SearchUiState.SearchOperation.Success(
                        searchTerm = query,
                        results = emptyList(),
                    ),
                )
            } else {
                flow { emit(improvedSearchManager.autoCompleteSearch(term = query)) }
                    .map<List<SearchAutoCompleteItem>, SearchUiState.SearchOperation<List<SearchAutoCompleteItem>>> {
                        SearchUiState.SearchOperation.Success(
                            searchTerm = query,
                            results = it,
                        )
                    }.catch {
                        emit(
                            SearchUiState.SearchOperation.Error(
                                searchTerm = query,
                                error = it,
                            ),
                        )
                    }
                    .onStart {
                        emit(SearchUiState.SearchOperation.Loading(query))
                    }
            }
        }

    val searchSuggestions = combine(
        autoCompleteResults,
        subscribedPodcastUuids.asFlow(),
    ) { autoComplete, subscribedUuids ->
        when (autoComplete) {
            is SearchUiState.SearchOperation.Success -> {
                autoComplete.copy(
                    results = autoComplete.results.map {
                        when (it) {
                            is SearchAutoCompleteItem.Podcast -> {
                                it.copy(isSubscribed = subscribedUuids.contains(it.uuid))
                            }

                            else -> it
                        }
                    },
                )
            }

            else -> autoComplete
        }
    }

    private val serverSearchResults = searchQuery
        .filter { it is Query.SearchResults }
        .subscribeOn(Schedulers.io())
        .map { (it as Query.SearchResults).copy(term = it.term.trim()) }
        .debounce {
            val debounceQuery = it.term.isNotEmpty() && !it.immediate
            if (debounceQuery) {
                val debounceMs = settings.getPodcastSearchDebounceMs()
                Observable.timer(debounceMs, TimeUnit.MILLISECONDS)
            } else {
                Observable.empty()
            }
        }
        .map { it.term }
        .switchMap {
            if (it.length <= 1) {
                Observable.just(GlobalServerSearch())
            } else {
                analyticsTracker.track(AnalyticsEvent.SEARCH_PERFORMED, AnalyticsProp.sourceMap(source))
                loadingObservable.accept(true)

                var globalSearch = GlobalServerSearch(searchTerm = it)
                val podcastServerSearch = serviceManager
                    .searchForPodcastsRx(it)
                    .map { podcastSearch ->
                        globalSearch = globalSearch.copy(podcastSearch = podcastSearch)
                        globalSearch
                    }
                    .toObservable()

                if (!it.startsWith("http")) {
                    val episodesServerSearch = cacheServiceManager
                        .searchEpisodes(it)
                        .map { episodeSearch ->
                            globalSearch = globalSearch.copy(episodeSearch = episodeSearch)
                            globalSearch
                        }

                    podcastServerSearch.mergeWith(episodesServerSearch)
                } else {
                    podcastServerSearch
                }
                    .subscribeOn(Schedulers.io())
                    .onErrorReturn { exception ->
                        GlobalServerSearch(error = exception)
                    }
                    .doFinally {
                        loadingObservable.accept(false)
                    }
            }
        }

    private val searchFlowable = Observables.combineLatest(searchQuery.filter { it is Query.SearchResults }, subscribedPodcastUuids, localPodcastsResults, serverSearchResults, loadingObservable) { searchTerm, subscribedPodcastUuids, localPodcastsResult, serverSearchResults, loading ->
        if (loading) {
            SearchUiState.SearchOperation.Loading(searchTerm.term)
        } else if (serverSearchResults.error != null) {
            analyticsTracker.track(AnalyticsEvent.SEARCH_FAILED, AnalyticsProp.sourceMap(source))
            SearchUiState.SearchOperation.Error(searchTerm = searchTerm.term, error = serverSearchResults.error!!)
        } else if (searchTerm.term.isBlank()) {
            SearchUiState.SearchOperation.Success(searchTerm = searchTerm.term, results = SearchResults.SegregatedResults(podcasts = emptyList(), episodes = emptyList()))
        } else {
            // set if the podcast is subscribed so we can show a tick
            val serverPodcastsResult = serverSearchResults.podcastSearch.searchResults.map { podcast -> FolderItem.Podcast(podcast) }
            serverPodcastsResult.forEach {
                if (subscribedPodcastUuids.contains(it.podcast.uuid)) {
                    it.podcast.isSubscribed = true
                }
            }
            val searchPodcastsResult = (localPodcastsResult + serverPodcastsResult).distinctBy { it.uuid }
            val searchEpisodesResult = serverSearchResults.episodeSearch.episodes

            SearchUiState.SearchOperation.Success(
                searchTerm = searchTerm.term,
                results = SearchResults.SegregatedResults(
                    podcasts = searchPodcastsResult,
                    episodes = searchEpisodesResult,
                ),
            )
        }
    }
        .doOnError { Timber.e(it) }
        .onErrorReturn { exception ->
            analyticsTracker.track(AnalyticsEvent.SEARCH_FAILED, AnalyticsProp.sourceMap(source))
            SearchUiState.SearchOperation.Error(
                searchTerm = searchQuery.value?.term.orEmpty(),
                error = exception,
            )
        }
        .observeOn(AndroidSchedulers.mainThread())

    val searchResults = searchFlowable.asFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val improvedSearchResults: Flow<SearchUiState.SearchOperation<SearchResults.ImprovedResults>> = combine(
        searchQuery.filter { it is Query.SearchResults }.map { it.term.trim() }.asFlow(),
        subscribedPodcastUuids.asFlow(),
        localPodcastsResults.asFlow(),
    ) { query, subscribedPodcastUuids, localPodcasts -> Triple(query, subscribedPodcastUuids, localPodcasts) }
        .flatMapLatest { (query, subscribedUuids, localPodcasts) ->
            if (query.isBlank()) {
                flowOf(SearchUiState.SearchOperation.Success(searchTerm = query, results = SearchResults.ImprovedResults(emptyList())))
            } else {
                flow {
                    emit(SearchUiState.SearchOperation.Loading(searchTerm = query))
                    val apiResults = improvedSearchManager.combinedSearch(query)
                    val localResults = localPodcasts.map {
                        when (it) {
                            is FolderItem.Folder -> ImprovedSearchResultItem.FolderItem(folder = it.folder, podcasts = it.podcasts)
                            is FolderItem.Podcast -> ImprovedSearchResultItem.PodcastItem(uuid = it.uuid, isFollowed = subscribedUuids.contains(it.uuid), title = it.podcast.title, author = it.podcast.author)
                        }
                    }
                    val combinedResults = apiResults + localResults
                    emit(SearchUiState.SearchOperation.Success(searchTerm = query, results = SearchResults.ImprovedResults(combinedResults)))
                }.catch {
                    Log.d("===", "error $it")
                    emit(SearchUiState.SearchOperation.Error(searchTerm = query, error = it) as SearchUiState.SearchOperation<SearchResults.ImprovedResults>)
                }
            }
        }

    fun updateAutCompleteQuery(query: String) {
        searchQuery.accept(Query.Suggestions(query))
    }

    fun updateSearchQuery(query: String, immediate: Boolean = false) {
        searchQuery.accept(Query.SearchResults(query, immediate))
    }

    fun setOnlySearchRemote(remote: Boolean) {
        onlySearchRemoteObservable.accept(remote)
    }

    fun setSource(source: SourceView) {
        this.source = source
    }

    private sealed interface Query {
        val term: String

        data class Suggestions(override val term: String) : Query
        data class SearchResults(override val term: String, val immediate: Boolean = false) : Query
    }

    private object AnalyticsProp {
        private const val SOURCE = "source"
        fun sourceMap(source: SourceView) = mapOf(SOURCE to source.analyticsValue)
    }
}
