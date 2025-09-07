## Requirements
- JDK 17
- Docker

## Integration tests

All related tests are located in `com.checkout.payment.gateway.controller.PaymentGatewayControllerTest`. 
For the bank mocking I'm using WireMock stubs managed by TestContainers, to run tests execute:
```
gradle test
```

## Running the application

I've provided a Dockerfile to build the project and also it's used in the Docker Compose stack to run with the provided bank simulator (Mountebank).

Run it with:
``` 
docker compose up
```

And application api will be available at http://localhost:8090/, for convenience I've provided a collection of HTTP requests for manual testing, check the `requests.http` file, they could be executed from IntelliJ IDEA.

## Implementation details

I've added a simple concept of idempotency that's managed in the service layer:
- each request should include `Idempotency-Key` header`
- before payment request processing it should get a unique lock to prevent race conditions
- there is an idempotency state store to keep track of requests; it includes (an idempotency key, request, response, status: IN_PROGRESS, SUCCESS, FAILED)
- if a new request coming with the same idempotency key, but different body, it will be rejected with `422` status code
- if there is already successful request with same idempotency key and body, it will return the response from cache

As we don't have a database and multiple instances of the application, those guarantees are managed by locks and concurrent collections.

Currently, the full request is stored in the idempotency store. However, it will include things like full card number and CVV, so that is not what we want from a security perspective, instead we can use a hash of the request body to do a similar check.

Request validation is done with Bean Validation, e.g.:
```java
  @JsonProperty("card_number")
  @NotNull(message = "Card number is required")
  @Size(min = 14, max = 19, message = "Card number must be between 14 and 19 digits")
  @Pattern(regexp = "\\d+", message = "Card number must only contain numeric characters")
  private String cardNumber;
```

A custom validation for the future expiry date or a currency list is done in the controller layer. 