// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: api.proto

package com.pocketcasts.service.api;

/**
 * Protobuf type {@code api.PaymentResponse}
 */
public  final class PaymentResponse extends
    com.google.protobuf.GeneratedMessageLite<
        PaymentResponse, PaymentResponse.Builder> implements
    // @@protoc_insertion_point(message_implements:api.PaymentResponse)
    PaymentResponseOrBuilder {
  private PaymentResponse() {
    paymentDate_ = "";
    currency_ = "";
  }
  public static final int PAYMENT_DATE_FIELD_NUMBER = 1;
  private java.lang.String paymentDate_;
  /**
   * <code>string payment_date = 1;</code>
   * @return The paymentDate.
   */
  @java.lang.Override
  public java.lang.String getPaymentDate() {
    return paymentDate_;
  }
  /**
   * <code>string payment_date = 1;</code>
   * @return The bytes for paymentDate.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getPaymentDateBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(paymentDate_);
  }
  /**
   * <code>string payment_date = 1;</code>
   * @param value The paymentDate to set.
   */
  private void setPaymentDate(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    paymentDate_ = value;
  }
  /**
   * <code>string payment_date = 1;</code>
   */
  private void clearPaymentDate() {

    paymentDate_ = getDefaultInstance().getPaymentDate();
  }
  /**
   * <code>string payment_date = 1;</code>
   * @param value The bytes for paymentDate to set.
   */
  private void setPaymentDateBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    paymentDate_ = value.toStringUtf8();

  }

  public static final int AMOUNT_FIELD_NUMBER = 2;
  private double amount_;
  /**
   * <code>double amount = 2;</code>
   * @return The amount.
   */
  @java.lang.Override
  public double getAmount() {
    return amount_;
  }
  /**
   * <code>double amount = 2;</code>
   * @param value The amount to set.
   */
  private void setAmount(double value) {
    
    amount_ = value;
  }
  /**
   * <code>double amount = 2;</code>
   */
  private void clearAmount() {

    amount_ = 0D;
  }

  public static final int CURRENCY_FIELD_NUMBER = 3;
  private java.lang.String currency_;
  /**
   * <code>string currency = 3;</code>
   * @return The currency.
   */
  @java.lang.Override
  public java.lang.String getCurrency() {
    return currency_;
  }
  /**
   * <code>string currency = 3;</code>
   * @return The bytes for currency.
   */
  @java.lang.Override
  public com.google.protobuf.ByteString
      getCurrencyBytes() {
    return com.google.protobuf.ByteString.copyFromUtf8(currency_);
  }
  /**
   * <code>string currency = 3;</code>
   * @param value The currency to set.
   */
  private void setCurrency(
      java.lang.String value) {
    java.lang.Class<?> valueClass = value.getClass();
  
    currency_ = value;
  }
  /**
   * <code>string currency = 3;</code>
   */
  private void clearCurrency() {

    currency_ = getDefaultInstance().getCurrency();
  }
  /**
   * <code>string currency = 3;</code>
   * @param value The bytes for currency to set.
   */
  private void setCurrencyBytes(
      com.google.protobuf.ByteString value) {
    checkByteStringIsUtf8(value);
    currency_ = value.toStringUtf8();

  }

  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      java.nio.ByteBuffer data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      java.nio.ByteBuffer data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      com.google.protobuf.ByteString data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      com.google.protobuf.ByteString data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(byte[] data)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      byte[] data,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws com.google.protobuf.InvalidProtocolBufferException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, data, extensionRegistry);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(java.io.InputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static com.pocketcasts.service.api.PaymentResponse parseDelimitedFrom(java.io.InputStream input)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input);
  }

  public static com.pocketcasts.service.api.PaymentResponse parseDelimitedFrom(
      java.io.InputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return parseDelimitedFrom(DEFAULT_INSTANCE, input, extensionRegistry);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      com.google.protobuf.CodedInputStream input)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input);
  }
  public static com.pocketcasts.service.api.PaymentResponse parseFrom(
      com.google.protobuf.CodedInputStream input,
      com.google.protobuf.ExtensionRegistryLite extensionRegistry)
      throws java.io.IOException {
    return com.google.protobuf.GeneratedMessageLite.parseFrom(
        DEFAULT_INSTANCE, input, extensionRegistry);
  }

  public static Builder newBuilder() {
    return (Builder) DEFAULT_INSTANCE.createBuilder();
  }
  public static Builder newBuilder(com.pocketcasts.service.api.PaymentResponse prototype) {
    return (Builder) DEFAULT_INSTANCE.createBuilder(prototype);
  }

  /**
   * Protobuf type {@code api.PaymentResponse}
   */
  public static final class Builder extends
      com.google.protobuf.GeneratedMessageLite.Builder<
        com.pocketcasts.service.api.PaymentResponse, Builder> implements
      // @@protoc_insertion_point(builder_implements:api.PaymentResponse)
      com.pocketcasts.service.api.PaymentResponseOrBuilder {
    // Construct using com.pocketcasts.service.api.PaymentResponse.newBuilder()
    private Builder() {
      super(DEFAULT_INSTANCE);
    }


    /**
     * <code>string payment_date = 1;</code>
     * @return The paymentDate.
     */
    @java.lang.Override
    public java.lang.String getPaymentDate() {
      return instance.getPaymentDate();
    }
    /**
     * <code>string payment_date = 1;</code>
     * @return The bytes for paymentDate.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getPaymentDateBytes() {
      return instance.getPaymentDateBytes();
    }
    /**
     * <code>string payment_date = 1;</code>
     * @param value The paymentDate to set.
     * @return This builder for chaining.
     */
    public Builder setPaymentDate(
        java.lang.String value) {
      copyOnWrite();
      instance.setPaymentDate(value);
      return this;
    }
    /**
     * <code>string payment_date = 1;</code>
     * @return This builder for chaining.
     */
    public Builder clearPaymentDate() {
      copyOnWrite();
      instance.clearPaymentDate();
      return this;
    }
    /**
     * <code>string payment_date = 1;</code>
     * @param value The bytes for paymentDate to set.
     * @return This builder for chaining.
     */
    public Builder setPaymentDateBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setPaymentDateBytes(value);
      return this;
    }

    /**
     * <code>double amount = 2;</code>
     * @return The amount.
     */
    @java.lang.Override
    public double getAmount() {
      return instance.getAmount();
    }
    /**
     * <code>double amount = 2;</code>
     * @param value The amount to set.
     * @return This builder for chaining.
     */
    public Builder setAmount(double value) {
      copyOnWrite();
      instance.setAmount(value);
      return this;
    }
    /**
     * <code>double amount = 2;</code>
     * @return This builder for chaining.
     */
    public Builder clearAmount() {
      copyOnWrite();
      instance.clearAmount();
      return this;
    }

    /**
     * <code>string currency = 3;</code>
     * @return The currency.
     */
    @java.lang.Override
    public java.lang.String getCurrency() {
      return instance.getCurrency();
    }
    /**
     * <code>string currency = 3;</code>
     * @return The bytes for currency.
     */
    @java.lang.Override
    public com.google.protobuf.ByteString
        getCurrencyBytes() {
      return instance.getCurrencyBytes();
    }
    /**
     * <code>string currency = 3;</code>
     * @param value The currency to set.
     * @return This builder for chaining.
     */
    public Builder setCurrency(
        java.lang.String value) {
      copyOnWrite();
      instance.setCurrency(value);
      return this;
    }
    /**
     * <code>string currency = 3;</code>
     * @return This builder for chaining.
     */
    public Builder clearCurrency() {
      copyOnWrite();
      instance.clearCurrency();
      return this;
    }
    /**
     * <code>string currency = 3;</code>
     * @param value The bytes for currency to set.
     * @return This builder for chaining.
     */
    public Builder setCurrencyBytes(
        com.google.protobuf.ByteString value) {
      copyOnWrite();
      instance.setCurrencyBytes(value);
      return this;
    }

    // @@protoc_insertion_point(builder_scope:api.PaymentResponse)
  }
  @java.lang.Override
  @java.lang.SuppressWarnings({"unchecked", "fallthrough"})
  protected final java.lang.Object dynamicMethod(
      com.google.protobuf.GeneratedMessageLite.MethodToInvoke method,
      java.lang.Object arg0, java.lang.Object arg1) {
    switch (method) {
      case NEW_MUTABLE_INSTANCE: {
        return new com.pocketcasts.service.api.PaymentResponse();
      }
      case NEW_BUILDER: {
        return new Builder();
      }
      case BUILD_MESSAGE_INFO: {
          java.lang.Object[] objects = new java.lang.Object[] {
            "paymentDate_",
            "amount_",
            "currency_",
          };
          java.lang.String info =
              "\u0000\u0003\u0000\u0000\u0001\u0003\u0003\u0000\u0000\u0000\u0001\u0208\u0002\u0000" +
              "\u0003\u0208";
          return newMessageInfo(DEFAULT_INSTANCE, info, objects);
      }
      // fall through
      case GET_DEFAULT_INSTANCE: {
        return DEFAULT_INSTANCE;
      }
      case GET_PARSER: {
        com.google.protobuf.Parser<com.pocketcasts.service.api.PaymentResponse> parser = PARSER;
        if (parser == null) {
          synchronized (com.pocketcasts.service.api.PaymentResponse.class) {
            parser = PARSER;
            if (parser == null) {
              parser =
                  new DefaultInstanceBasedParser<com.pocketcasts.service.api.PaymentResponse>(
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


  // @@protoc_insertion_point(class_scope:api.PaymentResponse)
  private static final com.pocketcasts.service.api.PaymentResponse DEFAULT_INSTANCE;
  static {
    PaymentResponse defaultInstance = new PaymentResponse();
    // New instances are implicitly immutable so no need to make
    // immutable.
    DEFAULT_INSTANCE = defaultInstance;
    com.google.protobuf.GeneratedMessageLite.registerDefaultInstance(
      PaymentResponse.class, defaultInstance);
  }

  public static com.pocketcasts.service.api.PaymentResponse getDefaultInstance() {
    return DEFAULT_INSTANCE;
  }

  private static volatile com.google.protobuf.Parser<PaymentResponse> PARSER;

  public static com.google.protobuf.Parser<PaymentResponse> parser() {
    return DEFAULT_INSTANCE.getParserForType();
  }
}

