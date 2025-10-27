package nl.eazysoftware.eazyrecyclingservice.test.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Test configuration that provides a PostgreSQL Testcontainer for integration tests.
 * 
 * This replaces H2 with a real PostgreSQL database to ensure:
 * - Tests run against the same database as production
 * - PostgreSQL-specific features (sequences, native queries) work correctly
 * - Better test fidelity and confidence
 * 
 * The container is shared across all tests for performance.
 */
@TestConfiguration(proxyBeanMethods = false)
class PostgresTestContainersConfig {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")
            .withReuse(true) // Reuse container across test runs for speed
    }
}
