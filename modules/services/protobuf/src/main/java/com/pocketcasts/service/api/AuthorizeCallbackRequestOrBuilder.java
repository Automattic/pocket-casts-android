// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

public interface AuthorizeCallbackRequestOrBuilder extends
    // @@protoc_insertion_point(interface_extends:api.AuthorizeCallbackRequest)
    com.google.protobuf.MessageLiteOrBuilder {

  /**
   * <code>string id_token = 1;</code>
   * @return The idToken.
   */
  java.lang.String getIdToken();
  /**
   * <code>string id_token = 1;</code>
   * @return The bytes for idToken.
   */
  com.google.protobuf.ByteString
      getIdTokenBytes();

  /**
   * <code>string state = 2;</code>
   * @return The state.
   */
  java.lang.String getState();
  /**
   * <code>string state = 2;</code>
   * @return The bytes for state.
   */
  com.google.protobuf.ByteString
      getStateBytes();
}
