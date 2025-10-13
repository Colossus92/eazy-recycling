package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory.randomWasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import org.junit.jupiter.api.*
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.util.stream.Stream

private const val PATH = "/waste-streams"

@Disabled
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WasteStreamControllerSecurityTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreamJpaRepository

  private lateinit var testWasteStreamDto: WasteStreamDto

  @Autowired
  private lateinit var companyRepository: CompanyRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  @PersistenceContext
  private lateinit var entityManager: EntityManager

  private lateinit var processorCompany: CompanyDto
  private lateinit var consignorCompany: CompanyDto
  private lateinit var pickupCompany: CompanyDto

  @BeforeAll
  fun setupOnce() {
    transactionTemplate.execute {
      processorCompany = companyRepository.save(
        TestCompanyFactory.createTestCompany(processorId = "12345")
      )
      consignorCompany = companyRepository.save(
        TestCompanyFactory.createTestCompany(
          chamberOfCommerceId = "87654321",
          vihbId = "123456VIHB"
        )
      )
      pickupCompany = companyRepository.save(
        TestCompanyFactory.createTestCompany(
          chamberOfCommerceId = "98765432",
          vihbId = "123457VIHB"
        )
      )
    }
  }

  @AfterAll
  fun cleanUpOnce() {
    companyRepository.deleteAll()
  }

  @BeforeEach
  fun setup() {
    // Generate a unique number for each test to avoid collisions in parallel execution
    val uniqueNumber = randomWasteStreamNumber()
    testWasteStreamDto = TestWasteStreamFactory.createTestWasteStreamDto(
      number = uniqueNumber,
      name = "Test Waste Stream",
      processorPartyId = processorCompany,
      consignorParty = consignorCompany,
      pickupParty = pickupCompany
    )
    wasteStreamRepository.saveAndFlush(testWasteStreamDto)
  }

  @AfterEach
  fun cleanup() {
    // Delete all waste streams first
    wasteStreamRepository.deleteAll()
    wasteStreamRepository.flush()

    // Clear any persistence context to prevent issues with detached entities
    entityManager.clear()
  }

  companion object {
    private const val PLACEHOLDER = "{number}"

    @JvmStatic
    fun roleAccessScenarios(): Stream<Arguments> {
      return Stream.of(
        // GET all waste streams - any authenticated role can access
        Arguments.of(PATH, "GET", Roles.ADMIN, 200),
        Arguments.of(PATH, "GET", Roles.PLANNER, 200),
        Arguments.of(PATH, "GET", Roles.CHAUFFEUR, 200),
        Arguments.of(PATH, "GET", "unauthorized_role", 403),

        // GET waste stream by number - any authenticated role can access
        Arguments.of("$PATH/$PLACEHOLDER", "GET", Roles.ADMIN, 200),
        Arguments.of("$PATH/$PLACEHOLDER", "GET", Roles.PLANNER, 200),
        Arguments.of("$PATH/$PLACEHOLDER", "GET", Roles.CHAUFFEUR, 200),
        Arguments.of("$PATH/$PLACEHOLDER", "GET", "unauthorized_role", 403),

        // POST (create) waste stream - any authenticated role can access
        Arguments.of(PATH, "POST", Roles.ADMIN, 201),
        Arguments.of(PATH, "POST", Roles.PLANNER, 201),
        Arguments.of(PATH, "POST", Roles.CHAUFFEUR, 201),
        Arguments.of(PATH, "POST", "unauthorized_role", 403),

        // PUT (update) waste stream - any authenticated role can access
        Arguments.of("$PATH/$PLACEHOLDER", "PUT", Roles.ADMIN, 204),
        Arguments.of("$PATH/$PLACEHOLDER", "PUT", Roles.PLANNER, 204),
        Arguments.of("$PATH/$PLACEHOLDER", "PUT", Roles.CHAUFFEUR, 204),
        Arguments.of("$PATH/$PLACEHOLDER", "PUT", "unauthorized_role", 403),

        // DELETE waste stream - any authenticated role can access
        Arguments.of("$PATH/$PLACEHOLDER", "DELETE", Roles.ADMIN, 204),
        Arguments.of("$PATH/$PLACEHOLDER", "DELETE", Roles.PLANNER, 204),
        Arguments.of("$PATH/$PLACEHOLDER", "DELETE", Roles.CHAUFFEUR, 204),
        Arguments.of("$PATH/$PLACEHOLDER", "DELETE", "unauthorized_role", 403)
      )
    }
  }

  @ParameterizedTest(name = "{1} {0} with role {2} should return {3}")
  @MethodSource("roleAccessScenarios")
  fun `should verify role-based access control for waste stream endpoints`(
    endpoint: String,
    method: String,
    role: String,
    expectedStatus: Int
  ) {
    // Replace placeholder with actual waste stream number from test data
    val actualEndpoint = endpoint.replace("{number}", testWasteStreamDto.number)

    val request = when (method) {
      "GET" -> get(actualEndpoint)
      "POST" -> {
        val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
          companyId = consignorCompany.id!!,
          name = "New Waste Stream",
          processorPartyId = "12345",
          pickupParty = pickupCompany.id!!
        )
        post(actualEndpoint)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(wasteStreamRequest))
      }

      "PUT" -> {
        val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
          companyId = consignorCompany.id!!,
          name = "Updated Waste Stream",
          processorPartyId = "12345",
          pickupParty = pickupCompany.id!!
        )
        put(actualEndpoint)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(wasteStreamRequest))
      }

      "DELETE" -> delete(actualEndpoint)
      else -> throw IllegalArgumentException("Unsupported method: $method")
    }

    mockMvc.perform(
      request.with(
        jwt().authorities(SimpleGrantedAuthority(role))
      )
    ).andExpect(status().`is`(expectedStatus))
  }
}
