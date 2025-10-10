package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import java.util.stream.Stream

private const val PATH = "/waste-streams"
private const val WASTE_STREAM_NUMBER = "08123ABCDEFG"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WasteStreamControllerSecurityTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreams

  private lateinit var testWasteStreamDto: WasteStreamDto

  @BeforeEach
  fun setup() {
    testWasteStreamDto = TestWasteStreamFactory.createTestWasteStreamDto(
      number = WASTE_STREAM_NUMBER,
      name = "Test Waste Stream"
    )
    wasteStreamRepository.save(testWasteStreamDto)
  }

  @AfterEach
  fun cleanup() {
    wasteStreamRepository.deleteAll()
  }

  companion object {
    @JvmStatic
    fun roleAccessScenarios(): Stream<Arguments> {
      return Stream.of(
        // GET all waste streams - any authenticated role can access
        Arguments.of(PATH, "GET", Roles.ADMIN, 200),
        Arguments.of(PATH, "GET", Roles.PLANNER, 200),
        Arguments.of(PATH, "GET", Roles.CHAUFFEUR, 200),
        Arguments.of(PATH, "GET", "unauthorized_role", 403),

        // POST (create) waste stream - any authenticated role can access
        Arguments.of(PATH, "POST", Roles.ADMIN, 200),
        Arguments.of(PATH, "POST", Roles.PLANNER, 200),
        Arguments.of(PATH, "POST", Roles.CHAUFFEUR, 200),
        Arguments.of(PATH, "POST", "unauthorized_role", 403),

        // PUT (update) waste stream - any authenticated role can access
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "PUT", Roles.ADMIN, 200),
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "PUT", Roles.PLANNER, 200),
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "PUT", Roles.CHAUFFEUR, 200),
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "PUT", "unauthorized_role", 403),

        // DELETE waste stream - any authenticated role can access
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "DELETE", Roles.ADMIN, 200),
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "DELETE", Roles.PLANNER, 200),
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "DELETE", Roles.CHAUFFEUR, 200),
        Arguments.of("$PATH/$WASTE_STREAM_NUMBER", "DELETE", "unauthorized_role", 403)
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
    val request = when (method) {
      "GET" -> get(endpoint)
      "POST" -> post(endpoint)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"number":"12001ZYXWVUT","name":"New Waste Stream"}""")

      "PUT" -> put(endpoint)
        .contentType(MediaType.APPLICATION_JSON)
        .content("""{"number":"$WASTE_STREAM_NUMBER","name":"Updated Waste Stream"}""")

      "DELETE" -> delete(endpoint)
      else -> throw IllegalArgumentException("Unsupported method: $method")
    }

    mockMvc.perform(
      request.with(
        jwt().authorities(SimpleGrantedAuthority(role))
      )
    ).andExpect(status().`is`(expectedStatus))
  }
}
