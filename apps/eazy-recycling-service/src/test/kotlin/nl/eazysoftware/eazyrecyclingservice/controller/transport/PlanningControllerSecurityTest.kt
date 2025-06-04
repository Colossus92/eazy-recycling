package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.service.PlanningService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
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
import java.time.LocalDate
import java.util.*
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PlanningControllerSecurityTest {

    companion object {
        // Class-level constants for IDs that need to be accessed in companion object methods
        private val TEST_DRIVER_ID = UUID.randomUUID()
        private val TEST_TRUCK_ID = "TRUCK-123"
        private val TEST_DATE = LocalDate.now()

        @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET planning - admin and planner can access
                Arguments.of(Roles.ADMIN, 200),
                Arguments.of(Roles.PLANNER, 200),
                Arguments.of(Roles.CHAUFFEUR, 403),
                Arguments.of("unauthorized_role", 403)
            )
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var planningService: PlanningService

    private val testDriverId = TEST_DRIVER_ID
    private val testTruckId = TEST_TRUCK_ID
    private val testDate = TEST_DATE

    @BeforeEach
    fun setup() {
        // Mock the service call
        whenever(planningService.getPlanningByDate(any(), any(), any(), any())).thenReturn(
            PlanningView(
                dates = listOf(testDate.toString()),
                transports = listOf(
                    TransportsView(
                        truck = testTruckId,
                        transports = mapOf(
                            testDate.toString() to listOf(
                                TransportView(
                                    pickupDate = testDate.toString(),
                                    deliveryDate = testDate.plusDays(1).toString(),
                                    id = UUID.randomUUID().toString(),
                                    truck = Truck(licensePlate = "ABC123"),
                                    originCity = "Test Origin City",
                                    destinationCity = "Test Destination City",
                                    driver = ProfileDto(
                                        id = testDriverId,
                                        firstName = "Test",
                                        lastName = "Driver",
                                    ),
                                    status = TransportDto.Status.PLANNED,
                                    displayNumber = "123",
                                    containerId = UUID.randomUUID().toString(),
                                    transportType = TransportType.CONTAINER
                                )
                            )
                        )
                    )
                )
            )
        )
    }

    @ParameterizedTest(name = "GET /planning/date with role {0} should return {1}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for planning endpoint`(
        role: String,
        expectedStatus: Int
    ) {
        mockMvc.perform(
            get("/planning/${testDate}")
                .with(
                    jwt()
                        .authorities(SimpleGrantedAuthority(role))
                        .jwt { jwt -> jwt.claim("sub", testDriverId.toString()) }
                )
        ).andExpect(status().`is`(expectedStatus))
    }

    @ParameterizedTest(name = "GET /planning/date with role {0} and parameters should return {1}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for planning endpoint with parameters`(
        role: String,
        expectedStatus: Int
    ) {
        mockMvc.perform(
            get("/planning/${testDate}")
                .param("truckId", testTruckId)
                .param("driverId", testDriverId.toString())
                .param("status", "PLANNED")
                .with(
                    jwt()
                        .authorities(SimpleGrantedAuthority(role))
                        .jwt { jwt -> jwt.claim("sub", testDriverId.toString()) }
                )
        ).andExpect(status().`is`(expectedStatus))
    }
}
