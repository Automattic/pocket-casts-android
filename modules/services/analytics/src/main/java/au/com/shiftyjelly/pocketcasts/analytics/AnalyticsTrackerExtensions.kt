package au.com.shiftyjelly.pocketcasts.analytics

fun AnalyticsTracker.discoverListShowAllTapped(listId: String, listDate: String) {
    discoverListEvent(
        analyticsEvent = AnalyticsEvent.DISCOVER_LIST_SHOW_ALL_TAPPED,
        listId = listId,
        listDate = listDate,
    )
}

fun AnalyticsTracker.discoverShowAllTapped(listId: String, listDate: String) {
    discoverListEvent(
        analyticsEvent = AnalyticsEvent.DISCOVER_SHOW_ALL_TAPPED,
        listId = listId,
        listDate = listDate,
    )
}

private fun AnalyticsTracker.discoverListEvent(analyticsEvent: AnalyticsEvent, listId: String, listDate: String) {
    track(
        event = analyticsEvent,
        properties = mapOf(
            AnalyticsParameter.listId to listId,
            AnalyticsParameter.listDate to listDate,
        ),
    )
}

fun AnalyticsTracker.discoverListPodcastTapped(podcastUuid: String, listId: String?, listDate: String?) {
    discoverListPodcastEvent(
        analyticsEvent = AnalyticsEvent.DISCOVER_LIST_PODCAST_TAPPED,
        podcastUuid = podcastUuid,
        listId = listId,
        listDate = listDate,
    )
}

fun AnalyticsTracker.discoverListPodcastSubscribed(podcastUuid: String, listId: String?, listDate: String?) {
    discoverListPodcastEvent(
        analyticsEvent = AnalyticsEvent.DISCOVER_LIST_PODCAST_SUBSCRIBED,
        podcastUuid = podcastUuid,
        listId = listId,
        listDate = listDate,
    )
}

private fun AnalyticsTracker.discoverListPodcastEvent(analyticsEvent: AnalyticsEvent, podcastUuid: String, listId: String?, listDate: String?) {
    if (listId == null) {
        return
    }
    track(
        event = analyticsEvent,
        properties = mapOf(
            AnalyticsParameter.podcastUuid to podcastUuid,
            AnalyticsParameter.listId to listId,
            AnalyticsParameter.listDate to (listDate ?: ""),
        ),
    )
}
