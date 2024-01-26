// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

// Protobuf Java Version: 3.25.1
package com.pocketcasts.service.api;

public interface SyncUserPodcastOrBuilder extends
    // @@protoc_insertion_point(interface_extends:api.SyncUserPodcast)
    com.google.protobuf.MessageLiteOrBuilder {

  /**
   * <code>string uuid = 1;</code>
   * @return The uuid.
   */
  java.lang.String getUuid();
  /**
   * <code>string uuid = 1;</code>
   * @return The bytes for uuid.
   */
  com.google.protobuf.ByteString
      getUuidBytes();

  /**
   * <code>.google.protobuf.BoolValue is_deleted = 2;</code>
   * @return Whether the isDeleted field is set.
   */
  boolean hasIsDeleted();
  /**
   * <code>.google.protobuf.BoolValue is_deleted = 2;</code>
   * @return The isDeleted.
   */
  com.google.protobuf.BoolValue getIsDeleted();

  /**
   * <code>.google.protobuf.BoolValue subscribed = 3;</code>
   * @return Whether the subscribed field is set.
   */
  boolean hasSubscribed();
  /**
   * <code>.google.protobuf.BoolValue subscribed = 3;</code>
   * @return The subscribed.
   */
  com.google.protobuf.BoolValue getSubscribed();

  /**
   * <code>.google.protobuf.Int32Value auto_start_from = 4;</code>
   * @return Whether the autoStartFrom field is set.
   */
  boolean hasAutoStartFrom();
  /**
   * <code>.google.protobuf.Int32Value auto_start_from = 4;</code>
   * @return The autoStartFrom.
   */
  com.google.protobuf.Int32Value getAutoStartFrom();

  /**
   * <code>.google.protobuf.Int32Value episodes_sort_order = 5;</code>
   * @return Whether the episodesSortOrder field is set.
   */
  boolean hasEpisodesSortOrder();
  /**
   * <code>.google.protobuf.Int32Value episodes_sort_order = 5;</code>
   * @return The episodesSortOrder.
   */
  com.google.protobuf.Int32Value getEpisodesSortOrder();

  /**
   * <code>.google.protobuf.Int32Value auto_skip_last = 6;</code>
   * @return Whether the autoSkipLast field is set.
   */
  boolean hasAutoSkipLast();
  /**
   * <code>.google.protobuf.Int32Value auto_skip_last = 6;</code>
   * @return The autoSkipLast.
   */
  com.google.protobuf.Int32Value getAutoSkipLast();

  /**
   * <code>.google.protobuf.StringValue folder_uuid = 7;</code>
   * @return Whether the folderUuid field is set.
   */
  boolean hasFolderUuid();
  /**
   * <code>.google.protobuf.StringValue folder_uuid = 7;</code>
   * @return The folderUuid.
   */
  com.google.protobuf.StringValue getFolderUuid();

  /**
   * <code>.google.protobuf.Int32Value sort_position = 8;</code>
   * @return Whether the sortPosition field is set.
   */
  boolean hasSortPosition();
  /**
   * <code>.google.protobuf.Int32Value sort_position = 8;</code>
   * @return The sortPosition.
   */
  com.google.protobuf.Int32Value getSortPosition();

  /**
   * <code>.google.protobuf.Timestamp date_added = 9;</code>
   * @return Whether the dateAdded field is set.
   */
  boolean hasDateAdded();
  /**
   * <code>.google.protobuf.Timestamp date_added = 9;</code>
   * @return The dateAdded.
   */
  com.google.protobuf.Timestamp getDateAdded();

  /**
   * <code>.api.PodcastSettings settings = 10;</code>
   * @return Whether the settings field is set.
   */
  boolean hasSettings();
  /**
   * <code>.api.PodcastSettings settings = 10;</code>
   * @return The settings.
   */
  com.pocketcasts.service.api.PodcastSettings getSettings();
}
