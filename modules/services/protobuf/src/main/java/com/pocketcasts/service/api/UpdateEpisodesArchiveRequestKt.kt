// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializeupdateEpisodesArchiveRequest")
public inline fun updateEpisodesArchiveRequest(block: com.pocketcasts.service.api.UpdateEpisodesArchiveRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UpdateEpisodesArchiveRequest =
    com.pocketcasts.service.api.UpdateEpisodesArchiveRequestKt.Dsl._create(com.pocketcasts.service.api.UpdateEpisodesArchiveRequest.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.UpdateEpisodesArchiveRequest`
 */
public object UpdateEpisodesArchiveRequestKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.UpdateEpisodesArchiveRequest.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.UpdateEpisodesArchiveRequest.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.UpdateEpisodesArchiveRequest = _builder.build()

        /**
         * <code>bool archive = 1;</code>
         */
        public var archive: kotlin.Boolean
            @JvmName("getArchive")
            get() = _builder.getArchive()
            @JvmName("setArchive")
            set(value) {
                _builder.setArchive(value)
            }
        /**
         * `bool archive = 1;`
         */
        public fun clearArchive() {
            _builder.clearArchive()
        }

        /**
         * An uninstantiable, behaviorless type to represent the field in
         * generics.
         */
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        public class EpisodesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         */
        public val episodes: com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>
            @kotlin.jvm.JvmSynthetic
            get() = com.google.protobuf.kotlin.DslList(
                _builder.getEpisodesList()
            )
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         * @param value The episodes to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addEpisodes")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>.add(value: com.pocketcasts.service.api.EpisodeWithPodcast) {
            _builder.addEpisodes(value)
        }
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         * @param value The episodes to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignEpisodes")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>.plusAssign(value: com.pocketcasts.service.api.EpisodeWithPodcast) {
            add(value)
        }
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         * @param values The episodes to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addAllEpisodes")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>.addAll(values: kotlin.collections.Iterable<com.pocketcasts.service.api.EpisodeWithPodcast>) {
            _builder.addAllEpisodes(values)
        }
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         * @param values The episodes to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignAllEpisodes")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>.plusAssign(values: kotlin.collections.Iterable<com.pocketcasts.service.api.EpisodeWithPodcast>) {
            addAll(values)
        }
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         * @param index The index to set the value at.
         * @param value The episodes to set.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("setEpisodes")
        public operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>.set(index: kotlin.Int, value: com.pocketcasts.service.api.EpisodeWithPodcast) {
            _builder.setEpisodes(index, value)
        }
        /**
         * `repeated .api.EpisodeWithPodcast episodes = 2;`
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("clearEpisodes")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.EpisodeWithPodcast, EpisodesProxy>.clear() {
            _builder.clearEpisodes()
        }
    }
}
public inline fun com.pocketcasts.service.api.UpdateEpisodesArchiveRequest.copy(block: com.pocketcasts.service.api.UpdateEpisodesArchiveRequestKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UpdateEpisodesArchiveRequest =
    com.pocketcasts.service.api.UpdateEpisodesArchiveRequestKt.Dsl._create(this.toBuilder()).apply { block() }._build()
