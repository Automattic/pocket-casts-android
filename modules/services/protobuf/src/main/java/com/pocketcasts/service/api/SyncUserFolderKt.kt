// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: api.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")

package com.pocketcasts.service.api

@kotlin.jvm.JvmName("-initializesyncUserFolder")
public inline fun syncUserFolder(block: com.pocketcasts.service.api.SyncUserFolderKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.SyncUserFolder =
    com.pocketcasts.service.api.SyncUserFolderKt.Dsl._create(com.pocketcasts.service.api.SyncUserFolder.newBuilder()).apply { block() }._build()

/**
 * Protobuf type `api.SyncUserFolder`
 */
public object SyncUserFolderKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.service.api.SyncUserFolder.Builder,
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.service.api.SyncUserFolder.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.service.api.SyncUserFolder = _builder.build()

        /**
         * `string folder_uuid = 1;`
         */
        public var folderUuid: kotlin.String
            @JvmName("getFolderUuid")
            get() = _builder.getFolderUuid()

            @JvmName("setFolderUuid")
            set(value) {
                _builder.setFolderUuid(value)
            }

        /**
         * `string folder_uuid = 1;`
         */
        public fun clearFolderUuid() {
            _builder.clearFolderUuid()
        }

        /**
         * <code>bool is_deleted = 2;</code>
         */
        public var isDeleted: kotlin.Boolean
            @JvmName("getIsDeleted")
            get() = _builder.getIsDeleted()

            @JvmName("setIsDeleted")
            set(value) {
                _builder.setIsDeleted(value)
            }

        /**
         * `bool is_deleted = 2;`
         */
        public fun clearIsDeleted() {
            _builder.clearIsDeleted()
        }

        /**
         * `string name = 3;`
         */
        public var name: kotlin.String
            @JvmName("getName")
            get() = _builder.getName()

            @JvmName("setName")
            set(value) {
                _builder.setName(value)
            }

        /**
         * `string name = 3;`
         */
        public fun clearName() {
            _builder.clearName()
        }

        /**
         * <code>int32 color = 4;</code>
         */
        public var color: kotlin.Int
            @JvmName("getColor")
            get() = _builder.getColor()

            @JvmName("setColor")
            set(value) {
                _builder.setColor(value)
            }

        /**
         * `int32 color = 4;`
         */
        public fun clearColor() {
            _builder.clearColor()
        }

        /**
         * <code>int32 sort_position = 5;</code>
         */
        public var sortPosition: kotlin.Int
            @JvmName("getSortPosition")
            get() = _builder.getSortPosition()

            @JvmName("setSortPosition")
            set(value) {
                _builder.setSortPosition(value)
            }

        /**
         * `int32 sort_position = 5;`
         */
        public fun clearSortPosition() {
            _builder.clearSortPosition()
        }

        /**
         * <code>int32 podcasts_sort_type = 6;</code>
         */
        public var podcastsSortType: kotlin.Int
            @JvmName("getPodcastsSortType")
            get() = _builder.getPodcastsSortType()

            @JvmName("setPodcastsSortType")
            set(value) {
                _builder.setPodcastsSortType(value)
            }

        /**
         * `int32 podcasts_sort_type = 6;`
         */
        public fun clearPodcastsSortType() {
            _builder.clearPodcastsSortType()
        }

        /**
         * `.google.protobuf.Timestamp date_added = 7;`
         */
        public var dateAdded: com.google.protobuf.Timestamp
            @JvmName("getDateAdded")
            get() = _builder.getDateAdded()

            @JvmName("setDateAdded")
            set(value) {
                _builder.setDateAdded(value)
            }

        /**
         * `.google.protobuf.Timestamp date_added = 7;`
         */
        public fun clearDateAdded() {
            _builder.clearDateAdded()
        }

        /**
         * `.google.protobuf.Timestamp date_added = 7;`
         * @return Whether the dateAdded field is set.
         */
        public fun hasDateAdded(): kotlin.Boolean {
            return _builder.hasDateAdded()
        }
    }
}
public inline fun com.pocketcasts.service.api.SyncUserFolder.copy(block: `com.pocketcasts.service.api`.SyncUserFolderKt.Dsl.() -> kotlin.Unit): com.pocketcasts.service.api.SyncUserFolder =
    `com.pocketcasts.service.api`.SyncUserFolderKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.service.api.SyncUserFolderOrBuilder.dateAddedOrNull: com.google.protobuf.Timestamp?
    get() = if (hasDateAdded()) getDateAdded() else null
