package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyBranchRepository
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.LocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
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

    @Autowired
    private lateinit var companyBranchRepository: CompanyBranchRepository

    private lateinit var testCompany: CompanyDto
    private lateinit var testLocation: LocationDto
    private lateinit var testTruck: Truck
    private lateinit var testDriver: ProfileDto
    private lateinit var testContainer: WasteContainerDto
    private lateinit var testBranch: CompanyBranchDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)

        // Create test company
        testCompany = CompanyDto(
            name = "Test Company",
            address = AddressDto(
                streetName = "Company Street",
                buildingNumber = "123",
                postalCode = "1234 AB",
                city = "Test City",
                country = "Test Country"
            )
        )
        companyRepository.save(testCompany)

        // Create test branch
        testBranch = CompanyBranchDto(
            company = testCompany,
            address = AddressDto(
                streetName = "Branch Street",
                buildingNumber = "456",
                postalCode = "5678 CD",
                city = "Branch City",
                country = "Branch Country"
            )
        )
        companyBranchRepository.save(testBranch)

        // Create test location
        testLocation = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Location Street",
                buildingNumber = "456",
                postalCode = "5678 CD",
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
            pickupPostalCode = "9012 EF",
            pickupCity = "Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "101",
            deliveryPostalCode = "1122 GH",
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
            pickupPostalCode = "9012 EF",
            pickupCity = "Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "101",
            deliveryPostalCode = "1122 GH",
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
            consignorClassification = 1,
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
            pickupPostalCode = "3344 IJ",
            pickupCity = "Updated Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Updated Delivery Street",
            deliveryBuildingNumber = "888",
            deliveryPostalCode = "5566 KL",
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
            pickupParty = testCompany,
            consignorClassification = 2
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
            pickupPostalCode = "3344 IJ",
            pickupCity = "Updated Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Updated Delivery Street",
            deliveryBuildingNumber = "888",
            deliveryPostalCode = "5566 KL",
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
            processingMethodCode = "A.02",
            consignorClassification = 1
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
        assertThat(updatedTransport?.goods?.consignorClassification).isOne
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
            pickupPostalCode = "9012 EF",
            pickupCity = "Pickup City",
            deliveryCompanyId = testCompany.id,
            deliveryStreet = "Delivery Street",
            deliveryBuildingNumber = "101",
            deliveryPostalCode = "1122 GH",
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

    @Test
    fun `should mark transport as finished`() {
        // Given
        val transport = createTestTransport("Transport to be finished")
        val savedTransport = transportRepository.save(transport)
        val request = TransportController.TransportFinishedRequest(hours = 2.5)

        // When & Then
        securedMockMvc.post(
            "/transport/${savedTransport.id}/finished",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isOk)

        // Verify transport was marked as finished in the database
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.transportHours).isEqualTo(2.5)
        assertThat(updatedTransport?.getStatus()).isEqualTo(TransportDto.Status.FINISHED)
    }

    @Test
    fun `should return not found when marking non-existent transport as finished`() {
        // Given
        val nonExistentId = UUID.randomUUID()
        val request = TransportController.TransportFinishedRequest(hours = 2.0)

        // When & Then
        securedMockMvc.post(
            "/transport/$nonExistentId/finished",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should forbid non-driver user from marking someone else's transport as finished`() {
        // Given
        val transport = createTestTransport("Another Driver's Transport")
        val savedTransport = transportRepository.save(transport.copy(driver = testDriver))
        val request = TransportController.TransportFinishedRequest(hours = 1.5)

        // Different user ID than the driver
        val differentUserId = UUID.randomUUID().toString()

        // When & Then - Using a different user ID as the JWT subject
        securedMockMvc.postWithSubject(
            "/transport/${savedTransport.id}/finished",
            objectMapper.writeValueAsString(request),
            differentUserId,
            listOf("chauffeur") // Non-admin role
        )
            .andExpect(status().isForbidden)

        // Verify transport was NOT marked as finished
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.transportHours).isNull()
        assertThat(updatedTransport?.getStatus()).isNotEqualTo(TransportDto.Status.FINISHED)
    }

    @Test
    fun `should allow admin to mark any transport as finished`() {
        // Given
        val transport = createTestTransport("Admin Access Transport")
        val savedTransport = transportRepository.save(transport.copy(driver = testDriver))
        val request = TransportController.TransportFinishedRequest(hours = 4.0)

        // Different user ID than the driver
        val adminUserId = UUID.randomUUID().toString()

        // When & Then - Using admin role
        securedMockMvc.postWithSubject(
            "/transport/${savedTransport.id}/finished",
            objectMapper.writeValueAsString(request),
            adminUserId,
            listOf("admin")
        )
            .andExpect(status().isOk)

        // Verify transport was marked as finished
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.transportHours).isEqualTo(4.0)
        assertThat(updatedTransport?.getStatus()).isEqualTo(TransportDto.Status.FINISHED)
    }

    @Test
    fun `should allow planner to mark any transport as finished`() {
        // Given
        val transport = createTestTransport("Planner Access Transport")
        val savedTransport = transportRepository.save(transport.copy(driver = testDriver))
        val request = TransportController.TransportFinishedRequest(hours = 2.75)

        // Different user ID than the driver
        val plannerUserId = UUID.randomUUID().toString()

        // When & Then - Using planner role
        securedMockMvc.postWithSubject(
            "/transport/${savedTransport.id}/finished",
            objectMapper.writeValueAsString(request),
            plannerUserId,
            listOf("planner")
        )
            .andExpect(status().isOk)

        // Verify transport was marked as finished
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.transportHours).isEqualTo(2.75)
        assertThat(updatedTransport?.getStatus()).isEqualTo(TransportDto.Status.FINISHED)
    }

    @Test
    fun `should create container transport with branch references`() {
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
            pickupCompanyBranchId = testBranch.id,
            pickupStreet = "Branch Street",
            pickupBuildingNumber = "456",
            pickupPostalCode = "5678 CD",
            pickupCity = "Branch City",
            deliveryCompanyId = testCompany.id,
            deliveryCompanyBranchId = testBranch.id,
            deliveryStreet = "Branch Street",
            deliveryBuildingNumber = "456",
            deliveryPostalCode = "5678 CD",
            deliveryCity = "Branch City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Transport with Branch References"
        )

        // When & Then
        securedMockMvc.post(
            "/transport/container",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("Transport with Branch References"))
            .andExpect(jsonPath("$.transportType").value("CONTAINER"))
            .andExpect(jsonPath("$.containerOperation").value("DELIVERY"))

        // Verify transport was saved in the database with branch references
        val savedTransports = transportRepository.findAll()
        assertThat(savedTransports).hasSize(1)
        assertThat(savedTransports[0].note).isEqualTo("Transport with Branch References")
        assertThat(savedTransports[0].pickupCompanyBranch?.id).isEqualTo(testBranch.id)
        assertThat(savedTransports[0].deliveryCompanyBranch?.id).isEqualTo(testBranch.id)
    }

    @Test
    fun `should update container transport with branch references`() {
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
            pickupCompanyBranchId = testBranch.id,
            pickupStreet = "Branch Street",
            pickupBuildingNumber = "456",
            pickupPostalCode = "5678 CD",
            pickupCity = "Branch City",
            deliveryCompanyId = testCompany.id,
            deliveryCompanyBranchId = testBranch.id,
            deliveryStreet = "Branch Street",
            deliveryBuildingNumber = "456",
            deliveryPostalCode = "5678 CD",
            deliveryCity = "Branch City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Updated Transport with Branches"
        )

        // When & Then
        securedMockMvc.put(
            "/transport/container/${savedTransport.id}",
            objectMapper.writeValueAsString(updateRequest)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("Updated Transport with Branches"))
            .andExpect(jsonPath("$.containerOperation").value("EXCHANGE"))

        // Verify transport was updated in the database with branch references
        val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id!!)
        assertThat(updatedTransport).isNotNull
        assertThat(updatedTransport?.note).isEqualTo("Updated Transport with Branches")
        assertThat(updatedTransport?.pickupCompanyBranch?.id).isEqualTo(testBranch.id)
        assertThat(updatedTransport?.deliveryCompanyBranch?.id).isEqualTo(testBranch.id)
    }

    @Test
    fun `should fail when branch does not belong to company`() {
        // Given
        // Create a second company
        val anotherCompany = CompanyDto(
            name = "Another Company",
            address = AddressDto(
                streetName = "Another Street",
                buildingNumber = "789",
                postalCode = "9012 EF",
                city = "Another City",
                country = "Another Country"
            )
        )
        companyRepository.save(anotherCompany)

        val request = CreateContainerTransportRequest(
            consignorPartyId = testCompany.id!!,
            pickupDateTime = LocalDateTime.now().plusDays(1),
            deliveryDateTime = LocalDateTime.now().plusDays(2),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.DELIVERY,
            driverId = testDriver.id,
            carrierPartyId = testCompany.id!!,
            pickupCompanyId = anotherCompany.id, // Different company
            pickupCompanyBranchId = testBranch.id, // Branch belongs to testCompany
            pickupStreet = "Branch Street",
            pickupBuildingNumber = "456",
            pickupPostalCode = "5678 CD",
            pickupCity = "Branch City",
            deliveryCompanyId = testCompany.id,
            deliveryCompanyBranchId = testBranch.id,
            deliveryStreet = "Branch Street",
            deliveryBuildingNumber = "456",
            deliveryPostalCode = "5678 CD",
            deliveryCity = "Branch City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Invalid Branch-Company Relationship"
        )

        // When & Then
        securedMockMvc.post(
            "/transport/container",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value(containsString("is niet van bedrijf")))

        // Verify no transport was saved
        val savedTransports = transportRepository.findAll()
        assertThat(savedTransports).isEmpty()
    }

    @Test
    fun `should create waste transport with branch references`() {
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
            pickupCompanyBranchId = testBranch.id,
            pickupStreet = "Branch Street",
            pickupBuildingNumber = "456",
            pickupPostalCode = "5678 CD",
            pickupCity = "Branch City",
            deliveryCompanyId = testCompany.id,
            deliveryCompanyBranchId = testBranch.id,
            deliveryStreet = "Branch Street",
            deliveryBuildingNumber = "456",
            deliveryPostalCode = "5678 CD",
            deliveryCity = "Branch City",
            truckId = testTruck.licensePlate,
            containerId = testContainer.uuid,
            note = "Waste Transport with Branches",
            wasteStreamNumber = "WSN123",
            weight = 1000,
            unit = "KG",
            quantity = 1,
            goodsName = "Test Waste",
            euralCode = "200301",
            processingMethodCode = "A.02",
            consignorClassification = 1,
        )

        // When & Then
        securedMockMvc.post(
            "/transport/waste",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.note").value("Waste Transport with Branches"))
            .andExpect(jsonPath("$.transportType").value("WASTE"))
            .andExpect(jsonPath("$.containerOperation").value("PICKUP"))
            .andExpect(jsonPath("$.goods").exists())
            .andExpect(jsonPath("$.goods.goodsItem.wasteStreamNumber").value("WSN123"))

        // Verify transport was saved in the database with branch references
        val savedTransports = transportRepository.findAll()
        assertThat(savedTransports).hasSize(1)
        assertThat(savedTransports[0].note).isEqualTo("Waste Transport with Branches")
        assertThat(savedTransports[0].pickupCompanyBranch?.id).isEqualTo(testBranch.id)
        assertThat(savedTransports[0].deliveryCompanyBranch?.id).isEqualTo(testBranch.id)
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
