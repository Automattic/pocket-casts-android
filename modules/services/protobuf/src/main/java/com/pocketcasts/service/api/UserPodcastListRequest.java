// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.UserPodcastListRequest}
 */
public  final class UserPodcastListRequest extends
    com.google.protobuf.GeneratedMessageLite<
        UserPodcastListRequest, UserPodcastListRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:api.UserPodcastListRequest)
    UserPodcastListRequestOrBuilder {
  private UserPodcastListRequest() {
    v_ = "";
    m_ = "";
  }
  public static final int V_FIELD_NUMBER = 1;
  private java.lang.String v_;
  /**
   * <code>string v = 1;</code>
   * @return The v.
   */
  @java.lang.Override
  public java.lang.String getV() {
    return v_;
  }
  /**
   * <code>string v = 1;</code>
   * @return The bytes for v.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getVBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(v_);
  }
  /**
   * <code>string v = 1;</code>
   * @param value The v to set.
   */
  private void setV(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    v_ = value;
  }
  /**
   * <code>string v = 1;</code>
   */
  private void clearV() {

    v_ = getDefaultInstance().getV();
  }
  /**
   * <code>string v = 1;</code>
   * @param value The bytes for v to set.
   */
  private void setVBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    v_ = value.toStringUtf8();

  }

  public static final int M_FIELD_NUMBER = 2;
  private java.lang.String m_;
  /**
   * <code>string m = 2;</code>
   * @return The m.
   */
  @java.lang.Override
  public java.lang.String getM() {
    return m_;
  }
  /**
   * <code>string m = 2;</code>
   * @return The bytes for m.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getMBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(m_);
  }
  /**
   * <code>string m = 2;</code>
   * @param value The m to set.
   */
  private void setM(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    m_ = value;
  }
  /**
   * <code>string m = 2;</code>
   */
  private void clearM() {

    m_ = getDefaultInstance().getM();
  }
  /**
   * <code>string m = 2;</code>
   * @param value The bytes for m to set.
   */
  private void setMBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    m_ = value.toStringUtf8();

  }

  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.UserPodcastListRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.UserPodcastListRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.UserPodcastListRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.UserPodcastListRequest prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.UserPodcastListRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.UserPodcastListRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.UserPodcastListRequest)
      com.pocketcasts.service.api.UserPodcastListRequestOrBuilder {
    // Construct using com.pocketcasts.service.api.UserPodcastListRequest.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string v = 1;</code>
     * @return The v.
     */
    @java.lang.Override
    public java.lang.String getV() {
      return instance.getV();
    }
    /**
     * <code>string v = 1;</code>
     * @return The bytes for v.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getVBytes() {
      return instance.getVBytes();
    }
    /**
     * <code>string v = 1;</code>
     * @param value The v to set.
     * @return This builder for chaining.
     */
    public Builder setV(
        java.lang.String value) {
      copyOnWrite();
      instance.setV(value);
      return this;
    }
    /**
     * <code>string v = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearV() {
      copyOnWrite();
      instance.clearV();
      return this;
    }
    /**
     * <code>string v = 1;</code>
     * @param value The bytes for v to set.
     * @return This builder for chaining.
     */
    public Builder setVBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setVBytes(value);
      return this;
    }

    /**
     * <code>string m = 2;</code>
     * @return The m.
     */
    @java.lang.Override
    public java.lang.String getM() {
      return instance.getM();
    }
    /**
     * <code>string m = 2;</code>
     * @return The bytes for m.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getMBytes() {
      return instance.getMBytes();
    }
    /**
     * <code>string m = 2;</code>
     * @param value The m to set.
     * @return This builder for chaining.
     */
    public Builder setM(
        java.lang.String value) {
      copyOnWrite();
      instance.setM(value);
      return this;
    }
    /**
     * <code>string m = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearM() {
      copyOnWrite();
      instance.clearM();
      return this;
    }
    /**
     * <code>string m = 2;</code>
     * @param value The bytes for m to set.
     * @return This builder for chaining.
     */
    public Builder setMBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setMBytes(value);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.UserPodcastListRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.UserPodcastListRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "v_",
            "m_",
          };
          java.lang.String info =
              "\u0000\u0002\u0000\u0000\u0001\u0002\u0002\u0000\u0000\u0000\u0001\u0208\u0002\u0208" +
              "";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.UserPodcastListRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.UserPodcastListRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.UserPodcastListRequest>(
                      DEFAULT_INSTANCE);
              PARSER = parser;
            }
          }
        }
        return parser;
    }
    case GET_MEMOIZED_IS_INITIALIZED: {
      return (byte) 1;
    }
    case SET_MEMOIZED_IS_INITIALIZED: {
      return null;
    }
    }
    throw new UnsupportedOperationException();
  }


  // @@protoc_insertion_point(class_scope:api.UserPodcastListRequest)
  private static final com.pocketcasts.service.api.UserPodcastListRequest DEFAULT_INSTANCE;
  static {
    UserPodcastListRequest defaultInstance = new UserPodcastListRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      UserPodcastListRequest.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.UserPodcastListRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<UserPodcastListRequest> PARSER;

  public static com.google.protobuf.Parser<UserPodcastListRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

