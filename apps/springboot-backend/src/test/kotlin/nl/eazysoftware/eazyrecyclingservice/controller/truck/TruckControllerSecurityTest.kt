package nl.eazysoftware.eazyrecyclingservice.controller.truck

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
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
class TruckControllerSecurityTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var truckRepository: TruckRepository

    private val testTruck = TruckDto(
        licensePlate = "TEST-123",
        brand = "Test Brand",
        model = "Test Model"
    )

    @BeforeEach
    fun setup() {
        truckRepository.save(testTruck)
    }

    @AfterEach
    fun cleanup() {
        truckRepository.deleteAll()
    }

    companion object {
        @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET all trucks
                Arguments.of("/trucks", "GET", Roles.ADMIN, 200),
                Arguments.of("/trucks", "GET", Roles.PLANNER, 200),
                Arguments.of("/trucks", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/trucks", "GET", "unauthorized_role", 403),

                // GET truck by license plate
                Arguments.of("/trucks/TEST-123", "GET", Roles.ADMIN, 200),
                Arguments.of("/trucks/TEST-123", "GET", Roles.PLANNER, 200),
                Arguments.of("/trucks/TEST-123", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/trucks/TEST-123", "GET", "unauthorized_role", 403),

                // POST (create) truck
                Arguments.of("/trucks", "POST", Roles.ADMIN, 201),
                Arguments.of("/trucks", "POST", Roles.PLANNER, 201),
                Arguments.of("/trucks", "POST", Roles.CHAUFFEUR, 403),
                Arguments.of("/trucks", "POST", "unauthorized_role", 403),

                // PUT (update) truck
                Arguments.of("/trucks/TEST-123", "PUT", Roles.ADMIN, 200),
                Arguments.of("/trucks/TEST-123", "PUT", Roles.PLANNER, 200),
                Arguments.of("/trucks/TEST-123", "PUT", Roles.CHAUFFEUR, 403),
                Arguments.of("/trucks/TEST-123", "PUT", "unauthorized_role", 403),

                // DELETE truck
                Arguments.of("/trucks/TEST-123", "DELETE", Roles.ADMIN, 204),
                Arguments.of("/trucks/TEST-123", "DELETE", Roles.PLANNER, 204),
                Arguments.of("/trucks/TEST-123", "DELETE", Roles.CHAUFFEUR, 403),
                Arguments.of("/trucks/TEST-123", "DELETE", "unauthorized_role", 403)
            )
        }
    }

    @ParameterizedTest(name = "{1} {0} with role {2} should return {3}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for truck endpoints`(
        endpoint: String,
        method: String,
        role: String,
        expectedStatus: Int
    ) {
        val request = when (method) {
            "GET" -> get(endpoint)
            "POST" -> post(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TruckDto(
                    licensePlate = "NEW-123",
                    brand = "New Brand",
                    model = "New Model"
                )))
            "PUT" -> put(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testTruck.copy(
                    brand = "Updated Brand"
                )))
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
