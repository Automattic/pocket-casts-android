package au.com.shiftyjelly.pocketcasts.repositories.podcast;

// TODO clean this up

public class EpisodeManagerTest {
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
//    @Test
//    public void findEpisodesByPodcastWhere() throws Exception {
//        DatabaseManager databaseManager = mock(DatabaseManager.class);
//        EpisodeManagerImpl episodeManager = new EpisodeManagerImpl(null, null, null, null, databaseManager, null, null);
//        Podcast podcast = new Podcast();
//        podcast.setUuid("69b443a0-26dd-0134-2f28-737688e4d168");
//        RowParser<Episode> rowParser = episode -> true;
//
//        // check the query generated is correct without a where query
//        episodeManager.findEpisodesByPodcastWhere(podcast, "", rowParser);
//        verify(databaseManager).findWhere(eq(Episode.class), eq("podcast_id = '69b443a0-26dd-0134-2f28-737688e4d168' AND is_deleted = 0"), Matchers.<RowParser<DatabaseManagerImpl.DatabaseRecord>>any());
//
//        // check the query generated is correct with a where query
//        episodeManager.findEpisodesByPodcastWhere(podcast, "starred = 1", rowParser);
//        verify(databaseManager).findWhere(eq(Episode.class), eq("podcast_id = '69b443a0-26dd-0134-2f28-737688e4d168' AND is_deleted = 0 AND starred = 1"), Matchers.<RowParser<DatabaseManagerImpl.DatabaseRecord>>any());
//    }
//
//    @Test
//    public void findEpisodesByPodcastOrderBy() throws Exception {
//        DatabaseManager databaseManager = mock(DatabaseManager.class);
//        EpisodeManagerImpl episodeManager = new EpisodeManagerImpl(null, null, null, null, databaseManager, null, null);
//        Podcast podcast = new Podcast();
//        podcast.setUuid("69b443a0-26dd-0134-2f28-737688e4d168");
//        RowParser<Episode> rowParser = episode -> true;
//
//        // check the query generated is correct without a where query
//        episodeManager.findEpisodesByPodcastOrderBy(podcast, "", rowParser);
//        verify(databaseManager).findWhere(eq(Episode.class), eq("podcast_id = '69b443a0-26dd-0134-2f28-737688e4d168' AND is_deleted = 0"), Matchers.<RowParser<DatabaseManagerImpl.DatabaseRecord>>any());
//
//        // check the query generated is correct with a where query
//        episodeManager.findEpisodesByPodcastOrderBy(podcast, "published_date DESC", rowParser);
//        verify(databaseManager).findWhere(eq(Episode.class), eq("podcast_id = '69b443a0-26dd-0134-2f28-737688e4d168' AND is_deleted = 0 ORDER BY published_date DESC"), Matchers.<RowParser<DatabaseManagerImpl.DatabaseRecord>>any());
//    }
//
//    @Test
//    public void findEpisodesJoin() throws Exception {
//        DatabaseManager databaseManager = mock(DatabaseManager.class);
//        EpisodeManagerImpl episodeManager = new EpisodeManagerImpl(null, null, null, null, databaseManager, null, null);
//        Podcast podcast = new Podcast();
//        podcast.setUuid("69b443a0-26dd-0134-2f28-737688e4d168");
//
//        // check the query generated is correct without a where query
//        episodeManager.findEpisodesJoin("");
//        verify(databaseManager).findJoin(eq(Episode.class), eq(""));
//
//        // check the query generated is correct with a where query
//        episodeManager.findEpisodesJoin("JOIN playlist_episodes ON episode.uuid = playlist_episodes.episodeUuid WHERE playlist_episodes.playlistId = 72 ORDER BY playlist_episodes.position ASC");
//        verify(databaseManager).findJoin(eq(Episode.class), eq("JOIN playlist_episodes ON episode.uuid = playlist_episodes.episodeUuid WHERE playlist_episodes.playlistId = 72 ORDER BY playlist_episodes.position ASC"));
//    }
//
//    @Test
//    public void findEpisodesByUuids() throws Exception {
//        DatabaseManager databaseManager = mock(DatabaseManager.class);
//        EpisodeManagerImpl episodeManager = new EpisodeManagerImpl(null, null, null, null, databaseManager, null, null);
//        Podcast podcast = new Podcast();
//        podcast.setUuid("69b443a0-26dd-0134-2f28-737688e4d168");
//
//        // check the query generated is correct without a where query
//        episodeManager.findEpisodesByUuids(null, false);
//        verifyNoMoreInteractions(databaseManager);
//
//        // check the query generated is correct with a where query
//        episodeManager.findEpisodesByUuids(new String[] { "67090710-26db-0134-2f28-737688e4d168", "416ed060-26da-0134-2f28-737688e4d168", "3bb47210-26d2-0134-2f28-737688e4d168" }, true);
//        verify(databaseManager).findFirstWhere(eq(Episode.class), eq("uuid = ?"), eq(new String[] { "67090710-26db-0134-2f28-737688e4d168" }));
//        verify(databaseManager).findFirstWhere(eq(Episode.class), eq("uuid = ?"), eq(new String[] { "416ed060-26da-0134-2f28-737688e4d168" }));
//        verify(databaseManager).findFirstWhere(eq(Episode.class), eq("uuid = ?"), eq(new String[] { "3bb47210-26d2-0134-2f28-737688e4d168" }));
//
//        episodeManager.findEpisodesByUuids(new String[] { "67090710-26db-0134-2f28-737688e4d168", "416ed060-26da-0134-2f28-737688e4d168", "3bb47210-26d2-0134-2f28-737688e4d168" }, false);
//        verify(databaseManager).findWhere(eq(Episode.class), eq("uuid IN ('67090710-26db-0134-2f28-737688e4d168','416ed060-26da-0134-2f28-737688e4d168','3bb47210-26d2-0134-2f28-737688e4d168')"));
//    }

}