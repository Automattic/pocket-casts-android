// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")

package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializebookmarkRequest")
public inline fun bookmarkRequest(block: com.pocketcasts.service.api.BookmarkRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.BookmarkRequest =
    com.pocketcasts.service.api.BookmarkRequestKt.Dsl._create(com.pocketcasts.service.api.BookmarkRequest.newBuilder()).apply { block() }._build()

/**
 * Protobuf type `api.BookmarkRequest`
 */
public object BookmarkRequestKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.BookmarkRequest.Builder,
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.BookmarkRequest.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.BookmarkRequest = _builder.build()

        /**
         * `string podcast_uuid = 1;`
         */
        public var podcastUuid: kotlin.String
            @JvmName("getPodcastUuid")
            get() = _builder.getPodcastUuid()

            @JvmName("setPodcastUuid")
            set(value) {
                _builder.setPodcastUuid(value)
            }

        /**
         * `string podcast_uuid = 1;`
         */
        public fun clearPodcastUuid() {
            _builder.clearPodcastUuid()
        }

        /**
         * `string episode_uuid = 2;`
         */
        public var episodeUuid: kotlin.String
            @JvmName("getEpisodeUuid")
            get() = _builder.getEpisodeUuid()

            @JvmName("setEpisodeUuid")
            set(value) {
                _builder.setEpisodeUuid(value)
            }

        /**
         * `string episode_uuid = 2;`
         */
        public fun clearEpisodeUuid() {
            _builder.clearEpisodeUuid()
        }

        /**
         * `.google.protobuf.Int32Value time = 3;`
         */
        public var time: com.google.protobuf.Int32Value
            @JvmName("getTime")
            get() = _builder.getTime()

            @JvmName("setTime")
            set(value) {
                _builder.setTime(value)
            }

        /**
         * `.google.protobuf.Int32Value time = 3;`
         */
        public fun clearTime() {
            _builder.clearTime()
        }

        /**
         * `.google.protobuf.Int32Value time = 3;`
         * @return Whether the time field is set.
         */
        public fun hasTime(): kotlin.Boolean {
            return _builder.hasTime()
        }

        /**
         * `.google.protobuf.StringValue title = 4;`
         */
        public var title: com.google.protobuf.StringValue
            @JvmName("getTitle")
            get() = _builder.getTitle()

            @JvmName("setTitle")
            set(value) {
                _builder.setTitle(value)
            }

        /**
         * `.google.protobuf.StringValue title = 4;`
         */
        public fun clearTitle() {
            _builder.clearTitle()
        }

        /**
         * `.google.protobuf.StringValue title = 4;`
         * @return Whether the title field is set.
         */
        public fun hasTitle(): kotlin.Boolean {
            return _builder.hasTitle()
        }
    }
}
public inline fun com.pocketcasts.service.api.BookmarkRequest.copy(block: com.pocketcasts.service.api.BookmarkRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.BookmarkRequest =
    com.pocketcasts.service.api.BookmarkRequestKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.service.api.BookmarkRequestOrBuilder.timeOrNull: com.google.protobuf.Int32Value?
    get() = if (hasTime()) getTime() else null

public val com.pocketcasts.service.api.BookmarkRequestOrBuilder.titleOrNull: com.google.protobuf.StringValue?
    get() = if (hasTitle()) getTitle() else null
