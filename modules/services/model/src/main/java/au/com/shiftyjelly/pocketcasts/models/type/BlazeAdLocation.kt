package au.com.shiftyjelly.pocketcasts.models.type

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter

enum class BlazeAdLocation(val value: String) {
    PodcastList(
        value = "podcastList",
    ),
    Player(
        value = "player",
    ),
    Unknown(
        value = "",
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
