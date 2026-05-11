# Repository Guidelines

## Project Structure & Module Organization

This is a Java 17 Spring Boot backend for a coffee ordering API. Source code lives under `src/main/java/org/example/coffee`:

- `controller/` exposes REST endpoints.
- `service/` contains business logic, with focused subpackages such as `service/order`, `service/order/payment`, and `service/shipping`.
- `repository/` contains Spring Data and custom query access.
- `entity/`, `dto/`, and `mapper/` hold persistence models, request/response models, and MapStruct mappings.
- `config/`, `exceptionhandler/`, `token/`, `helper/`, and `cloudinary/` hold infrastructure code.

Tests live in `src/test/java/org/example/coffee`. Generated build output goes to `target/` and should not be committed. Local runtime configuration is expected in `src/main/resources/application.properties`, which is ignored by Git.

## Build, Test, and Development Commands

Use the Maven wrapper so contributors run the same Maven version:

- `./mvnw test` or `mvnw.cmd test`: run the JUnit test suite with Surefire.
- `./mvnw clean package` or `mvnw.cmd clean package`: compile, test, and build the application artifact in `target/`.
- `./mvnw spring-boot:run` or `mvnw.cmd spring-boot:run`: start the API locally using your `application.properties`.

## Coding Style & Naming Conventions

Use 4-space indentation for Java. Keep package names lowercase and classes in PascalCase. Follow existing suffixes: `*Controller`, `*Service`, `*Repository`, `*Entity`, `*Input`, `*Output`, and `*Mapper`. Prefer constructor injection through Lombok annotations such as `@AllArgsConstructor`. Keep controllers thin; put validation, state transitions, payment, shipping, and repository orchestration in services.

MapStruct is configured with `defaultComponentModel=spring`, so mapper interfaces should be Spring-injectable. Avoid committing generated files from `target/`.

## Testing Guidelines

The project uses Spring Boot Test, JUnit 5, Mockito, and Maven Surefire. Name tests after the class under test, for example `OrderServiceTest`, and place them in the matching package under `src/test/java`. Use method names that describe the scenario and result, such as `cancelOrder_notOwner_fail`. Add focused unit tests for service logic and state transitions.

## Commit & Pull Request Guidelines

The current Git history only shows `initial commit`, so no strict convention is established. Use short, imperative commit subjects such as `Add order cancellation validation` or `Fix product size mapping`.

Pull requests should include a concise summary, affected endpoints or services, test results from `./mvnw test`, and any configuration changes. Link related issues when available. Include request/response examples when API behavior changes.

## Security & Configuration Tips

Do not commit secrets, database URLs, JWT keys, Cloudinary credentials, PayOS keys, or GHN tokens. Keep machine-specific values in ignored local configuration and document required property names in PR notes when adding new settings.
