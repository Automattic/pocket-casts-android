// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.SubscriptionsPurchaseAndroidRequest}
 */
public  final class SubscriptionsPurchaseAndroidRequest extends
    com.google.protobuf.GeneratedMessageLite<
        SubscriptionsPurchaseAndroidRequest, SubscriptionsPurchaseAndroidRequest.Builder> implements
    // @@protoc_insertion_point(message_implements:api.SubscriptionsPurchaseAndroidRequest)
    SubscriptionsPurchaseAndroidRequestOrBuilder {
  private SubscriptionsPurchaseAndroidRequest() {
    purchaseToken_ = "";
    sku_ = "";
  }
  public static final int PURCHASETOKEN_FIELD_NUMBER = 1;
  private java.lang.String purchaseToken_;
  /**
   * <code>string purchaseToken = 1;</code>
   * @return The purchaseToken.
   */
  @java.lang.Override
  public java.lang.String getPurchaseToken() {
    return purchaseToken_;
  }
  /**
   * <code>string purchaseToken = 1;</code>
   * @return The bytes for purchaseToken.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getPurchaseTokenBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(purchaseToken_);
  }
  /**
   * <code>string purchaseToken = 1;</code>
   * @param value The purchaseToken to set.
   */
  private void setPurchaseToken(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    purchaseToken_ = value;
  }
  /**
   * <code>string purchaseToken = 1;</code>
   */
  private void clearPurchaseToken() {

    purchaseToken_ = getDefaultInstance().getPurchaseToken();
  }
  /**
   * <code>string purchaseToken = 1;</code>
   * @param value The bytes for purchaseToken to set.
   */
  private void setPurchaseTokenBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    purchaseToken_ = value.toStringUtf8();

  }

  public static final int SKU_FIELD_NUMBER = 2;
  private java.lang.String sku_;
  /**
   * <code>string sku = 2;</code>
   * @return The sku.
   */
  @java.lang.Override
  public java.lang.String getSku() {
    return sku_;
  }
  /**
   * <code>string sku = 2;</code>
   * @return The bytes for sku.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getSkuBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(sku_);
  }
  /**
   * <code>string sku = 2;</code>
   * @param value The sku to set.
   */
  private void setSku(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    sku_ = value;
  }
  /**
   * <code>string sku = 2;</code>
   */
  private void clearSku() {

    sku_ = getDefaultInstance().getSku();
  }
  /**
   * <code>string sku = 2;</code>
   * @param value The bytes for sku to set.
   */
  private void setSkuBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    sku_ = value.toStringUtf8();

  }

  public static final int NEWSLETTEROPTIN_FIELD_NUMBER = 3;
  private boolean newsletterOptIn_;
  /**
   * <code>bool newsletterOptIn = 3;</code>
   * @return The newsletterOptIn.
   */
  @java.lang.Override
  public boolean getNewsletterOptIn() {
    return newsletterOptIn_;
  }
  /**
   * <code>bool newsletterOptIn = 3;</code>
   * @param value The newsletterOptIn to set.
   */
  private void setNewsletterOptIn(boolean value) {
    
    newsletterOptIn_ = value;
  }
  /**
   * <code>bool newsletterOptIn = 3;</code>
   */
  private void clearNewsletterOptIn() {

    newsletterOptIn_ = false;
  }

  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.SubscriptionsPurchaseAndroidRequest}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.SubscriptionsPurchaseAndroidRequest)
      com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequestOrBuilder {
    // Construct using com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string purchaseToken = 1;</code>
     * @return The purchaseToken.
     */
    @java.lang.Override
    public java.lang.String getPurchaseToken() {
      return instance.getPurchaseToken();
    }
    /**
     * <code>string purchaseToken = 1;</code>
     * @return The bytes for purchaseToken.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getPurchaseTokenBytes() {
      return instance.getPurchaseTokenBytes();
    }
    /**
     * <code>string purchaseToken = 1;</code>
     * @param value The purchaseToken to set.
     * @return This builder for chaining.
     */
    public Builder setPurchaseToken(
        java.lang.String value) {
      copyOnWrite();
      instance.setPurchaseToken(value);
      return this;
    }
    /**
     * <code>string purchaseToken = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearPurchaseToken() {
      copyOnWrite();
      instance.clearPurchaseToken();
      return this;
    }
    /**
     * <code>string purchaseToken = 1;</code>
     * @param value The bytes for purchaseToken to set.
     * @return This builder for chaining.
     */
    public Builder setPurchaseTokenBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setPurchaseTokenBytes(value);
      return this;
    }

    /**
     * <code>string sku = 2;</code>
     * @return The sku.
     */
    @java.lang.Override
    public java.lang.String getSku() {
      return instance.getSku();
    }
    /**
     * <code>string sku = 2;</code>
     * @return The bytes for sku.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getSkuBytes() {
      return instance.getSkuBytes();
    }
    /**
     * <code>string sku = 2;</code>
     * @param value The sku to set.
     * @return This builder for chaining.
     */
    public Builder setSku(
        java.lang.String value) {
      copyOnWrite();
      instance.setSku(value);
      return this;
    }
    /**
     * <code>string sku = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearSku() {
      copyOnWrite();
      instance.clearSku();
      return this;
    }
    /**
     * <code>string sku = 2;</code>
     * @param value The bytes for sku to set.
     * @return This builder for chaining.
     */
    public Builder setSkuBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setSkuBytes(value);
      return this;
    }

    /**
     * <code>bool newsletterOptIn = 3;</code>
     * @return The newsletterOptIn.
     */
    @java.lang.Override
    public boolean getNewsletterOptIn() {
      return instance.getNewsletterOptIn();
    }
    /**
     * <code>bool newsletterOptIn = 3;</code>
     * @param value The newsletterOptIn to set.
     * @return This builder for chaining.
     */
    public Builder setNewsletterOptIn(boolean value) {
      copyOnWrite();
      instance.setNewsletterOptIn(value);
      return this;
    }
    /**
     * <code>bool newsletterOptIn = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearNewsletterOptIn() {
      copyOnWrite();
      instance.clearNewsletterOptIn();
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.SubscriptionsPurchaseAndroidRequest)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "purchaseToken_",
            "sku_",
            "newsletterOptIn_",
          };
          java.lang.String info =
              "\u0000\u0003\u0000\u0000\u0001\u0003\u0003\u0000\u0000\u0000\u0001\u0208\u0002\u0208" +
              "\u0003\u0007";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest>(
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


  // @@protoc_insertion_point(class_scope:api.SubscriptionsPurchaseAndroidRequest)
  private static final com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest DEFAULT_INSTANCE;
  static {
    SubscriptionsPurchaseAndroidRequest defaultInstance = new SubscriptionsPurchaseAndroidRequest();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      SubscriptionsPurchaseAndroidRequest.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.SubscriptionsPurchaseAndroidRequest getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<SubscriptionsPurchaseAndroidRequest> PARSER;

  public static com.google.protobuf.Parser<SubscriptionsPurchaseAndroidRequest> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

