// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

public interface UpNextPlayRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:api.UpNextPlayRequest)
    com.google.protobuf.MessageLiteOrBuilder {

  /**
   * <code>string version = 1;</code>
   * @return The version.
   */
  java.lang.String getVersion();
  /**
   * <code>string version = 1;</code>
   * @return The bytes for version.
   */
  com.google.protobuf.ByteString
      getVersionBytes();

  /**
   * <code>string model = 2;</code>
   * @return The model.
   */
  java.lang.String getModel();
  /**
   * <code>string model = 2;</code>
   * @return The bytes for model.
   */
  com.google.protobuf.ByteString
      getModelBytes();

  /**
   * <code>.api.UpNextEpisodeRequest episode = 3;</code>
   * @return Whether the episode field is set.
   */
  boolean hasEpisode();
  /**
   * <code>.api.UpNextEpisodeRequest episode = 3;</code>
   * @return The episode.
   */
  com.pocketcasts.service.api.UpNextEpisodeRequest getEpisode();
}
