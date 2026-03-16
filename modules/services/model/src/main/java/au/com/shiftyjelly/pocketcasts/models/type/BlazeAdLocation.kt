package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import com.automattic.eventhorizon.BlazeAdSource
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class BlazeAdLocation(
    val key: String,
    val eventHorizonValue: BlazeAdSource,
    val feature: Feature?,
) {
    PodcastList(
        key = "podcast_list",
        eventHorizonValue = BlazeAdSource.PodcastList,
        feature = Feature.BANNER_ADS_PODCASTS,
    ),
    Player(
        key = "player",
        eventHorizonValue = BlazeAdSource.Player,
        feature = Feature.BANNER_ADS_PLAYER,
    ),
    Unknown(
        key = "",
        eventHorizonValue = BlazeAdSource.Unknown,
        feature = null,
    ),
    ;

    companion object {
        fun fromServer(value: String?) = entries.find { it.key == value } ?: Unknown
    }
}

class BlazeAdLocationMoshiAdapter : JsonAdapter<BlazeAdLocation>() {
    override fun toJson(writer: JsonWriter, blazeAdLocation: BlazeAdLocation?) {
        writer.value(blazeAdLocation?.key)
    }

    override fun fromJson(reader: JsonReader): BlazeAdLocation {
        return BlazeAdLocation.fromServer(reader.nextString())
    }
}
