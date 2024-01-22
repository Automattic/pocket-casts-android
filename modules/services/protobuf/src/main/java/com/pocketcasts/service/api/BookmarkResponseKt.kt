// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")

package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializebookmarkResponse")
public inline fun bookmarkResponse(block: com.pocketcasts.service.api.BookmarkResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.BookmarkResponse =
    com.pocketcasts.service.api.BookmarkResponseKt.Dsl._create(com.pocketcasts.service.api.BookmarkResponse.newBuilder()).apply { block() }._build()

/**
 * Protobuf type `api.BookmarkResponse`
 */
public object BookmarkResponseKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.BookmarkResponse.Builder,
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.BookmarkResponse.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.BookmarkResponse = _builder.build()

        /**
         * `string bookmark_uuid = 1;`
         */
        public var bookmarkUuid: kotlin.String
            @JvmName("getBookmarkUuid")
            get() = _builder.getBookmarkUuid()

            @JvmName("setBookmarkUuid")
            set(value) {
                _builder.setBookmarkUuid(value)
            }

        /**
         * `string bookmark_uuid = 1;`
         */
        public fun clearBookmarkUuid() {
            _builder.clearBookmarkUuid()
        }

        /**
         * `string podcast_uuid = 2;`
         */
        public var podcastUuid: kotlin.String
            @JvmName("getPodcastUuid")
            get() = _builder.getPodcastUuid()

            @JvmName("setPodcastUuid")
            set(value) {
                _builder.setPodcastUuid(value)
            }

        /**
         * `string podcast_uuid = 2;`
         */
        public fun clearPodcastUuid() {
            _builder.clearPodcastUuid()
        }

        /**
         * `string episode_uuid = 3;`
         */
        public var episodeUuid: kotlin.String
            @JvmName("getEpisodeUuid")
            get() = _builder.getEpisodeUuid()

            @JvmName("setEpisodeUuid")
            set(value) {
                _builder.setEpisodeUuid(value)
            }

        /**
         * `string episode_uuid = 3;`
         */
        public fun clearEpisodeUuid() {
            _builder.clearEpisodeUuid()
        }

        /**
         * <code>int32 time = 5;</code>
         */
        public var time: kotlin.Int
            @JvmName("getTime")
            get() = _builder.getTime()

            @JvmName("setTime")
            set(value) {
                _builder.setTime(value)
            }

        /**
         * `int32 time = 5;`
         */
        public fun clearTime() {
            _builder.clearTime()
        }

        /**
         * `string title = 6;`
         */
        public var title: kotlin.String
            @JvmName("getTitle")
            get() = _builder.getTitle()

            @JvmName("setTitle")
            set(value) {
                _builder.setTitle(value)
            }

        /**
         * `string title = 6;`
         */
        public fun clearTitle() {
            _builder.clearTitle()
        }

        /**
         * `.google.protobuf.Timestamp createdAt = 7;`
         */
        public var createdAt: com.google.protobuf.Timestamp
            @JvmName("getCreatedAt")
            get() = _builder.getCreatedAt()

            @JvmName("setCreatedAt")
            set(value) {
                _builder.setCreatedAt(value)
            }

        /**
         * `.google.protobuf.Timestamp createdAt = 7;`
         */
        public fun clearCreatedAt() {
            _builder.clearCreatedAt()
        }

        /**
         * `.google.protobuf.Timestamp createdAt = 7;`
         * @return Whether the createdAt field is set.
         */
        public fun hasCreatedAt(): kotlin.Boolean {
            return _builder.hasCreatedAt()
        }
    }
}
public inline fun com.pocketcasts.service.api.BookmarkResponse.copy(block: com.pocketcasts.service.api.BookmarkResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.BookmarkResponse =
    com.pocketcasts.service.api.BookmarkResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.service.api.BookmarkResponseOrBuilder.createdAtOrNull: com.google.protobuf.Timestamp?
    get() = if (hasCreatedAt()) getCreatedAt() else null
