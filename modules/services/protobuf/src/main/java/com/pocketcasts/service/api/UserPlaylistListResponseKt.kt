// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializeuserPlaylistListResponse")
public inline fun userPlaylistListResponse(block: com.pocketcasts.service.api.UserPlaylistListResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UserPlaylistListResponse =
    com.pocketcasts.service.api.UserPlaylistListResponseKt.Dsl._create(com.pocketcasts.service.api.UserPlaylistListResponse.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `api.UserPlaylistListResponse`
 */
public object UserPlaylistListResponseKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.UserPlaylistListResponse.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.UserPlaylistListResponse.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.UserPlaylistListResponse = _builder.build()

        /**
         * An uninstantiable, behaviorless type to represent the field in
         * generics.
         */
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        public class PlaylistsProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         */
        public val playlists: com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>
            @kotlin.jvm.JvmSynthetic
            get() = com.google.protobuf.kotlin.DslList(
                _builder.getPlaylistsList()
            )
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         * @param value The playlists to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addPlaylists")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>.add(value: com.pocketcasts.service.api.PlaylistSyncResponse) {
            _builder.addPlaylists(value)
        }
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         * @param value The playlists to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignPlaylists")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>.plusAssign(value: com.pocketcasts.service.api.PlaylistSyncResponse) {
            add(value)
        }
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         * @param values The playlists to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addAllPlaylists")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>.addAll(values: kotlin.collections.Iterable<com.pocketcasts.service.api.PlaylistSyncResponse>) {
            _builder.addAllPlaylists(values)
        }
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         * @param values The playlists to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignAllPlaylists")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>.plusAssign(values: kotlin.collections.Iterable<com.pocketcasts.service.api.PlaylistSyncResponse>) {
            addAll(values)
        }
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         * @param index The index to set the value at.
         * @param value The playlists to set.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("setPlaylists")
        public operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>.set(index: kotlin.Int, value: com.pocketcasts.service.api.PlaylistSyncResponse) {
            _builder.setPlaylists(index, value)
        }
        /**
         * `repeated .api.PlaylistSyncResponse playlists = 1;`
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("clearPlaylists")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.service.api.PlaylistSyncResponse, PlaylistsProxy>.clear() {
            _builder.clearPlaylists()
        }
    }
}
public inline fun com.pocketcasts.service.api.UserPlaylistListResponse.copy(block: com.pocketcasts.service.api.UserPlaylistListResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.UserPlaylistListResponse =
    com.pocketcasts.service.api.UserPlaylistListResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()
