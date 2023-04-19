package au.com.shiftyjelly.pocketcasts

// TODO: Uncomment and fix these tests once the service is migrated properly to MediaLibraryService
// @RunWith(AndroidJUnit4::class)
class AutoPlaybackServiceTest {
    /*@get:Rule
    val serviceRule = ServiceTestRule()

    private lateinit var service: AutoPlaybackService

    @Before
    fun setup() {
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            AutoPlaybackService::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        service = (binder as PlaybackService.LocalBinder).service as AutoPlaybackService
    }


    @Test
    @Throws(TimeoutException::class)
    fun testReturnsCorrectTabs() {
        runBlocking {
            val children = service.loadRootChildren()
            assertEquals("There are 3 tabs", 3, children.size)
            assertEquals("The first tab should be discover", DISCOVER_ROOT, children[0].mediaId)
            assertEquals("The second tab should be podcasts", PODCASTS_ROOT, children[1].mediaId)
            assertEquals("The third tab should be episode filters", FILTERS_ROOT, children[2].mediaId)
        }
    }

    @Test
    fun testLoadDiscover() {
        runBlocking {
            val discover = service.loadDiscoverRoot()
            assertTrue("Discover should have content", discover.isNotEmpty())
        }
    }

    @Test
    fun testLoadFilters() {
        runBlocking {
            val playlist = Playlist(uuid = UUID.randomUUID().toString(), title = "Test title", iconId = 0)
            service.playlistManager = mock { on { findAll() }.doReturn(listOf(playlist)) }

            val filtersRoot = service.loadFiltersRoot()
            assertTrue("Filters should not be empty", filtersRoot.isNotEmpty())
            assertTrue("Filter uuid should be equal", filtersRoot[0].mediaId == playlist.uuid)
            assertTrue("Filter title should be correct", filtersRoot[0].mediaMetadata.title == playlist.title)
            assertTrue("Filter should have an icon", filtersRoot[0].mediaMetadata.artworkUri != null)
        }
    }

    @Test
    fun testLoadPodcasts() {
        val podcast = Podcast(UUID.randomUUID().toString(), title = "Test podcast")
        val podcastManager = mock<PodcastManager> { on { runBlocking { findSubscribedSorted() } }.doReturn(listOf(podcast)) }
        service.podcastManager = podcastManager
        val subscriptionManager = mock<SubscriptionManager> { on { getCachedStatus() }.doReturn(SubscriptionStatus.Free()) }
        service.subscriptionManager = subscriptionManager

        runBlocking {
            val podcastsRoot = service.loadPodcastsChildren()
            assertTrue("Podcasts should not be empty", podcastsRoot.isNotEmpty())
            assertTrue("Podcast uuid should be equal", podcastsRoot[0].mediaId == podcast.uuid)
            assertTrue("Podcast title should be correct", podcastsRoot[0].mediaMetadata.title == podcast.title)
        }
    }

    @Test
    fun testLoadPodcastEpisodes() {
        runBlocking {
            val podcast = Podcast(UUID.randomUUID().toString(), title = "Test podcast")
            val episode = Episode(
                UUID.randomUUID().toString(),
                title = "Test episode",
                publishedDate = Date()
            )

            service.librarySessionCallback.playlistManager = mock { on { findByUuid(any()) }.doReturn(null) }
            service.librarySessionCallback.podcastManager =
                mock { on { runBlocking { findPodcastByUuidSuspend(any()) } }.doReturn(podcast) }
            service.librarySessionCallback.episodeManager =
                mock { on { findEpisodesByPodcastOrdered(any()) }.doReturn(listOf(episode)) }

            val episodes = (service.librarySessionCallback as AutoPlaybackService.AutoMediaLibrarySessionCallback).loadEpisodeChildren(podcast.uuid)
            assertTrue("Episodes should have content", episodes.isNotEmpty())
            assertTrue(
                "Episode uuid should be equal",
                episodes[0].mediaId == AutoMediaId(episode.uuid, podcast.uuid).toMediaId()
            )
            assertTrue(
                "Episode title should be correct",
                episodes[0].mediaMetadata.title == episode.title
            )
        }
    }*/
}
