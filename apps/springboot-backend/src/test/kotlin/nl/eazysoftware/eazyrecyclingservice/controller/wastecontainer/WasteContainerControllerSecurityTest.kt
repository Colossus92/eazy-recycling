package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WasteContainerControllerSecurityTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var wasteContainerRepository: WasteContainerRepository

    private lateinit var testContainerId: String
    private lateinit var testContainer: WasteContainerDto

    @BeforeEach
    fun setup() {
        testContainer = WasteContainerDto(
            id = "Test Container",
            notes = "Test container"
        )
        val savedContainer = wasteContainerRepository.save(testContainer)
        testContainerId = savedContainer.id
    }

    @AfterEach
    fun cleanup() {
        wasteContainerRepository.deleteAll()
    }

    companion object {
        @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET all containers - any role can access
                Arguments.of("/containers", "GET", Roles.ADMIN, 200),
                Arguments.of("/containers", "GET", Roles.PLANNER, 200),
                Arguments.of("/containers", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/containers", "GET", "unauthorized_role", 403),

                // GET container by id - any role can access
                Arguments.of("/containers/{id}", "GET", Roles.ADMIN, 200),
                Arguments.of("/containers/{id}", "GET", Roles.PLANNER, 200),
                Arguments.of("/containers/{id}", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/containers/{id}", "GET", "unauthorized_role", 403),

                // POST (create) container - only admin and planner can access
                Arguments.of("/containers", "POST", Roles.ADMIN, 201),
                Arguments.of("/containers", "POST", Roles.PLANNER, 201),
                Arguments.of("/containers", "POST", Roles.CHAUFFEUR, 403),
                Arguments.of("/containers", "POST", "unauthorized_role", 403),

                // PUT (update) container - any role can access
                Arguments.of("/containers/{id}", "PUT", Roles.ADMIN, 200),
                Arguments.of("/containers/{id}", "PUT", Roles.PLANNER, 200),
                Arguments.of("/containers/{id}", "PUT", Roles.CHAUFFEUR, 200),
                Arguments.of("/containers/{id}", "PUT", "unauthorized_role", 403),

                // DELETE container - only admin and planner can access
                Arguments.of("/containers/{id}", "DELETE", Roles.ADMIN, 204),
                Arguments.of("/containers/{id}", "DELETE", Roles.PLANNER, 204),
                Arguments.of("/containers/{id}", "DELETE", Roles.CHAUFFEUR, 403),
                Arguments.of("/containers/{id}", "DELETE", "unauthorized_role", 403)
            )
        }
    }

    @ParameterizedTest(name = "{1} {0} with role {2} should return {3}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for container endpoints`(
        endpoint: String,
        method: String,
        role: String,
        expectedStatus: Int
    ) {
        // Replace {id} placeholder with actual ID
        val resolvedEndpoint = endpoint.replace("{id}", testContainerId)

        val request = when (method) {
            "GET" -> get(resolvedEndpoint)
            "POST" -> post(resolvedEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"New Container"}""")
            "PUT" -> put(resolvedEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"uuid":"$testContainerId","id":"${testContainer.id}","notes":"Updated Container"}""")
            "DELETE" -> delete(resolvedEndpoint)
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        mockMvc.perform(
            request.with(
                jwt().authorities(SimpleGrantedAuthority(role))
            )
        ).andExpect(status().`is`(expectedStatus))
    }
}
