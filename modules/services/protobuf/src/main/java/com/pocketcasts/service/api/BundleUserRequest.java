// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.BundleUserRequest}
 */
public  final class BundleUserRequest extends
    com.google.protobuf.GeneratedMessageLite<
        BundleUserRequest, BundleUserRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:api.BundleUserRequest)
    BundleUserRequestOrBuilder {
  private BundleUserRequest() {
    userUuid_ = "";
    bundles_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
  }
  public static final int USER_UUID_FIELD_NUMBER = 1;
  private java.lang.String userUuid_;
  /**
   * <code>string user_uuid = 1;</code>
   * @return The userUuid.
   */
  @java.lang.Override
  public java.lang.String getUserUuid() {
    return userUuid_;
  }
  /**
   * <code>string user_uuid = 1;</code>
   * @return The bytes for userUuid.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getUserUuidBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(userUuid_);
  }
  /**
   * <code>string user_uuid = 1;</code>
   * @param value The userUuid to set.
   */
  private void setUserUuid(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    userUuid_ = value;
  }
  /**
   * <code>string user_uuid = 1;</code>
   */
  private void clearUserUuid() {

    userUuid_ = getDefaultInstance().getUserUuid();
  }
  /**
   * <code>string user_uuid = 1;</code>
   * @param value The bytes for userUuid to set.
   */
  private void setUserUuidBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    userUuid_ = value.toStringUtf8();

  }

  public static final int BUNDLES_FIELD_NUMBER = 2;
  private com.google.protobuf.Internal.ProtobufList<java.lang.String> bundles_;
  /**
   * <code>repeated string bundles = 2;</code>
   * @return A list containing the bundles.
   */
  @java.lang.Override
  public java.util.List<java.lang.String> getBundlesList() {
    return bundles_;
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @return The count of bundles.
   */
  @java.lang.Override
  public int getBundlesCount() {
    return bundles_.size();
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @param index The index of the element to return.
   * @return The bundles at the given index.
   */
  @java.lang.Override
  public java.lang.String getBundles(int index) {
    return bundles_.get(index);
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @param index The index of the value to return.
   * @return The bytes of the bundles at the given index.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getBundlesBytes(int index) {
    return com.google.protobuf.ByteString.copyFromUtf8(
        bundles_.get(index));
  }
  private void ensureBundlesIsMutable() {
    com.google.protobuf.Internal.ProtobufList<java.lang.String> tmp =
        bundles_;  if (!tmp.isModifiable()) {
      bundles_ =
          com.google.protobuf.GeneratedMessageLite.mutableCopy(tmp);
     }
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @param index The index to set the value at.
   * @param value The bundles to set.
   */
  private void setBundles(
      int index, java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  ensureBundlesIsMutable();
    bundles_.set(index, value);
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @param value The bundles to add.
   */
  private void addBundles(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  ensureBundlesIsMutable();
    bundles_.add(value);
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @param values The bundles to add.
   */
  private void addAllBundles(
      java.lang.Iterable<java.lang.String> values) {
    ensureBundlesIsMutable();
    com.google.protobuf.AbstractMessageLite.addAll(
        values, bundles_);
  }
  /**
   * <code>repeated string bundles = 2;</code>
   */
  private void clearBundles() {
    bundles_ = com.google.protobuf.GeneratedMessageLite.emptyProtobufList();
  }
  /**
   * <code>repeated string bundles = 2;</code>
   * @param value The bytes of the bundles to add.
   */
  private void addBundlesBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    ensureBundlesIsMutable();
    bundles_.add(value.toStringUtf8());
  }

  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.BundleUserRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.BundleUserRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.BundleUserRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.BundleUserRequest prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.BundleUserRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.BundleUserRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.BundleUserRequest)
      com.pocketcasts.service.api.BundleUserRequestOrBuilder {
    // Construct using com.pocketcasts.service.api.BundleUserRequest.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string user_uuid = 1;</code>
     * @return The userUuid.
     */
    @java.lang.Override
    public java.lang.String getUserUuid() {
      return instance.getUserUuid();
    }
    /**
     * <code>string user_uuid = 1;</code>
     * @return The bytes for userUuid.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getUserUuidBytes() {
      return instance.getUserUuidBytes();
    }
    /**
     * <code>string user_uuid = 1;</code>
     * @param value The userUuid to set.
     * @return This builder for chaining.
     */
    public Builder setUserUuid(
        java.lang.String value) {
      copyOnWrite();
      instance.setUserUuid(value);
      return this;
    }
    /**
     * <code>string user_uuid = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearUserUuid() {
      copyOnWrite();
      instance.clearUserUuid();
      return this;
    }
    /**
     * <code>string user_uuid = 1;</code>
     * @param value The bytes for userUuid to set.
     * @return This builder for chaining.
     */
    public Builder setUserUuidBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setUserUuidBytes(value);
      return this;
    }

    /**
     * <code>repeated string bundles = 2;</code>
     * @return A list containing the bundles.
     */
    @java.lang.Override
    public java.util.List<java.lang.String>
        getBundlesList() {
      return java.util.Collections.unmodifiableList(
          instance.getBundlesList());
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @return The count of bundles.
     */
    @java.lang.Override
    public int getBundlesCount() {
      return instance.getBundlesCount();
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @param index The index of the element to return.
     * @return The bundles at the given index.
     */
    @java.lang.Override
    public java.lang.String getBundles(int index) {
      return instance.getBundles(index);
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @param index The index of the value to return.
     * @return The bytes of the bundles at the given index.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getBundlesBytes(int index) {
      return instance.getBundlesBytes(index);
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @param index The index to set the value at.
     * @param value The bundles to set.
     * @return This builder for chaining.
     */
    public Builder setBundles(
        int index, java.lang.String value) {
      copyOnWrite();
      instance.setBundles(index, value);
      return this;
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @param value The bundles to add.
     * @return This builder for chaining.
     */
    public Builder addBundles(
        java.lang.String value) {
      copyOnWrite();
      instance.addBundles(value);
      return this;
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @param values The bundles to add.
     * @return This builder for chaining.
     */
    public Builder addAllBundles(
        java.lang.Iterable<java.lang.String> values) {
      copyOnWrite();
      instance.addAllBundles(values);
      return this;
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearBundles() {
      copyOnWrite();
      instance.clearBundles();
      return this;
    }
    /**
     * <code>repeated string bundles = 2;</code>
     * @param value The bytes of the bundles to add.
     * @return This builder for chaining.
     */
    public Builder addBundlesBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.addBundlesBytes(value);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.BundleUserRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.BundleUserRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "userUuid_",
            "bundles_",
          };
          java.lang.String info =
              "\u0000\u0002\u0000\u0000\u0001\u0002\u0002\u0000\u0001\u0000\u0001\u0208\u0002\u021a" +
              "";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.BundleUserRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.BundleUserRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.BundleUserRequest>(
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


  // @@protoc_insertion_point(class_scope:api.BundleUserRequest)
  private static final com.pocketcasts.service.api.BundleUserRequest DEFAULT_INSTANCE;
  static {
    BundleUserRequest defaultInstance = new BundleUserRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      BundleUserRequest.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.BundleUserRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<BundleUserRequest> PARSER;

  public static com.google.protobuf.Parser<BundleUserRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

