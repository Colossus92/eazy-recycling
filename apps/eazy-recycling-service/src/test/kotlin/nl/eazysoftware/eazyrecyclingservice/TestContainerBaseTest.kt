package nl.eazysoftware.eazyrecyclingservice

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(
    properties = ["spring.profiles.active=test"],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
abstract class TestContainerBaseTest {

    companion object {
        private val postgresContainer = PostgreSQLContainer("postgres:16-alpine").apply {
            withDatabaseName("test_db")
            withUsername("test_user")
            withPassword("test_pass")
            start()
        }

        @JvmStatic
        @DynamicPropertySource
        @Suppress("unused")
        fun configureProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}