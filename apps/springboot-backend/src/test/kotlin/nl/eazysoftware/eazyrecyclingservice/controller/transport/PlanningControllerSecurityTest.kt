package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.domain.service.PlanningService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
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

        @JvmStatic
        fun driverPlanningAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(Roles.ADMIN, TEST_DRIVER_ID, null, 200),
                Arguments.of(Roles.PLANNER, TEST_DRIVER_ID, null, 200),
                Arguments.of(Roles.CHAUFFEUR, TEST_DRIVER_ID, TEST_DRIVER_ID, 200), // Chauffeur accessing own planning
//                Arguments.of(Roles.CHAUFFEUR, UUID.randomUUID(), TEST_DRIVER_ID, 403), // Chauffeur accessing another driver's planning
                Arguments.of("unauthorized_role", TEST_DRIVER_ID, null, 403)
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
                                    transportType = TransportType.CONTAINER,
                                    sequenceNumber = 1,
                                )
                            )
                        )
                    )
                )
            )
        )

        // Mock the service call for driver planning
        whenever(planningService.getPlanningByDriver(any(), any(), any())).thenReturn(
            mapOf(
                testDate to mapOf(
                    testTruckId to listOf(
                        DriverPlanningItem(
                            id = UUID.randomUUID(),
                            displayNumber = "123",
                            pickupDateTime = testDate.atTime(10, 0),
                            deliveryDateTime = testDate.atTime(12, 0),
                            pickupLocation = testDeliveryLocation(),
                            deliveryLocation = testDeliveryLocation(),
                            containerId = "CONT-001",
                            status = TransportDto.Status.PLANNED
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

    @ParameterizedTest(name = "Role {0} accessing driver planning: expected status {3}")
    @MethodSource("driverPlanningAccessScenarios")
    fun `should verify role-based access control for driver planning endpoint`(
        role: String,
        requestedDriverId: UUID,
        userSubjectId: UUID?,
        expectedStatus: Int
    ) {
        mockMvc.perform(
            get("/planning/driver/${requestedDriverId}")
                .param("startDate", testDate.toString())
                .param("endDate", testDate.plusDays(7).toString())
                .with(
                    jwt()
                        .authorities(SimpleGrantedAuthority(role))
                        .jwt { jwt ->
                            if (userSubjectId != null) {
                                jwt.claim("sub", userSubjectId.toString())
                            }
                        }
                )
        ).andExpect(status().`is`(expectedStatus))
    }

    private fun testDeliveryLocation() = nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto(
        id = UUID.randomUUID().toString(),
        address = nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto(
            streetName = "Test Street",
            buildingNumber = "123",
            postalCode = "1234AB",
            city = "Test City",
            country = "Test Country"
        )
    )
}
