package nl.eazysoftware.eazyrecyclingservice.controller.transport

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

class PlanningControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var transportRepository: TransportRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCompany: CompanyDto
    private lateinit var testPickupLocation: PickupLocationDto
    private lateinit var testDeliveryLocation: PickupLocationDto
    private lateinit var testTruck: Truck
    private lateinit var testDriver: ProfileDto
    private lateinit var otherDriver: ProfileDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)

        // Create test company
        testCompany = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Company Street",
                buildingNumber = "123",
                postalCode = "1234AB",
                city = "Test City",
                country = "Test Country"
            )
        )
        entityManager.persist(testCompany)

        // Create test locations
        testPickupLocation = PickupLocationDto.PickupProjectLocationDto(
                id = UUID.randomUUID().toString(),
                company = testCompany,
                streetName = "Pickup Street",
                buildingNumber = "456",
                buildingNumberAddition = null,
                postalCode = "5678CD",
                city = "Pickup City",
                country = "Pickup Country",
        )
        entityManager.persist(testPickupLocation)

        testDeliveryLocation = PickupLocationDto.PickupProjectLocationDto(
                id = UUID.randomUUID().toString(),
                company = testCompany,
                streetName = "Delivery Street",
                buildingNumber = "789",
                buildingNumberAddition = null,
                postalCode = "9012EF",
                city = "Delivery City",
                country = "Delivery Country"
        )
        entityManager.persist(testDeliveryLocation)

        // Create test truck
        testTruck = Truck(
            licensePlate = "TEST-123",
            brand = "Mercedes",
            model = "Actros"
        )
        entityManager.persist(testTruck)

        // Create test drivers
        testDriver = ProfileDto(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
        )
        entityManager.persist(testDriver)

        otherDriver = ProfileDto(
            id = UUID.randomUUID(),
            firstName = "Jane",
            lastName = "Smith",
        )
        entityManager.persist(otherDriver)

        entityManager.flush()
    }

    @AfterEach
    fun cleanup() {
        transportRepository.deleteAll()
    }

    @Test
    fun `should get driver planning with admin role`() {
        // Given
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        createTestTransportsForDriver(startDate)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/planning/driver/${testDriver.id}")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.${startDate}").exists())
            .andExpect(jsonPath("$.${startDate}.TEST-123").isArray)
            .andExpect(jsonPath("$.${startDate}.TEST-123.length()").value(2))
    }

    @Test
    fun `should get driver planning with chauffeur role when requesting own planning`() {
        // Given
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        createTestTransportsForDriver(startDate)

        // When & Then - Driver accessing their own planning
        mockMvc.perform(
            MockMvcRequestBuilders.get("/planning/driver/${testDriver.id}")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(jwt()
                    .authorities(SimpleGrantedAuthority(Roles.CHAUFFEUR))
                    .jwt { it.claim("sub", testDriver.id.toString()) }
                )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should get driver planning with planner role`() {
        // Given
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        createTestTransportsForDriver(startDate)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/planning/driver/${testDriver.id}")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.PLANNER)))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
    }

    @Test
    fun `should return empty planning when no transports exist for driver`() {
        // Given
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        // No transports created for this driver

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.get("/planning/driver/${testDriver.id}")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isEmpty)
    }

    @Test
    fun `should return 400 when missing required parameters`() {
        // When & Then - Missing startDate
        mockMvc.perform(
            MockMvcRequestBuilders.get("/planning/driver/${testDriver.id}")
                .param("endDate", LocalDate.now().plusDays(7).toString())
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
            .andExpect(status().isBadRequest)

        // When & Then - Missing endDate
        mockMvc.perform(
            MockMvcRequestBuilders.get("/planning/driver/${testDriver.id}")
                .param("startDate", LocalDate.now().toString())
                .with(jwt().authorities(SimpleGrantedAuthority(Roles.ADMIN)))
        )
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @MethodSource("securityTestCases")
    fun `should enforce proper authorization for driver planning endpoint`(testCase: SecurityTestCase) {
        // Given
        val startDate = LocalDate.now()
        val endDate = startDate.plusDays(7)

        // When & Then
        mockMvc.perform(
            MockMvcRequestBuilders.request(testCase.method, "/planning/driver/${testDriver.id}")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .with(jwt().authorities(SimpleGrantedAuthority(testCase.role)))
        )
            .andExpect(status().`is`(testCase.expectedStatus))
    }

    private fun createTestTransportsForDriver(date: LocalDate): List<TransportDto> {
        val pickupDateTime1 = date.atTime(10, 0)
        val deliveryDateTime1 = date.atTime(12, 0)
        val pickupDateTime2 = date.atTime(14, 0)
        val deliveryDateTime2 = date.atTime(16, 0)

        val transport1 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = testCompany,
            carrierParty = testCompany,
            pickupLocation = testPickupLocation,
            deliveryLocation = testDeliveryLocation,
            pickupDateTime = pickupDateTime1.atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
            deliveryDateTime = deliveryDateTime1.atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
            note = "Test Transport 1",
            truck = testTruck,
            driver = testDriver,
            sequenceNumber = 1,
            transportType = TransportType.CONTAINER,
        )

        val transport2 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-002",
            consignorParty = testCompany,
            carrierParty = testCompany,
            pickupLocation = testPickupLocation,
            deliveryLocation = testDeliveryLocation,
            pickupDateTime = pickupDateTime2.atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
            deliveryDateTime = deliveryDateTime2.atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
            note = "Test Transport 2",
            truck = testTruck,
            driver = testDriver,
            sequenceNumber = 2,
            transportType = TransportType.CONTAINER,
        )

        return transportRepository.saveAll(listOf(transport1, transport2))
    }

    companion object {
        @JvmStatic
        fun securityTestCases() = listOf(
            SecurityTestCase(HttpMethod.GET, Roles.ADMIN, 200),
            SecurityTestCase(HttpMethod.GET, Roles.PLANNER, 200),
            SecurityTestCase(HttpMethod.GET, Roles.CHAUFFEUR, 200)
        )
    }

    data class SecurityTestCase(
        val method: HttpMethod,
        val role: String,
        val expectedStatus: Int
    )
}
