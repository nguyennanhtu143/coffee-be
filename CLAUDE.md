# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, test, run

Use the Maven wrapper (`mvnw.cmd` on Windows, `./mvnw` elsewhere) so everyone runs the same Maven version.

- `mvnw.cmd test` — run the JUnit 5 / Mockito suite (Surefire is configured with `--add-opens java.base/java.lang*` and `bytebuddy.experimental=true` so Mockito can mock Java 17 internals).
- `mvnw.cmd test -Dtest=OrderServiceTest` — single test class. Append `#methodName` for a single method.
- `mvnw.cmd clean package` — full build into `target/`.
- `mvnw.cmd spring-boot:run` — start the API; reads `src/main/resources/application.properties` (gitignored, machine-local).

The annotation-processor pipeline is order-sensitive: Lombok → `lombok-mapstruct-binding` → MapStruct, with `-Amapstruct.defaultComponentModel=spring`. If you add another processor, slot it in the same `<annotationProcessorPaths>` list in `pom.xml` rather than relying on classpath order.

## Architecture

Spring Boot 3.3 + Java 17 REST backend for a coffee ordering app. Layering is the conventional `controller → service → repository → entity` with MapStruct DTO mappers. Three things are non-obvious and worth knowing before editing:

### 1. Auth: stateless JWT, hand-rolled, with a static helper

`config/JwtAuthenticationFilter` parses `Authorization: Bearer <jwt>` on every request and seeds `SecurityContextHolder` with a `ROLE_USER` authority — but **services don't read the SecurityContext**. They take the raw `accessToken` header and call `TokenHelper.getUserIdFromToken(accessToken)` to extract `user_id`. `TokenHelper` is a Spring component that copies `@Value` fields into `static` slots in `@PostConstruct` so the static API works — keep it as a singleton; don't instantiate it manually in tests, mock the static call instead. Public endpoints are whitelisted explicitly in `SecurityConfig`; everything else requires a valid token.

Shop-vs-customer authorization is a per-call check inside services (`customRepository.getUserBy(userId).getIsShop()`), not a Spring Security role. New shop endpoints must repeat that check.

### 2. Order state machine via Strategy pattern

`service/order/StateGeneration` selects a `StateOrder` implementation by state string (`CANCELED` → `CancellationState`, everything else → `BaseState`) and delegates `getOrders(...)`. To support a new state with different listing behavior, add another `StateOrder` bean and extend `StateGeneration.findSateBy`. State string constants live in `common/Common.java` (`PENDING_PAYMENT`, `CONFIRMED`, `SHIPPING`, `DELIVERING`, `WAITING_DELIVERY`, `COMPLETED`, `CANCELED`, `DELIVERY_FAILED`, `RETURNING`, `RETURNED`, `RETURN_REFUND`) — use them, don't inline strings.

The same Strategy shape is used for payment: `service/order/payment/PaymentMethod.findPaymentMethod("cash" | other)` returns a `Payment` implementation. `cash` → `CashPayment`; anything else → `SePayPayment` (so `PayOSPayment` and `SePayPayment` co-exist but only SePay is wired into the dispatcher today).

### 3. SePay payment + cart-clearing invariant

Online orders **must not** clear the cart at order-creation time. `OrderService.orderProducts` stores the comma-separated `cartIds` on `UserOrderEntity.pendingCartIds` and only `PaymentService.confirmPayment` (called from the SePay webhook or polling check) deletes them. Cash orders clear the cart immediately. If you change order creation, preserve this branching or you'll either drop a paid customer's cart prematurely or leave stale items after payment.

`PaymentService` matches webhook/polling transactions against orders by scanning `transaction_content` for `${sepay.transfer-prefix}<orderId>` and requires `txAmount >= order.totalPrice`. The webhook endpoint and `/api/v1/order/cancel-unpaid` are public (no JWT) — they're called by SePay's servers and by the FE `beforeunload` handler respectively.

### Other things to know

- **Custom repository pattern**: `repository/CustomRepository` is a Spring-managed aggregator of `findById(...).orElseThrow(NotFoundException)` lookups. Prefer it over inlining the same `orElseThrow` in services.
- **Specifications, not Querydsl**: dynamic admin filters live in `repository/specification/` (e.g. `UserOrderSpecifications.adminOrders`). Add new admin filters there.
- **Error contract**: `exceptionhandler/GlobalExceptionHandler` maps `BadRequestException` → 400, `NotFoundException` → 404, `UnauthorizedException` → 401, `ForbiddenException` → 403, everything else → 500, all wrapped in `dto/ApiResponse<T>` with `success`/`message`/`data`/`timestamp`. Throw the typed exceptions; don't return error response entities by hand.
- **Shipping**: `service/shipping/GHNService` wraps the GHN HTTP API; `ShippingService` is the orchestration layer. Province/district/ward lookups and fee calculation are public (used by FE before login).
- **DBs**: both MySQL and PostgreSQL drivers are on the classpath; the active one is selected by `application.properties`.

## Conventions worth following

- 4-space indent. Lombok constructor injection (`@AllArgsConstructor` on services/controllers) — no field `@Autowired`.
- Suffixes: `*Controller`, `*Service`, `*Repository`, `*Entity`, `*Input`, `*Output`, `*Mapper`. MapStruct mappers are Spring beans (`defaultComponentModel=spring`) — inject them, don't call `Mappers.getMapper(...)`.
- Keep controllers thin: validation annotations + delegation. State transitions, payment branching, shipping integration, and repository orchestration belong in services.
- Tests follow `Class_scenario_result` naming, e.g. `cancelOrder_notOwner_fail`, in the matching package under `src/test/java`.
- Do not commit `application.properties`, secrets, JWT keys, Cloudinary creds, PayOS keys, GHN tokens, or the `target/` directory.
