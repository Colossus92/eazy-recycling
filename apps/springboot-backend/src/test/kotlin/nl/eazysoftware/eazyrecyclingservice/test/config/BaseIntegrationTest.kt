package nl.eazysoftware.eazyrecyclingservice.test.config

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.ToetsenAfvalstroomNummerServiceSoap
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Transactional

/**
 * Base class for integration tests using PostgreSQL Testcontainers.
 *
 * Provides:
 * - Real PostgreSQL database via Testcontainers
 * - MockMvc configuration for controller tests
 * - Test profile activation
 * - Transactional rollback for test isolation
 *
 * Usage:
 * ```kotlin
 * @SpringBootTest
 * class MyIntegrationTest : BaseIntegrationTest() {
 *     // Your tests here
 * }
 * ```
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostgresTestContainersConfig::class)
@Transactional
class BaseIntegrationTest {
  @MockitoBean
  private lateinit var wasteStreamValidator: ToetsenAfvalstroomNummerServiceSoap
}
