// Generated by the protocol buffer compiler. DO NOT EDIT!
// source: files.proto

// Generated files should ignore deprecation warnings
@file:Suppress("DEPRECATION")
package com.pocketcasts.files.data

@kotlin.jvm.JvmName("-initializefileListResponse")
public inline fun fileListResponse(block: com.pocketcasts.files.data.FileListResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.files.data.FileListResponse =
    com.pocketcasts.files.data.FileListResponseKt.Dsl._create(com.pocketcasts.files.data.FileListResponse.newBuilder()).apply { block() }._build()
/**
 * Protobuf type `files.FileListResponse`
 */
public object FileListResponseKt {
    @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
    @com.google.protobuf.kotlin.ProtoDslMarker
    public class Dsl private constructor(
        private val _builder: com.pocketcasts.files.data.FileListResponse.Builder
    ) {
        public companion object {
            @kotlin.jvm.JvmSynthetic
            @kotlin.PublishedApi
            internal fun _create(builder: com.pocketcasts.files.data.FileListResponse.Builder): Dsl = Dsl(builder)
        }

        @kotlin.jvm.JvmSynthetic
        @kotlin.PublishedApi
        internal fun _build(): com.pocketcasts.files.data.FileListResponse = _builder.build()

        /**
         * An uninstantiable, behaviorless type to represent the field in
         * generics.
         */
        @kotlin.OptIn(com.google.protobuf.kotlin.OnlyForUseByGeneratedProtoCode::class)
        public class FilesProxy private constructor() : com.google.protobuf.kotlin.DslProxy()
        /**
         * `repeated .files.File files = 1;`
         */
        public val files: com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>
            @kotlin.jvm.JvmSynthetic
            get() = com.google.protobuf.kotlin.DslList(
                _builder.getFilesList()
            )
        /**
         * `repeated .files.File files = 1;`
         * @param value The files to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addFiles")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>.add(value: com.pocketcasts.files.data.File) {
            _builder.addFiles(value)
        }
        /**
         * `repeated .files.File files = 1;`
         * @param value The files to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignFiles")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>.plusAssign(value: com.pocketcasts.files.data.File) {
            add(value)
        }
        /**
         * `repeated .files.File files = 1;`
         * @param values The files to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("addAllFiles")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>.addAll(values: kotlin.collections.Iterable<com.pocketcasts.files.data.File>) {
            _builder.addAllFiles(values)
        }
        /**
         * `repeated .files.File files = 1;`
         * @param values The files to add.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("plusAssignAllFiles")
        @Suppress("NOTHING_TO_INLINE")
        public inline operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>.plusAssign(values: kotlin.collections.Iterable<com.pocketcasts.files.data.File>) {
            addAll(values)
        }
        /**
         * `repeated .files.File files = 1;`
         * @param index The index to set the value at.
         * @param value The files to set.
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("setFiles")
        public operator fun com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>.set(index: kotlin.Int, value: com.pocketcasts.files.data.File) {
            _builder.setFiles(index, value)
        }
        /**
         * `repeated .files.File files = 1;`
         */
        @kotlin.jvm.JvmSynthetic
        @kotlin.jvm.JvmName("clearFiles")
        public fun com.google.protobuf.kotlin.DslList<com.pocketcasts.files.data.File, FilesProxy>.clear() {
            _builder.clearFiles()
        }

        /**
         * `.files.AccountUsage account = 2;`
         */
        public var account: com.pocketcasts.files.data.AccountUsage
            @JvmName("getAccount")
            get() = _builder.getAccount()
            @JvmName("setAccount")
            set(value) {
                _builder.setAccount(value)
            }
        /**
         * `.files.AccountUsage account = 2;`
         */
        public fun clearAccount() {
            _builder.clearAccount()
        }
        /**
         * `.files.AccountUsage account = 2;`
         * @return Whether the account field is set.
         */
        public fun hasAccount(): kotlin.Boolean {
            return _builder.hasAccount()
        }
    }
}
public inline fun com.pocketcasts.files.data.FileListResponse.copy(block: com.pocketcasts.files.data.FileListResponseKt.Dsl.() -> kotlin.Unit): com.pocketcasts.files.data.FileListResponse =
    com.pocketcasts.files.data.FileListResponseKt.Dsl._create(this.toBuilder()).apply { block() }._build()

public val com.pocketcasts.files.data.FileListResponseOrBuilder.accountOrNull: com.pocketcasts.files.data.AccountUsage?
    get() = if (hasAccount()) getAccount() else null
