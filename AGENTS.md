# Conventions for AI Agents and human developers

## Main Frameworks and Tools

- Use Maven (see pom.xml) as the build tool, avoid using other build tools.
  - We do not use Maven Wrapper (`mvnw`), `mvn` should be installed on the system.
  - Always use properties to define versions of dependencies in the `pom.xml`, avoid using hard-coded versions.
- Use Git for version control. It is installed on the system.
  - Use trunk-based development on the `main` branch for small changes, e.g. bug fixes.
  - Use feature branches for larger new features, e.g. `my-new-feature`.
- Use Java as the programming language, avoid using other programming languages.
- Use the Java SDK version defined by the pom.xml.
- Use Spring Boot as the main framework (see pom.xml for the version).
- Do not use Spring WebFlux and Project Reactor in this lib.

## General rules on programming

- Use UTF-8 encoding for all source files.
- Use camelCase for variable and method names, e.g. `myVariable`, `myMethod()`.
- Use PascalCase for class and interface names, e.g. `MyClass`, `MyInterface`.
- Consider [.editorconfig](./.editorconfig) for general formatting rules.
- Remove unused imports automatically.
- Avoid using `var` for local variables.
- Use `final` for method parameters and local variables whenever possible.
- Do not use `this.` for instance variables unless necessary.
- Use `@Override` annotation when overriding methods.
- Replace anonymous classes with lambda expressions where applicable.
- Do not use the Java keyword assert, neither in tests nor in production code.
- Do not use `@Lombok`.
- Do not use `@Slf4j` for logging.
- Use Java records for immutable data classes.
- Use `private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class);` for logging.

## Detailed Spring related conventions

- Use constructor injection for dependencies.
- Use Spring Actuator (see pom.xml) for monitoring and management, avoid using other monitoring libraries.
- Use Jackson (see pom.xml) for JSON serialization and deserialization, avoid using other JSON libraries.
- Use `@ConfigurationProperties` for configuration classes.
  All application properties should be added to `ApplicationProperties.java` or its nested classes.
- Avoid using `@Value` for simple values, e.g. `@Value("${myValue}")`.
- Use Spring Web annotations for HTTP services, e.g. `@RestController`, `@GetMapping`, `@PostMapping`, etc.
- Use `@Service` for service classes and `@Component` for other components.
- Use `ResponseEntity` for HTTP responses.

## Testing conventions

- Use JUnit 5 (see pom.xml) for tests.
- Whenever possible, use AssertJ (see pom.xml) for assertions in tests.
- Avoid using mockito or other mocking libraries!
- Use Testcontainers for integration tests with external dependencies, e.g. S3.
  Use the pinned official `adobe/s3mock:5.2.3` image for S3 integration tests.
- Use AAA pattern for tests (Arrange, Act, Assert) and separate the three sections with a
  three slash comment line, like  `// arrange`, `// act`, `// assert`.

### Naming conventions for tests

- Normal unit test use the Suffix 'Test', `MyServiceTest`.
- Integration tests with `@SpringBootTest` use the suffix `IT`, e.g. `MyServiceIT`.
