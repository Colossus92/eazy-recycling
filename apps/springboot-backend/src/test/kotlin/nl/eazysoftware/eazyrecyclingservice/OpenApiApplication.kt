package nl.eazysoftware.eazyrecyclingservice

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

/**
 * Application entry point for the OpenAPI profile.
 * 
 * This class starts the application with a PostgreSQL testcontainer,
 * allowing OpenAPI documentation to be generated with a real database.
 * 
 * Usage:
 *   ./gradlew bootTestRun --args='--spring.profiles.active=openapi'
 * 
 * Or run this class directly from your IDE with the 'openapi' profile.
 */
fun main(args: Array<String>) {
    fromApplication<Application>()
        .with(OpenApiTestcontainersConfiguration::class)
        .run(*args)
}

@TestConfiguration(proxyBeanMethods = false)
class OpenApiTestcontainersConfiguration {

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("openapidb")
            .withUsername("openapi")
            .withPassword("openapi")
            .withReuse(true) // Reuse container for faster restarts during development
    }
}
