# PostgreSQL Testcontainers Configuration

This package provides PostgreSQL Testcontainers integration for integration tests, replacing H2 with a real PostgreSQL database for better test fidelity.

## Why PostgreSQL Testcontainers?

### Problems with H2

-  **Dialect differences**: H2 doesn't support all PostgreSQL features (sequences, native queries, specific SQL syntax)
- **False positives**: Tests may pass with H2 but fail in production with PostgreSQL
- **Sequence management**: PostgreSQL-specific sequence operations fail with H2

### Benefits of Testcontainers

- ✅ **Production parity**: Tests run against the same database as production
- ✅ **PostgreSQL features**: Full support for sequences, native queries, and PostgreSQL-specific syntax
- ✅ **Isolation**: Each test suite gets a fresh database instance
- ✅ **CI/CD ready**: Works seamlessly in Docker-based CI environments

## Usage

### Option 1: Extend `BaseIntegrationTest`

The simplest way to use PostgreSQL Testcontainers:

```kotlin
class MyIntegrationTest : BaseIntegrationTest() {
  
  @Autowired
  private lateinit var myService: MyService
  
  @Test
  fun `should test something`() {
    // Your test code - automatic PostgreSQL database!
  }
}
```

### Option 2: Manual Configuration

If you need custom configuration:

```kotlin
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainersConfig::class)
@Transactional
class MyCustomTest {
  // Your tests
}
```

## Configuration

### `PostgresTestContainersConfig`

Provides a PostgreSQL container with:
- **Image**: `postgres:16-alpine` (lightweight, fast startup)
- **Database**: `testdb`
- **Credentials**: `test` / `test`
- **Container reuse**: Enabled for faster test execution

The `@ServiceConnection` annotation automatically configures Spring Boot's DataSource.

### `application-test.yml`

Updated to:
- Remove H2 datasource configuration (Testcontainers provides it)
- Use PostgreSQL dialect
- Disable SQL init mode (Hibernate creates schema)

## Performance

### Container Reuse

The PostgreSQL container is configured with `.withReuse(true)`, which means:
- **First test run**: Container is created (~2-5 seconds)
- **Subsequent runs**: Existing container is reused (~instant)
- Container persists until Docker daemon restarts

### Startup Time

- **Cold start**: ~3-5 seconds (container pull + startup)
- **Warm start**: <1 second (reused container)
- **CI/CD**: Similar to local (Docker layer caching)

## Troubleshooting

### Docker Not Running

```
Could not find a valid Docker environment
```

**Solution**: Ensure Docker is running on your machine.

### Port Conflicts

```
Bind for 0.0.0.0:XXXXX failed: port is already allocated
```

**Solution**: Testcontainers automatically assigns random ports. If you see this, restart Docker.

### Slow Startup

**Solution**: Enable container reuse (already configured). Ensure Docker has sufficient resources (4GB+ RAM recommended).

## Migrated Tests

The following test classes have been migrated to use `BaseIntegrationTest`:

- ✅ `TransportControllerIntegrationTest`
- ✅ `PlanningControllerIntegrationTest`
- ✅ `WasteContainerControllerIntegrationTest`
- ✅ `TruckControllerIntegrationTest`
- ✅ `CompanyControllerTest`
- ✅ Security tests (multiple)
- ✅ Use case tests

## Example: Sequence Generation

The `TransportDisplayNumberGeneratorIntegrationTest` demonstrates testing PostgreSQL sequences:

```kotlin
@Test
fun `should generate unique numbers under normal concurrent load`() {
  // Setup: Create sequence first (mimics real usage)
  generator.generateDisplayNumber()
  
  // Then test concurrent access
  val threadCount = 3
  val numbersPerThread = 3
  
  // Generate 9 numbers concurrently
  // PostgreSQL sequences guarantee uniqueness
}
```

**Key Pattern**: The test creates the sequence first, then tests concurrent generation. This mirrors real-world usage where the sequence is created by the first transport of the year, and all subsequent transports just use the existing sequence.

This test would **fail with H2** but **passes with PostgreSQL Testcontainers**.

### Note on Sequence Creation Race Condition

Sequence creation happens once per year (on January 1st). In the extremely rare case of concurrent transport creation at that exact moment, one transaction may fail due to a duplicate sequence creation attempt. This is an acceptable tradeoff for simplicity over complex transaction isolation handling.

In practice, the first transport of the year creates the sequence, and the remaining ~10,000+ transports throughout the year use it without any issues.

## Best Practices

1. **Extend `BaseIntegrationTest`**: Use the base class unless you have specific requirements
2. **Use `@Transactional`**: Ensures test isolation with automatic rollback
3. **Avoid manual cleanup**: Let transactions handle it
4. **Test concurrent scenarios**: Now possible with real PostgreSQL
5. **Keep container running**: Don't stop the container between test runs for better performance

## Dependencies

```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers:$springVersion")
testImplementation("org.testcontainers:postgresql:1.20.5")
testImplementation("org.testcontainers:junit-jupiter:1.20.5")
```
