package au.com.shiftyjelly.pocketcasts.repositories.sync;

// TODO uncomment or remove this test
public class PodcastSyncProcessTest {

//    @Mock Notifications notifications;
//    @Mock Settings settings;
//    @Mock PodcastManager podcastManager;
//    @Mock EpisodeManager episodeManager;
//    @Mock PlaylistManager playlistManager;
//    @Mock PlaybackManager playbackManager;
//    @Mock StatsManager statsManager;
//
//    @Before
//    public void setUp() throws Exception {
//
//    }
//
//    @After
//    public void tearDown() throws Exception {
//
//    }
//
//    /**
//     * If the user isn't logged in just delete the podcasts and playlists that were marked to be deleted.
//     */
//    @Test
//    public void testPerformSyncNotLoggedIn() throws Exception {
//        when(settings.isLoggedIn()).thenReturn(false);
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(settings, podcastManager, episodeManager, playlistManager, notifications, statsManager, null, playbackManager);
//        syncProcess.performSync();
//
//        verify(podcastManager).deleteSynced(playbackManager);
//        verify(playlistManager).deleteSynced();
//    }
//
//    @Test
//    public void testPerformSync() throws Exception {
//        when(settings.isLoggedIn()).thenReturn(false);
//
//        Tests.logExceptions();
//
//        PodcastSyncProcess.ServerDelegate serverDelegate = new SyncServerDelegateTestImpl();
//
//        Podcast podcast = new Podcast();
//        podcast.setUuid("801ee640-04d4-0134-9cb2-59d98c6b72b8");
//        podcast.setDeleted(true);
//        podcast.setSubscribed(true);
//        podcast.setStartFromSecs(0);
//
//        List<Podcast> podcasts = new ArrayList<>();
//        podcasts.add(podcast);
//        when(podcastManager.findPodcastsToSync()).thenReturn(podcasts);
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(settings, podcastManager, episodeManager, playlistManager, notifications, statsManager, serverDelegate, null);
//        syncProcess.performSync();
//
//        verify(notifications, never()).broadcast(eq(NotificationType.SYNC_FAILED));
//    }
//
//    @Test
//    public void uploadPodcastChanges() throws Exception {
//        Podcast podcast = new Podcast();
//        podcast.setUuid("801ee640-04d4-0134-9cb2-59d98c6b72b8");
//        podcast.setDeleted(true);
//        podcast.setSubscribed(true);
//        podcast.setStartFromSecs(0);
//
//        Podcast podcast2 = new Podcast();
//        podcast2.setUuid("453ee640-04d4-0134-9cb2-59d98c6b76c1");
//        podcast2.setDeleted(false);
//        podcast2.setSubscribed(false);
//        podcast2.setStartFromSecs(30);
//
//
//        List<Podcast> podcasts = new ArrayList<>();
//        podcasts.add(podcast);
//        podcasts.add(podcast2);
//        when(podcastManager.findPodcastsToSync()).thenReturn(podcasts);
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, podcastManager, null, null, null, null, null, null);
//
//        JSONArray output = new JSONArray();
//        syncProcess.uploadPodcastChanges(output);
//
//        assertThat(output.length(), is(2));
//        assertThat(output.toString(), is(equalTo("["+
//            "{\"fields\":{\"subscribed\":\"1\",\"is_deleted\":\"1\",\"uuid\":\"801ee640-04d4-0134-9cb2-59d98c6b72b8\",\"auto_start_from\":0},\"type\":\"UserPodcast\"},"+
//            "{\"fields\":{\"subscribed\":\"0\",\"is_deleted\":\"0\",\"uuid\":\"453ee640-04d4-0134-9cb2-59d98c6b76c1\",\"auto_start_from\":30},\"type\":\"UserPodcast\"}"+
//            "]")));
//    }
//
//    @Test
//    public void uploadEpisodeChanges() throws Exception {
//        Tests.logExceptions();
//
//        Episode episode = new Episode();
//        episode.setUuid("134b3690-1f18-0134-a45d-13e6b3913b15");
//        episode.setPlayingStatus(EpisodePlayingStatus.IN_PROGRESS);
//        episode.setPlayingStatusModified(1467091351392l);
//        episode.setPlayedUpTo(197.916d);
//        episode.setPlayedUpToModified(1467091359785l);
//        episode.setPodcastUuid("da3271a0-69e7-0132-d9fd-5f4c86fd3263");
//        episode.setDuration(543.12d);
//        episode.setDurationModified(1054091359785l);
//
//        Episode episodeCompleted = new Episode();
//        episodeCompleted.setUuid("654b3690-1f18-0134-b45d-13e6b3913e50");
//        episodeCompleted.setPlayingStatus(EpisodePlayingStatus.COMPLETED);
//        episodeCompleted.setPlayingStatusModified(3467091351392l);
//        episodeCompleted.setPlayedUpTo(37.932d);
//        episodeCompleted.setPlayedUpToModified(1367091359775l);
//        episodeCompleted.setPodcastUuid("ca6311a0-69e7-0132-d9fd-5f4c86fd3111");
//        episodeCompleted.setStarred(true);
//        episodeCompleted.setStarredModified(1127091359775l);
//
//        Episode episodeDeleted = new Episode();
//        episodeDeleted.setUuid("b83764d0-1f58-0134-a45d-13e6b3913b15");
//        episodeDeleted.setDeleted(true);
//        episodeDeleted.setIsDeletedModified(1267091359775l);
//        episodeDeleted.setPlayingStatus(EpisodePlayingStatus.NOT_PLAYED);
//        episodeDeleted.setPlayingStatusModified(3167091351292l);
//        episodeDeleted.setPlayedUpTo(0d);
//        episodeDeleted.setPlayedUpToModified(1147091359775l);
//        episodeDeleted.setPodcastUuid("7480c620-b40d-0130-28ba-723c91aeae46");
//
//        doAnswer(invocation -> {
//            RowParser<Episode> rowParser = (RowParser<Episode>)invocation.getArguments()[0];
//            rowParser.parse(episode);
//            rowParser.parse(episodeCompleted);
//            rowParser.parse(episodeDeleted);
//            return null;
//
//        }).when(episodeManager).findEpisodesToSync(any(RowParser.class));
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, episodeManager, null, null, null, null, null);
//
//        JSONArray output = new JSONArray();
//        syncProcess.uploadEpisodesChanges(output);
//
//        assertThat(output.length(), is(3));
//        assertThat(output.toString(), is(equalTo("["+
//            "{\"fields\":{" +
//                "\"duration\":543.12," +
//                "\"user_podcast_uuid\":\"da3271a0-69e7-0132-d9fd-5f4c86fd3263\"," +
//                "\"played_up_to\":197.916," +
//                "\"playing_status\":2," +
//                "\"duration_modified\":1054091359785," +
//                "\"playing_status_modified\":1467091351392," +
//                "\"uuid\":\"134b3690-1f18-0134-a45d-13e6b3913b15\"," +
//                "\"played_up_to_modified\":1467091359785" +
//                "},\"type\":\"UserEpisode\"},"+
//            "{\"fields\":{" +
//                "\"user_podcast_uuid\":\"ca6311a0-69e7-0132-d9fd-5f4c86fd3111\"," +
//                "\"starred\":\"1\"," +
//                "\"played_up_to\":37.932," +
//                "\"playing_status\":3," +
//                "\"starred_modified\":1127091359775," +
//                "\"playing_status_modified\":3467091351392," +
//                "\"uuid\":\"654b3690-1f18-0134-b45d-13e6b3913e50\"," +
//                "\"played_up_to_modified\":1367091359775" +
//                "},\"type\":\"UserEpisode\"},"+
//            "{\"fields\":{" +
//                "\"user_podcast_uuid\":\"7480c620-b40d-0130-28ba-723c91aeae46\"," +
//                "\"is_deleted\":\"1\"," +
//                "\"played_up_to\":0," +
//                "\"is_deleted_modified\":1267091359775," +
//                "\"playing_status\":1," +
//                "\"playing_status_modified\":3167091351292," +
//                "\"uuid\":\"b83764d0-1f58-0134-a45d-13e6b3913b15\"," +
//                "\"played_up_to_modified\":1147091359775" +
//                "},\"type\":\"UserEpisode\"}"+
//            "]")));
//    }
//
//    @Test
//    public void uploadPlaylistChanges() throws Exception {
//        Playlist playlist = new Playlist();
//        playlist.setUuid("55501bbf-74f2-4f64-8d8f-38ee7816144d");
//        playlist.setDeleted(false);
//        playlist.setTitle("Downloaded");
//        playlist.setAllPodcasts(true);
//        playlist.setAudioVideo(Playlist.AUDIO_VIDEO_FILTER_ALL);
//        playlist.setNotDownloaded(false);
//        playlist.setDownloaded(true);
//        playlist.setDownloading(true);
//        playlist.setFinished(false);
//        playlist.setPartiallyPlayed(true);
//        playlist.setUnplayed(true);
//        playlist.setStarred(false);
//        playlist.setManual(false);
//        playlist.setSortPosition(4);
//        playlist.setSortId(0);
//        playlist.setIconId(0);
//        playlist.setFilterHours(0);
//
//        List<Playlist> playlists = new ArrayList<>();
//        playlists.add(playlist);
//        when(playlistManager.findPlaylistsToSync()).thenReturn(playlists);
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, null, playlistManager, null, null, null, null);
//
//        JSONArray output = new JSONArray();
//        syncProcess.uploadPlaylistChanges(output);
//
//        assertThat(output.length(), is(1));
//        assertThat(output.toString(), is(equalTo("["+
//                "{\"fields\":{" +
//                    "\"all_podcasts\":\"1\"," +
//                    "\"finished\":\"0\"," +
//                    "\"title\":\"Downloaded\"," +
//                    "\"manual\":\"0\"," +
//                    "\"icon_id\":0," +
//                    "\"uuid\":\"55501bbf-74f2-4f64-8d8f-38ee7816144d\"," +
//                    "\"downloaded\":\"1\"," +
//                    "\"sort_type\":0," +
//                    "\"filter_hours\":0," +
//                    "\"is_deleted\":\"0\"," +
//                    "\"sort_position\":4," +
//                    "\"starred\":\"0\"," +
//                    "\"downloading\":\"1\"," +
//                    "\"partially_played\":\"1\"," +
//                    "\"not_downloaded\":\"0\"," +
//                    "\"unplayed\":\"1\"," +
//                    "\"audio_video\":0" +
//                "}," +
//                "\"type\":\"UserPlaylist\"}"+
//                "]")));
//    }
//
//    /**
//     * Check the server data returned gets processed.
//     */
//    @Test
//    public void processServerData() throws Exception {
//        String resultJson = "{\"changes\":[],\"last_modified\":\"2016-07-06 03:10:55\"}";
//
//        PlaybackManager playbackManager = mock(PlaybackManager.class);
//        when(playbackManager.isPlaying()).thenReturn(false);
//        when(playbackManager.isBuffering()).thenReturn(false);
//        when(playbackManager.getCurrentEpisode()).thenReturn(null);
//
//        Settings settings = mock(Settings.class);
//        when(settings.shouldDeleteDownloadWhenPlayed()).thenReturn(false);
//
//        Notifications notifications = mock(Notifications.class);
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(settings, null, null, null, notifications, null, null, playbackManager);
//        syncProcess.processServerData(resultJson);
//    }
//
//    /**
//     * Check when reading the podcasts from the server and the uuid is empty.
//     */
//    @Test
//    public void processServerDataPodcastEmpty() throws Exception {
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, null, null, null, null, null, null);
//        boolean importResult = syncProcess.importPodcast(new JSONObject("{\"type\":\"UserPodcast\",\"fields\":{\"uuid\":\"\"}}"));
//        assertThat("If a podcast uuid is empty then just ignore it.", importResult, is(true));
//    }
//
//    @Test
//    public void processServerDataPodcastDeleted() throws Exception {
//        String podcastUuid = "5cda9490-4117-012e-1622-00163e1b201c";
//        Podcast podcast = new Podcast();
//        podcast.setUuid(podcastUuid);
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, podcastManager, null, null, null, null, null, playbackManager);
//        boolean importResult = syncProcess.importPodcast(new JSONObject("{\"type\":\"UserPodcast\",\"fields\":{\"uuid\":\""+podcastUuid+"\",\"is_deleted\":true,\"subscribed\":true,\"auto_start_from\":0,\"episodes_sort_order\":null}}"));
//        verify(podcastManager, never()).deleteSynced(eq(podcast), eq(playbackManager));
//        assertThat("After a sync podcasts if it doesn't exist just carry on.", importResult, is(true));
//
//        when(podcastManager.findPodcastByUuid(podcastUuid)).thenReturn(podcast);
//
//        syncProcess.importPodcast(new JSONObject("{\"type\":\"UserPodcast\",\"fields\":{\"uuid\":\""+podcastUuid+"\",\"is_deleted\":true,\"subscribed\":true,\"auto_start_from\":0,\"episodes_sort_order\":null}}"));
//        verify(podcastManager).deleteSynced(eq(podcast), eq(playbackManager));
//    }
//
//    @Test
//    public void processServerDataPodcastUpdated() throws Exception {
//        String podcastUuid = "5cda9490-4117-012e-1622-00163e1b201c";
//        Podcast podcast = new Podcast();
//        podcast.setUuid(podcastUuid);
//        when(podcastManager.findPodcastByUuid(podcastUuid)).thenReturn(podcast);
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, podcastManager, null, null, null, null, null, null);
//        syncProcess.importPodcast(new JSONObject("{\"type\":\"UserPodcast\",\"fields\":{\"uuid\":\""+podcastUuid+"\",\"is_deleted\":false,\"subscribed\":true,\"auto_start_from\":30,\"episodes_sort_order\":null}}"));
//        verify(podcastManager).markPodcastAsSynced(eq(podcast));
//    }
//
//    @Test
//    public void processServerDataPodcastNew() throws Exception {
//        String podcastUuid = "5cda9490-4117-012e-1622-00163e1b201c";
//        PodcastManager podcastManager = mock(PodcastManager.class);
//        PodcastSyncProcess.ServerDelegate serverDelegate = mock(PodcastSyncProcess.ServerDelegate.class);
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, podcastManager, null, null, null, null, serverDelegate, null);
//        syncProcess.importPodcast(new JSONObject("{\"type\":\"UserPodcast\",\"fields\":{\"uuid\":\""+podcastUuid+"\",\"is_deleted\":false,\"subscribed\":true,\"auto_start_from\":30,\"episodes_sort_order\":null}}"));
//        //assertThat("After a sync existing podcasts should be added.", podcastManager.markPodcastAsSyncedWasFired(podcastUuid), is(true));
//    }
//
//    @Test
//    public void processServerDataEpisodeEmpty() throws Exception {
//        SyncServerDelegateTestImpl server = new SyncServerDelegateTestImpl();
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, null, null, null, null, server, null);
//        syncProcess.importEpisode(
//            new JSONObject("{\"type\":\"UserEpisode\",\"fields\":{\"uuid\":\"\",\"playing_status\":2,\"played_up_to\":197,\"is_deleted\":false,\"duration\":null,\"starred\":false}}"),
//            true,
//            null,
//            false
//        );
//    }
//
//    @Test
//    public void processServerDataEpisodeDeleted() throws Exception {
//        Episode episode = new Episode();
//        String episodeUuid = "5cda9490-4117-012e-1622-00163e1b201c";
//        when(episodeManager.findByUuid(episodeUuid)).thenReturn(episode);
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, episodeManager, null, null, null, null, playbackManager);
//        syncProcess.importEpisode(
//                new JSONObject("{\"type\":\"UserEpisode\",\"fields\":{\"uuid\":\""+episodeUuid+"\",\"is_deleted\":true,\"playing_status\":2,\"played_up_to\":197,\"duration\":null,\"starred\":false}}"),
//                true,
//                null,
//                false
//        );
//        verify(episodeManager).deleteEpisodeWithoutSync(eq(episode), eq(playbackManager), eq(false));
//    }
//
//    @Test
//    public void processServerDataEpisodeUpdate() throws Exception {
//        String episodeUuid = "5cda9490-4117-012e-1622-00163e1b201c";
//        Episode episode = new Episode();
//        episode.setUuid(episodeUuid);
//        // check the modified fields get cleared
//        episode.setStarredModified(System.currentTimeMillis());
//        episode.setIsDeletedModified(System.currentTimeMillis());
//        episode.setPlayedUpToModified(System.currentTimeMillis());
//        episode.setPlayingStatusModified(System.currentTimeMillis());
//        episode.setDurationModified(System.currentTimeMillis());
//
//        when(episodeManager.findByUuid(episodeUuid)).thenReturn(episode);
//
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, episodeManager, null, notifications, null, null, null);
//        syncProcess.importEpisode(
//                new JSONObject("{\"type\":\"UserEpisode\",\"fields\":{\"uuid\":\""+episodeUuid+"\",\"is_deleted\":false,\"playing_status\":2,\"played_up_to\":197,\"duration\":100,\"starred\":true}}"),
//                true,
//                null,
//                false
//        );
//
//        Episode updatedEpisode = episodeManager.findByUuid(episodeUuid);
//        assertThat(updatedEpisode, is(notNullValue()));
//        assertThat(updatedEpisode.isStarred(), is(true));
//        assertThat(updatedEpisode.getPlayingStatus(), is(EpisodePlayingStatus.IN_PROGRESS));
//        assertThat(updatedEpisode.getDurationMs(), is(100000));
//
//        verify(notifications).broadcastCurrentTimeChangedOnSync(eq(episodeUuid), eq(197000));
//    }
//
//    @Test
//    public void processServerDataPlaylistUpdate() throws Exception {
//        Playlist playlist = new Playlist();
//        String playlistUuid = "5cda9490-4117-012e-1622-00163e1b201c";
//        when(playlistManager.findByUuid(playlistUuid)).thenReturn(playlist);
//        PodcastSyncProcess syncProcess = new PodcastSyncProcess(null, null, null, playlistManager, notifications, null, null, null);
//
//        syncProcess.importPlaylist(new JSONObject("{\"type\":\"UserPlaylist\",\"fields\":{\"uuid\":\""+playlistUuid+"\",\"title\":\"Downloaded\",\"auto_download\":null,\"auto_download_wifi_only\":null,\"auto_download_power_only\":null,\"all_podcasts\":true,\"podcast_uuids\":null,\"audio_video\":1,\"not_downloaded\":false,\"downloaded\":true,\"downloading\":true,\"finished\":false,\"partially_played\":true,\"unplayed\":true,\"starred\":true,\"manual\":false,\"episode_uuids\":null,\"sort_position\":4,\"sort_type\":0,\"icon_id\":2,\"is_deleted\":false,\"filter_hours\":0}}"));
//        assertThat(playlist, is(notNullValue()));
//        assertThat(playlist.isStarred(), is(true));
//        assertThat(playlist.getTitle(), is(equalTo("Downloaded")));
//        assertThat(playlist.isAllPodcasts(), is(true));
//        assertThat(playlist.getAudioVideo(), is(1));
//        assertThat(playlist.isNotDownloaded(), is(false));
//        assertThat(playlist.isDownloaded(), is(true));
//        assertThat(playlist.isDownloading(), is(true));
//        assertThat(playlist.isFinished(), is(false));
//        assertThat(playlist.isPartiallyPlayed(), is(true));
//        assertThat(playlist.isUnplayed(), is(true));
//        assertThat(playlist.isManual(), is(false));
//        assertThat(playlist.getSortPosition(), is(equalTo(4)));
//        assertThat(playlist.getIconId(), is(2));
//
//        verify(notifications).broadcast(eq(NotificationType.PLAYLIST_CHANGED));
//    }

}