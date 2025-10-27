package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.application.usecase.transport.*
import nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport.ContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport.WasteTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerTransport
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportId
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.domain.model.user.UserId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ContainerTransports
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteTransports
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.UserRoleDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
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

                // POST container transport - admin and planner can access (returns 201 CREATED)
                Arguments.of("/transport/container", "POST", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 201),
                Arguments.of("/transport/container", "POST", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 201),
                Arguments.of("/transport/container", "POST", Roles.CHAUFFEUR, TEST_DRIVER_ID.toString(), 403),
                Arguments.of("/transport/container", "POST", "unauthorized_role", UUID.randomUUID().toString(), 403),

                // POST waste transport - admin and planner can access (returns 201 CREATED)
                Arguments.of("/transport/waste", "POST", Roles.ADMIN, OTHER_DRIVER_ID.toString(), 201),
                Arguments.of("/transport/waste", "POST", Roles.PLANNER, OTHER_DRIVER_ID.toString(), 201),
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

    @MockitoBean
    private lateinit var createContainerTransport: CreateContainerTransport

    @MockitoBean
    private lateinit var updateContainerTransport: UpdateContainerTransport

    @MockitoBean
    private lateinit var containerTransports: ContainerTransports

    @MockitoBean
    private lateinit var createWasteTransport: CreateWasteTransport

    @MockitoBean
    private lateinit var updateWasteTransport: UpdateWasteTransport

    @MockitoBean
    private lateinit var wasteTransports: WasteTransports

    @MockitoBean
    private lateinit var wasteStreamJpaRepository: WasteStreamJpaRepository

    // Use the companion object constants instead of local variables
    private val testTransportId = TEST_TRANSPORT_ID
    private val testDriverId = TEST_DRIVER_ID
    private val otherDriverId = OTHER_DRIVER_ID

    private lateinit var transportWithTestDriver: TransportDto
    private lateinit var transportWithOtherDriver: TransportDto

    private var consignor: CompanyDto = mockCompanyDto()
    private var carrier: CompanyDto = mockCompanyDto()
    private var deliveryCompany: CompanyDto = mockCompanyDto()
    private var pickupCompany: CompanyDto = mockCompanyDto()
    private lateinit var testWasteStream: WasteStreamDto

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
            consignorParty = consignor,
            carrierParty = carrier,
            pickupLocation = mockLocationDto(),
            deliveryLocation = mockLocationDto(),
            pickupDateTime = LocalDateTime.now(),
            deliveryDateTime = LocalDateTime.now().plusDays(1),
            transportType = TransportType.CONTAINER,
            note = "Test transport",
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
        whenever(transportService.getTransportById(transportWithOtherDriver.id!!)).thenReturn(transportWithOtherDriver)
        whenever(transportService.getAllTransports()).thenReturn(
            listOf(
                transportWithTestDriver,
                transportWithOtherDriver
            )
        )

        // Mock container transport create use case
        whenever(createContainerTransport.handle(any())).thenReturn(
            CreateContainerTransportResult(
                transportId = TransportId(testTransportId)
            )
        )

        // Mock container transport update use case
        whenever(updateContainerTransport.handle(any())).thenReturn(
            UpdateContainerTransportResult(
                transportId = TransportId(testTransportId),
                status = "PLANNED"
            )
        )

        // Create test waste stream
        testWasteStream = WasteStreamDto(
            number = "123451234567",
            name = "Test Waste Stream",
            euralCode = Eural(code = "16 01 17", description = "Paper and cardboard"),
            processingMethodCode = ProcessingMethodDto(code = "A.01", description = "Recycling"),
            wasteCollectionType = WasteCollectionType.DEFAULT.name,
            pickupLocation = mockLocationDto(),
            processorParty = consignor,
            consignorParty = consignor,
            pickupParty = consignor,
            dealerParty = null,
            collectorParty = null,
            brokerParty = null,
            lastActivityAt = java.time.Instant.now(),
            status = WasteStreamStatus.ACTIVE.name
        )

        // Mock waste stream repository
        whenever(wasteStreamJpaRepository.findById(testWasteStream.number)).thenReturn(
            Optional.of(testWasteStream)
        )

        // Mock waste transport create use case
        whenever(createWasteTransport.handle(any())).thenReturn(
            CreateWasteTransportResult(
                transportId = TransportId(testTransportId)
            )
        )

        // Mock waste transport update use case
        whenever(updateWasteTransport.handle(any())).thenReturn(
            UpdateWasteTransportResult(
                transportId = TransportId(testTransportId),
                status = "PLANNED"
            )
        )

        // Mock container transports repository for authorization check
        val mockContainerTransport = ContainerTransport(
            transportId = TransportId(testTransportId),
            displayNumber = null,
            consignorParty = nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId(consignor.id!!),
            carrierParty = nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId(carrier.id!!),
            pickupLocation = nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.DutchAddress(
                address = nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address(
                    streetName = "Test Street",
                    postalCode = nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode("1234AB"),
                    buildingNumber = "1",
                    city = "Test City"
                )
            ),
            pickupDateTime = kotlinx.datetime.Clock.System.now(),
            deliveryLocation = nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.DutchAddress(
                address = nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address(
                    streetName = "Test Street",
                    postalCode = nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode("1234AB"),
                    buildingNumber = "1",
                    city = "Test City"
                )
            ),
            deliveryDateTime = kotlinx.datetime.Clock.System.now(),
            transportType = TransportType.CONTAINER,
            wasteContainer = null,
            truck = null,
            driver = UserId(testDriverId),
            note = nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note("Test note"),
            transportHours = null,
            updatedAt = kotlinx.datetime.Clock.System.now(),
            sequenceNumber = 1
        )
        whenever(containerTransports.findById(TransportId(testTransportId))).thenReturn(mockContainerTransport)

        // Mock waste transports repository for authorization check
        val mockWasteTransport = nl.eazysoftware.eazyrecyclingservice.domain.model.transport.WasteTransport(
            transportId = TransportId(testTransportId),
            displayNumber = null,
            carrierParty = nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId(carrier.id!!),
            pickupDateTime = kotlinx.datetime.Clock.System.now(),
            deliveryDateTime = kotlinx.datetime.Clock.System.now(),
            transportType = TransportType.WASTE,
            goodsItem = nl.eazysoftware.eazyrecyclingservice.domain.model.transport.GoodsItem(
                wasteStreamNumber = nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber(testWasteStream.number),
                netNetWeight = 1000,
                unit = "KG",
                quantity = 1
            ),
            wasteContainer = null,
            containerOperation = ContainerOperation.PICKUP,
            truck = null,
            driver = UserId(testDriverId),
            note = nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note("Test note"),
            transportHours = null,
            updatedAt = kotlinx.datetime.Clock.System.now(),
            sequenceNumber = 1
        )
        whenever(wasteTransports.findById(TransportId(testTransportId))).thenReturn(mockWasteTransport)
    }

    // Helper method to create mock objects
    private fun mockCompanyDto() = CompanyDto(
        id = UUID.randomUUID(),
        name = "Test Company",
        address = AddressDto(
            streetName = "Test Street",
            buildingNumber = "1",
            postalCode = "1234 AB",
            city = "Test City",
            country = "Test Country"
        )
    )

    private fun mockLocationDto() = PickupLocationDto.PickupProjectLocationDto(
            id = UUID.randomUUID().toString(),
            company = mockCompanyDto(),
            streetName = "Test Street",
            buildingNumber = "1",
            buildingNumberAddition = null,
            postalCode = "1234 AB",
            city = "Test City",
            country = "Test Country"
    )

    private fun createContainerTransportRequestJson(): String {
        val request = ContainerTransportRequest(
          consignorPartyId = consignor.id!!,
          pickupDateTime = LocalDateTime.now(),
          deliveryDateTime = LocalDateTime.now().plusDays(1),
          transportType = TransportType.CONTAINER,
          containerOperation = ContainerOperation.DELIVERY,
          driverId = testDriverId,
          carrierPartyId = carrier.id!!,
          pickupCompanyId = pickupCompany.id!!,
          pickupStreet = "Pickup Street",
          pickupBuildingNumber = "1",
          pickupPostalCode = "1234 AB",
          pickupCity = "Pickup City",
          deliveryCompanyId = deliveryCompany.id!!,
          deliveryStreet = "Delivery Street",
          deliveryBuildingNumber = "2",
          deliveryPostalCode = "5678 CD",
          deliveryCity = "Delivery City",
          truckId = "TRUCK-123",
          containerId = UUID.randomUUID(),
          note = "Test container transport"
        )

        return objectMapper.writeValueAsString(request)
    }

    private fun createWasteTransportRequestJson(): String {
        val request = WasteTransportRequest(
            pickupDateTime = LocalDateTime.now(),
            deliveryDateTime = LocalDateTime.now().plusDays(1),
            containerOperation = ContainerOperation.PICKUP,
            transportType = TransportType.WASTE,
            driverId = testDriverId,
            carrierPartyId = carrier.id!!,
            truckId = "TRUCK-123",
            containerId = UUID.randomUUID(),
            note = "Test waste transport",
            wasteStreamNumber = testWasteStream.number,
            weight = 10,
            unit = "KG",
            quantity = 1
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
