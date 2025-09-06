package com.checkout.payment.gateway.client.model;

public class PostPaymentResponseBuilder {
  private Boolean authorized;
  private String authorizationCode;

  public static PostPaymentResponseBuilder builder() {
    return new PostPaymentResponseBuilder();
  }

  public PostPaymentResponseBuilder authorized(Boolean authorized) {
    this.authorized = authorized;
    return this;
  }

  public PostPaymentResponseBuilder authorizationCode(String authorizationCode) {
    this.authorizationCode = authorizationCode;
    return this;
  }

  public PostPaymentResponse build() {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setAuthorized(this.authorized);
    response.setAuthorizationCode(this.authorizationCode);
    return response;
  }
}
