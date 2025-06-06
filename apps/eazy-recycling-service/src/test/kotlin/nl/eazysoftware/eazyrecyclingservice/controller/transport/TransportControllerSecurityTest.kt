package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.UserRoleDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransportControllerSecurityTest {

    companion object {
        // Class-level constants for IDs that need to be accessed in companion object methods
        private val TEST_TRANSPORT_ID = UUID.randomUUID()
        private val TEST_DRIVER_ID = UUID.randomUUID()
        private val OTHER_DRIVER_ID = UUID.randomUUID()

        @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET all transports - admin and planner can access
                Arguments.of("/transport", "GET", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport", "GET", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport", "GET", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 403),
                Arguments.of("/transport", "GET", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // POST waybill transport - admin and planner can access
                Arguments.of("/transport/waybill", "POST", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waybill", "POST", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waybill", "POST", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 403),
                Arguments.of("/transport/waybill", "POST", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // POST container transport - admin and planner can access
                Arguments.of("/transport/container", "POST", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/container", "POST", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/container", "POST", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 403),
                Arguments.of("/transport/container", "POST", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // POST waste transport - admin and planner can access
                Arguments.of("/transport/waste", "POST", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waste", "POST", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waste", "POST", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 403),
                Arguments.of("/transport/waste", "POST", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // PUT container transport - any role can access but authorization is checked after
                Arguments.of("/transport/container/{id}", "PUT", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/container/{id}", "PUT", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/container/{id}", "PUT", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/container/{id}", "PUT", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // PUT waste transport - any role can access but authorization is checked after
                Arguments.of("/transport/waste/{id}", "PUT", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waste/{id}", "PUT", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waste/{id}", "PUT", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 200),
                Arguments.of("/transport/waste/{id}", "PUT", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // DELETE transport - admin and planner can access
                Arguments.of("/transport/{id}", "DELETE", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 204),
                Arguments.of("/transport/{id}", "DELETE", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 204),
                Arguments.of("/transport/{id}", "DELETE", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 403),
                Arguments.of("/transport/{id}", "DELETE", "unauthorized_role", UUID.randomUUID().toString(), 403)
            )
        }

        @JvmStatic
        fun transportAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // Admin can access any transport
                Arguments.of(Roles.ADMIN, TEST_DRIVER_ID.toString(), TEST_DRIVER_ID.toString(), 200),
                Arguments.of(Roles.ADMIN, TEST_DRIVER_ID.toString(), OTHER_DRIVER_ID.toString(), 200),

                // Planner can access any transport
                Arguments.of(Roles.PLANNER, TEST_DRIVER_ID.toString(), TEST_DRIVER_ID.toString(), 200),
                Arguments.of(Roles.PLANNER, TEST_DRIVER_ID.toString(), OTHER_DRIVER_ID.toString(), 200),

                // Chauffeur can only access their own transports
                Arguments.of(Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), TEST_DRIVER_ID.toString(), 200),
                Arguments.of(Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), OTHER_DRIVER_ID.toString(), 403),

                // Unauthorized role cannot access any transport
                Arguments.of("unauthorized_role", TEST_DRIVER_ID.toString(), TEST_DRIVER_ID.toString(), 403),
                Arguments.of("unauthorized_role", TEST_DRIVER_ID.toString(), OTHER_DRIVER_ID.toString(), 403)
            )
        }
    }

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var transportService: TransportService

    // Use the companion object constants instead of local variables
    private val testTransportId = TEST_TRANSPORT_ID
    private val testDriverId = TEST_DRIVER_ID
    private val otherDriverId = OTHER_DRIVER_ID

    private lateinit var transportWithTestDriver: TransportDto
    private lateinit var transportWithOtherDriver: TransportDto

    @BeforeEach
    fun setup() {
        // Create transport with the test driver
        transportWithTestDriver = TransportDto(
            id = testTransportId,
            driver = ProfileDto(
                id = testDriverId,
                firstName = "Test",
                lastName = "Driver",
                roles = listOf(UserRoleDto(id = 1, userId = testDriverId, role = Roles.CHAUFFEUR))
            ),
            consignorParty = mockCompanyDto(),
            carrierParty = mockCompanyDto(),
            pickupLocation = mockLocationDto(),
            deliveryLocation = mockLocationDto(),
            pickupDateTime = LocalDateTime.now(),
            deliveryDateTime = LocalDateTime.now().plusDays(1),
            transportType = TransportType.CONTAINER,
            note = "Test transport",
            pickupCompany = mockCompanyDto(),
            deliveryCompany = mockCompanyDto(),
            containerOperation = ContainerOperation.DELIVERY,
            sequenceNumber = 1,
        )

        // Create transport with a different driver
        transportWithOtherDriver = transportWithTestDriver.copy(
            id = UUID.randomUUID(),
            driver = ProfileDto(
                id = otherDriverId,
                firstName = "Second",
                lastName = "Driver",
                roles = listOf(UserRoleDto(id = 2, userId = otherDriverId, role = Roles.CHAUFFEUR))
            ),
        )

        // Mock the service calls
        whenever(transportService.getTransportById(testTransportId)).thenReturn(transportWithTestDriver)
        whenever(transportService.updateContainerTransport(eq(testTransportId), any())).thenReturn(
            transportWithTestDriver
        )
        whenever(transportService.updateWasteTransport(eq(testTransportId), any())).thenReturn(transportWithTestDriver)
        whenever(transportService.getTransportById(transportWithOtherDriver.id!!)).thenReturn(transportWithOtherDriver)
        whenever(transportService.getAllTransports()).thenReturn(
            listOf(
                transportWithTestDriver,
                transportWithOtherDriver
            )
        )
    }

    // Helper method to create mock objects
    private fun mockCompanyDto() = CompanyDto(
        id = UUID.randomUUID(),
        name = "Test Company",
        address = AddressDto(
            streetName = "Test Street",
            buildingNumber = "1",
            postalCode = "1234AB",
            city = "Test City",
            country = "Test Country"
        )
    )

    private fun mockLocationDto() = LocationDto(
        id = UUID.randomUUID().toString(),
        address = AddressDto(
            streetName = "Test Street",
            buildingNumber = "1",
            postalCode = "1234AB",
            city = "Test City",
            country = "Test Country"
        )
    )

    private fun createContainerTransportRequestJson(): String {
        val request = CreateContainerTransportRequest(
            consignorPartyId = UUID.randomUUID(),
            pickupDateTime = LocalDateTime.now(),
            deliveryDateTime = LocalDateTime.now().plusDays(1),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.DELIVERY,
            driverId = testDriverId,
            carrierPartyId = UUID.randomUUID(),
            pickupCompanyId = UUID.randomUUID(),
            pickupStreet = "Pickup Street",
            pickupBuildingNumber = "1",
            pickupPostalCode = "1234AB",
            pickupCity = "Pickup City",
            deliveryCompanyId = UUID.randomUUID(),
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "2",
            deliveryPostalCode = "5678CD",
            deliveryCity = "Delivery City",
            truckId = "TRUCK-123",
            containerId = UUID.randomUUID(),
            note = "Test container transport"
        )

        return objectMapper.writeValueAsString(request)
    }

    private fun createWasteTransportRequestJson(): String {
        val request = CreateWasteTransportRequest(
            consigneePartyId = UUID.randomUUID().toString(),
            pickupPartyId = UUID.randomUUID().toString(),
            consignorPartyId = UUID.randomUUID(),
            pickupDateTime = LocalDateTime.now(),
            deliveryDateTime = LocalDateTime.now().plusDays(1),
            containerOperation = ContainerOperation.PICKUP,
            transportType = TransportType.WASTE,
            driverId = testDriverId,
            carrierPartyId = UUID.randomUUID(),
            pickupCompanyId = UUID.randomUUID(),
            pickupStreet = "Pickup Street",
            pickupBuildingNumber = "1",
            pickupPostalCode = "1234AB",
            pickupCity = "Pickup City",
            deliveryCompanyId = UUID.randomUUID(),
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "2",
            deliveryPostalCode = "5678CD",
            deliveryCity = "Delivery City",
            truckId = "TRUCK-123",
            containerId = UUID.randomUUID(),
            note = "Test waste transport",
            wasteStreamNumber = "WASTE-123",
            weight = 10,
            unit = "KG",
            quantity = 1,
            euralCode = "EURAL-123",
            goodsName = "Test Goods"
        )

        return objectMapper.writeValueAsString(request)
    }

    @ParameterizedTest(name = "{1} {0} with role {2} should return {4}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for transport endpoints`(
        endpoint: String,
        method: String,
        role: String,
        subClaim: String?,
        expectedStatus: Int
    ) {
        val actualEndpoint = if (endpoint.contains("{id}")) {
            endpoint.replace("{id}", testTransportId.toString())
        } else {
            endpoint
        }

        val request = when (method) {
            "GET" -> get(actualEndpoint)
            "POST" -> {
                when (actualEndpoint) {
                    "/transport/container" -> post(actualEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createContainerTransportRequestJson())

                    "/transport/waste" -> post(actualEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWasteTransportRequestJson())

                    "/transport/waybill" -> post(actualEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"waybillId":"${UUID.randomUUID()}","licensePlate":"ABC-123","driverId":"${UUID.randomUUID()}"}""")

                    else -> throw IllegalArgumentException("Unsupported endpoint: $endpoint")
                }
            }

            "PUT" -> {
                when {
                    actualEndpoint.contains("/transport/container/") -> put(actualEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createContainerTransportRequestJson())

                    actualEndpoint.contains("/transport/waste/") -> put(actualEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createWasteTransportRequestJson())

                    actualEndpoint.contains("/transport/waybill") -> post(actualEndpoint)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"waybillId":"${UUID.randomUUID()}","licensePlate":"ABC-123","driverId":"${UUID.randomUUID()}"}""")

                    else -> throw IllegalArgumentException("Unsupported endpoint: $endpoint")
                }
            }

            "DELETE" -> delete(actualEndpoint)
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        val jwtPostProcessor = if (subClaim != null) {
            jwt().authorities(SimpleGrantedAuthority(role)).jwt { jwt -> jwt.claim("sub", subClaim) }
        } else {
            jwt().authorities(SimpleGrantedAuthority(role))
        }

        mockMvc.perform(
            request.with(jwtPostProcessor)
        ).andExpect(status().`is`(expectedStatus))
    }

    @ParameterizedTest(name = "User with role {0} and ID {1} accessing transport with driver ID {2} should return {3}")
    @MethodSource("transportAccessScenarios")
    fun `should verify driver-specific authorization for transport access`(
        role: String,
        userId: String,
        driverId: String,
        expectedStatus: Int
    ) {
        // Use the test transport ID if the driver ID matches the test driver, otherwise use the other transport ID
        val transportId = if (driverId == testDriverId.toString()) {
            testTransportId
        } else {
            transportWithOtherDriver.id
        }

        mockMvc.perform(
            get("/transport/$transportId")
                .with(
                    jwt()
                        .authorities(SimpleGrantedAuthority(role))
                        .jwt { jwt -> jwt.claim("sub", userId) }
                )
        ).andExpect(status().`is`(expectedStatus))
    }
}
