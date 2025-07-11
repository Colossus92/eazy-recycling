package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.LocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethod
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TransportControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var transportRepository: TransportRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @Autowired
    private lateinit var locationRepository: LocationRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    private lateinit var testCompany: CompanyDto
    private lateinit var testLocation: LocationDto
    private lateinit var testTruck: Truck
    private lateinit var testDriver: ProfileDto
    private lateinit var testContainer: WasteContainerDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        
        // Create test company
        testCompany = CompanyDto(
            name = "Test Company",
            address = AddressDto(
                streetName = "Company Street",
                buildingNumber = "123",
                postalCode = "1234AB",
                city = "Test City",
                country = "Test Country"
            )
        )
        companyRepository.save(testCompany)

        // Create test location
        testLocation = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Location Street",
                buildingNumber = "456",
                postalCode = "5678CD",
                city = "Location City",
                country = "Location Country"
            )
        )
        locationRepository.save(testLocation)

        // Create test truck
        testTruck = Truck(
            licensePlate = "TEST-123",
            brand = "Mercedes",
            model = "Actros"
        )
        entityManager.persist(testTruck)

        // Create test driver
        testDriver = ProfileDto(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
        )
        entityManager.persist(testDriver)

        // Create test container
        testContainer = WasteContainerDto(
            id = "40M001",
            notes = "Test Container",
        )
        entityManager.persist(testContainer)

        entityManager.flush()
    }

    @AfterEach
    fun cleanup() {
        transportRepository.deleteAll()
    }

    @Test
    fun `should get all transports`() {
        // Given
        val transport1 = createTestTransport("Transport 1")
        val transport2 = createTestTransport("Transport 2")
        transportRepository.saveAll(listOf(transport1, transport2))

        // When & Then
        securedMockMvc.get("/transport")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `should get transport by id`() {
        // Given
        val transport = createTestTransport("Test Transport")
        val savedTransport = transportRepository.save(transport)

        // When & Then
        securedMockMvc.get("/transport/${savedTransport.id}")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("Test Transport"))
    }

    @Test
    fun `should return not found when getting transport with non-existent id`() {
        // When & Then
        val nonExistentId = UUID.randomUUID()
        securedMockMvc.get("/transport/$nonExistentId")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should create container transport`() {
        // Given
        val request = CreateContainerTransportRequest(
            consignorPartyId = testCompany.id!!,
            pickupDateTime = LocalDateTime.now().plusDays(1),
            deliveryDateTime = LocalDateTime.now().plusDays(2),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.DELIVERY,
            driverId = testDriver.id,
            carrierPartyId = testCompany.id!!,
            pickupCompanyId = testCompany.id,
            pickupStreet = "Pickup Street",
            pickupBuildingNumber = "789",
            pickupPostalCode = "9012EF",
            pickupCity = "Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "101",
            deliveryPostalCode = "1122GH",
            deliveryCity = "Delivery City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "New Container Transport"
        )

        // When & Then
        securedMockMvc.post(
            "/transport/container",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("New Container Transport"))
            .andExpect(jsonPath("$.transportType").value("CONTAINER"))
            .andExpect(jsonPath("$.containerOperation").value("DELIVERY"))

        // Verify transport was saved in the database
        val savedTransports = transportRepository.findAll()
        assertThat(savedTransports).hasSize(1)
        assertThat(savedTransports[0].note).isEqualTo("New Container Transport")
        assertThat(savedTransports[0].transportType).isEqualTo(TransportType.CONTAINER)
    }

    @Test
    fun `should create waste transport`() {
        // Given
        val request = CreateWasteTransportRequest(
            consigneePartyId = testCompany.id.toString(),
            pickupPartyId = testCompany.id.toString(),
            consignorPartyId = testCompany.id!!,
            pickupDateTime = LocalDateTime.now().plusDays(1),
            deliveryDateTime = LocalDateTime.now().plusDays(2),
            transportType = TransportType.WASTE,
            containerOperation = ContainerOperation.PICKUP,
            driverId = testDriver.id,
            carrierPartyId = testCompany.id!!,
            pickupCompanyId = testCompany.id,
            pickupStreet = "Pickup Street",
            pickupBuildingNumber = "789",
            pickupPostalCode = "9012EF",
            pickupCity = "Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "101",
            deliveryPostalCode = "1122GH",
            deliveryCity = "Delivery City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "New Waste Transport",
            wasteStreamNumber = "WSN123",
            weight = 1000,
            unit = "KG",
            quantity = 1,
            goodsName = "Test Waste",
            euralCode = "200301",
            processingMethodCode = "A.02",
        )

        // When & Then
        securedMockMvc.post(
            "/transport/waste",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("New Waste Transport"))
            .andExpect(jsonPath("$.transportType").value("WASTE"))
            .andExpect(jsonPath("$.containerOperation").value("PICKUP"))
            .andExpect(jsonPath("$.goods").exists())
            .andExpect(jsonPath("$.goods.goodsItem.wasteStreamNumber").value("WSN123"))

        // Verify transport was saved in the database
        val savedTransports = transportRepository.findAll()
        assertThat(savedTransports).hasSize(1)
        assertThat(savedTransports[0].note).isEqualTo("New Waste Transport")
        assertThat(savedTransports[0].transportType).isEqualTo(TransportType.WASTE)
        assertThat(savedTransports[0].goods).isNotNull
        assertThat(savedTransports[0].goods?.goodsItem?.wasteStreamNumber).isEqualTo("WSN123")
    }

    @Test
    fun `should update container transport`() {
        // Given
        val transport = createTestTransport("Original Transport")
        val savedTransport = transportRepository.save(transport)

        val updateRequest = CreateContainerTransportRequest(
            consignorPartyId = testCompany.id!!,
            pickupDateTime = LocalDateTime.now().plusDays(3),
            deliveryDateTime = LocalDateTime.now().plusDays(4),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.EXCHANGE,
            driverId = testDriver.id,
            carrierPartyId = testCompany.id!!,
            pickupCompanyId = testCompany.id,
            pickupStreet = "Updated Pickup Street",
            pickupBuildingNumber = "999",
            pickupPostalCode = "3344IJ",
            pickupCity = "Updated Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Updated Delivery Street",
            deliveryBuildingNumber = "888",
            deliveryPostalCode = "5566KL",
            deliveryCity = "Updated Delivery City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Updated Container Transport"
        )

        // When & Then
        securedMockMvc.put(
            "/transport/container/${savedTransport.id}",
            objectMapper.writeValueAsString(updateRequest)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("Updated Container Transport"))
            .andExpect(jsonPath("$.containerOperation").value("EXCHANGE"))

        // Verify transport was updated in the database
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.note).isEqualTo("Updated Container Transport")
        assertThat(updatedTransport?.containerOperation).isEqualTo(ContainerOperation.EXCHANGE)
    }

    @Test
    fun `should update waste transport`() {
        // Given
        val goodsItem = GoodsItemDto(
            wasteStreamNumber = "Original-WSN",
            netNetWeight = 500,
            unit = "KG",
            quantity = 1,
            euralCode = "200101",
            name = "Original Waste",
            processingMethodCode = "A.02",
        )

        val goods = GoodsDto(
            id = UUID.randomUUID().toString(),
            goodsItem = goodsItem,
            consigneeParty = testCompany,
            pickupParty = testCompany
        )

        val transport = createTestTransport("Original Waste Transport", goods)
        val savedTransport = transportRepository.save(transport)

        val updateRequest = CreateWasteTransportRequest(
            consigneePartyId = testCompany.id.toString(),
            pickupPartyId = testCompany.id.toString(),
            consignorPartyId = testCompany.id!!,
            pickupDateTime = LocalDateTime.now().plusDays(3),
            deliveryDateTime = LocalDateTime.now().plusDays(4),
            transportType = TransportType.WASTE,
            containerOperation = ContainerOperation.PICKUP,
            driverId = testDriver.id,
            carrierPartyId = testCompany.id!!,
            pickupCompanyId = testCompany.id,
            pickupStreet = "Updated Pickup Street",
            pickupBuildingNumber = "999",
            pickupPostalCode = "3344IJ",
            pickupCity = "Updated Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Updated Delivery Street",
            deliveryBuildingNumber = "888",
            deliveryPostalCode = "5566KL",
            deliveryCity = "Updated Delivery City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Updated Waste Transport",
            wasteStreamNumber = "Updated-WSN",
            weight = 2000,
            unit = "KG",
            quantity = 2,
            goodsName = "Updated Waste",
            euralCode = "200301",
            processingMethodCode = "A.02"
        )

        // When & Then
        securedMockMvc.put(
            "/transport/waste/${savedTransport.id}",
            objectMapper.writeValueAsString(updateRequest)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("Updated Waste Transport"))
            .andExpect(jsonPath("$.goods.goodsItem.wasteStreamNumber").value("Updated-WSN"))
            .andExpect(jsonPath("$.goods.goodsItem.netNetWeight").value(2000))

        // Verify transport was updated in the database
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.note).isEqualTo("Updated Waste Transport")
        assertThat(updatedTransport?.goods?.goodsItem?.wasteStreamNumber).isEqualTo("Updated-WSN")
        assertThat(updatedTransport?.goods?.goodsItem?.netNetWeight).isEqualTo(2000)
    }

    @Test
    fun `should assign waybill transport`() {
        // Given
        val goodsItem = GoodsItemDto(
            wasteStreamNumber = "WSN-Waybill",
            netNetWeight = 800,
            unit = "KG",
            quantity = 1,
            euralCode = "200201",
            name = "Waybill Waste",
            processingMethodCode = "A.02",
        )

        val goods = GoodsDto(
            id = UUID.randomUUID().toString(),
            goodsItem = goodsItem,
            consigneeParty = testCompany,
            pickupParty = testCompany
        )

        val transport = TransportDto(
            consignorParty = testCompany,
            carrierParty = testCompany,
            pickupCompany = testCompany,
            pickupLocation = testLocation,
            pickupDateTime = LocalDateTime.now().plusDays(1),
            deliveryCompany = testCompany,
            deliveryLocation = testLocation,
            deliveryDateTime = LocalDateTime.now().plusDays(2),
            transportType = TransportType.WASTE,
            note = "Unassigned Waybill Transport",
            goods = goods,
            sequenceNumber = 1,
        )
        val savedTransport = transportRepository.save(transport)

        val assignRequest = AssignWaybillTransportRequest(
            licensePlate = testTruck.licensePlate,
            waybillId = goods.uuid!!,
            driverId = testDriver.id
        )

        // When & Then
        securedMockMvc.post(
            "/transport/waybill",
            objectMapper.writeValueAsString(assignRequest)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.truck.licensePlate").value(testTruck.licensePlate))
            .andExpect(jsonPath("$.driver.id").value(testDriver.id.toString()))

        // Verify transport was updated in the database
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.truck?.licensePlate).isEqualTo(testTruck.licensePlate)
        assertThat(updatedTransport?.driver?.id).isEqualTo(testDriver.id)
    }

    @Test
    fun `should delete transport`() {
        // Given
        val transport = createTestTransport("Transport to Delete")
        val savedTransport = transportRepository.save(transport)

        // When & Then
        securedMockMvc.delete("/transport/${savedTransport.id}")
            .andExpect(status().isNoContent)

        // Verify transport was deleted
        assertThat(transportRepository.findByIdOrNull(savedTransport.id!!)).isNull()
    }

    @Test
    fun `should return not found when updating non-existent transport`() {
        // Given
        val nonExistentId = UUID.randomUUID()
        val updateRequest = CreateContainerTransportRequest(
            consignorPartyId = testCompany.id!!,
            pickupDateTime = LocalDateTime.now().plusDays(1),
            deliveryDateTime = LocalDateTime.now().plusDays(2),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.DELIVERY,
            driverId = testDriver.id,
            carrierPartyId = testCompany.id!!,
            pickupCompanyId = testCompany.id,
            pickupStreet = "Pickup Street",
            pickupBuildingNumber = "789",
            pickupPostalCode = "9012EF",
            pickupCity = "Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "101",
            deliveryPostalCode = "1122GH",
            deliveryCity = "Delivery City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Non-existent Transport"
        )

        // When & Then
        securedMockMvc.put(
            "/transport/container/$nonExistentId",
            objectMapper.writeValueAsString(updateRequest)
        )
            .andExpect(status().isNotFound)
    }

    private fun createTestTransport(note: String, goods: GoodsDto? = null): TransportDto {
        return TransportDto(
            consignorParty = testCompany,
            carrierParty = testCompany,
            pickupCompany = testCompany,
            pickupLocation = testLocation,
            pickupDateTime = LocalDateTime.now().plusDays(1),
            deliveryCompany = testCompany,
            deliveryLocation = testLocation,
            deliveryDateTime = LocalDateTime.now().plusDays(2),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.DELIVERY,
            truck = testTruck,
            driver = testDriver,
            note = note,
            goods = goods,
            sequenceNumber = 1,
        )
    }
}
