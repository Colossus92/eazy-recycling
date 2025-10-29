package nl.eazysoftware.eazyrecyclingservice.controller.user

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.service.UserService
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DriverControllerSecurityTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

  @MockitoBean
  @Suppress("unused")
  private lateinit var userService: UserService

    companion object {
      @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET all drivers - any role can access
                Arguments.of("/users/drivers", "GET", Roles.ADMIN, 200),
                Arguments.of("/users/drivers", "GET", Roles.PLANNER, 200),
                Arguments.of("/users/drivers", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/users/drivers", "GET", "unauthorized_role", 403)
            )
        }
    }

    @ParameterizedTest(name = "{1} {0} with role {2} should return {3}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for driver endpoints`(
        endpoint: String,
        method: String,
        role: String,
        expectedStatus: Int
    ) {
        val request = when (method) {
            "GET" -> get(endpoint)
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        mockMvc.perform(
            request.with(jwt().authorities(SimpleGrantedAuthority(role)))
        ).andExpect(status().`is`(expectedStatus))
    }
}
