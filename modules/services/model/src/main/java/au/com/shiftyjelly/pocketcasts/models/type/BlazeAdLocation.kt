package au.com.shiftyjelly.pocketcasts.models.type

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class BlazeAdLocation(val value: String, val feature: Feature?) {
    PodcastList(
        value = "podcast_list",
        feature = Feature.BANNER_ADS_PODCASTS,
    ),
    Player(
        value = "player",
        feature = Feature.BANNER_ADS_PLAYER,
    ),
    Unknown(
        value = "",
        feature = null,
    ),
    ;

    companion object {
        fun fromServer(value: String?) = entries.find { it.value == value } ?: Unknown
    }
}

class BlazeAdLocationMoshiAdapter : JsonAdapter<BlazeAdLocation>() {
    override fun toJson(writer: JsonWriter, blazeAdLocation: BlazeAdLocation?) {
        writer.value(blazeAdLocation?.value)
    }

    override fun fromJson(reader: JsonReader): BlazeAdLocation {
        return BlazeAdLocation.fromServer(reader.nextString())
    }
}
