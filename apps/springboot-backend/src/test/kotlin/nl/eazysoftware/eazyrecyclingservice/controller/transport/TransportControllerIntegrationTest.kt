package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport.ContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.containertransport.CreateContainerTransportResponse
import nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport.CreateWasteTransportResponse
import nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport.GoodsRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.wastetransport.WasteTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyProjectLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.company.ProjectLocationJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.TransportGoodsDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

class TransportControllerIntegrationTest(
  @param:Autowired private val transactionTemplate: TransactionTemplate
) : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var transportRepository: TransportRepository

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var entityManager: EntityManager

  @Autowired
  private lateinit var projectLocationJpaRepository: ProjectLocationJpaRepository

  @Autowired
  private lateinit var pickupLocationRepository: PickupLocationRepository

  @Autowired
  private lateinit var wasteStreamJpaRepository: WasteStreamJpaRepository

  @Autowired
  private lateinit var euralRepository: EuralRepository

  @Autowired
  private lateinit var processingMethodRepository: ProcessingMethodRepository

  private lateinit var testCompany: CompanyDto
  private lateinit var testLocation: PickupLocationDto.DutchAddressDto
  private lateinit var testTruck: TruckDto
  private lateinit var testDriver: ProfileDto
  private lateinit var testContainer: WasteContainerDto
  private lateinit var testBranch: CompanyProjectLocationDto
  private lateinit var testWasteStream: WasteStreamDto

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)

    // Create test company
    testCompany = CompanyDto(
      id = UUID.randomUUID(),
      name = "Test Company",
      processorId = "12345",
      address = AddressDto(
        streetName = "Company Street",
        buildingNumber = "123",
        postalCode = "1234 AB",
        city = "Test City",
        country = "Nederland"
      )
    )

    // Create test branch
    testBranch = CompanyProjectLocationDto(
      id = UUID.randomUUID(),
      company = testCompany,
      streetName = "Branch Street",
      buildingNumberAddition = null,
      buildingNumber = "456",
      postalCode = "5678CD",
      city = "Branch City",
      country = "Nederland",
      createdAt = Instant.now(),
      updatedAt = null,
    )

    // Create test location
    testLocation = PickupLocationDto.DutchAddressDto(
      streetName = "Location Street",
      buildingNumber = "456",
      buildingNumberAddition = null,
      postalCode = "5678 CD",
      city = "Location City",
      country = "Nederland"
    )

    transactionTemplate.execute {
      testCompany = companyRepository.save(testCompany)
      testBranch = projectLocationJpaRepository.save(testBranch)
      testLocation = pickupLocationRepository.save(testLocation)
    }


    // Create test truck
    testTruck = TruckDto(
      licensePlate = "TEST-123",
      brand = "Mercedes",
      description = "Actros"
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

    val testEural = euralRepository.findById("16 01 17")
      .orElseThrow { IllegalStateException("Eural 16 01 17 should exist in data.sql") }

    val testProcessingMethod = processingMethodRepository.findById("A.01")
      .orElseThrow { IllegalStateException("Processing method A.01 should exist in data.sql") }

    entityManager.flush()
    entityManager.clear()

    // Reload testCompany to ensure it's managed
    val managedCompany = companyRepository.findById(testCompany.id).get()
    val managedLocation = pickupLocationRepository.findById(testLocation.id).get()

    // Create test waste stream
    testWasteStream = TestWasteStreamFactory.createTestWasteStreamDto(
      number = "123451321321",
      name = "Test Waste Stream",
      euralCode = testEural,
      processingMethod = testProcessingMethod,
      pickupLocation = managedLocation,
      processorPartyId = managedCompany,
      consignorParty = managedCompany,
      pickupParty = managedCompany
    )
    wasteStreamJpaRepository.save(testWasteStream)

    entityManager.flush()
  }

  @AfterEach
  fun cleanup() {
    transportRepository.deleteAll()
    wasteStreamJpaRepository.deleteAll()
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
    val request = ContainerTransportRequest(
      consignorPartyId = testCompany.id,
      pickupDateTime = LocalDateTime.now().plusDays(1),
      deliveryDateTime = LocalDateTime.now().plusDays(2),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.DELIVERY,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      pickupLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      deliveryLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "New Container Transport"
    )

    // When & Then
    val createResponse = securedMockMvc.post(
      "/transport/container",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andReturn()


    val createResult = objectMapper.readValue(
      createResponse.response.contentAsString,
      CreateContainerTransportResponse::class.java
    )
    // Retrieve and verify the updated transport
    securedMockMvc.get("/transport/${createResult.transportId}")
      .andExpect(status().isOk)
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
    val request = WasteTransportRequest(
      pickupDateTime = LocalDateTime.now().plusDays(1),
      deliveryDateTime = LocalDateTime.now().plusDays(2),
      transportType = TransportType.WASTE,
      containerOperation = ContainerOperation.PICKUP,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "New Waste Transport",
      goods = listOf(
        GoodsRequest(
          wasteStreamNumber = testWasteStream.number,
          weight = 1000.0,
          unit = "KG",
          quantity = 1,
        )
      ),
    )

    // When & Then
    val createResponse = securedMockMvc.post(
      "/transport/waste",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.transportId").exists())
      .andReturn()

    val createResult = objectMapper.readValue(
      createResponse.response.contentAsString,
      CreateWasteTransportResponse::class.java
    )

    // Verify transport was saved in the database
    val savedTransports = transportRepository.findAll()
    assertThat(savedTransports).hasSize(1)
    assertThat(savedTransports[0].id).isEqualTo(createResult.transportId)
    assertThat(savedTransports[0].note).isEqualTo("New Waste Transport")
    assertThat(savedTransports[0].transportType).isEqualTo(TransportType.WASTE)
    assertThat(savedTransports[0].goods?.first()?.wasteStreamNumber).isEqualTo(testWasteStream.number)
    assertThat(savedTransports[0].containerOperation).isEqualTo(ContainerOperation.PICKUP)
  }

  @Test
  fun `should update container transport`() {
    // Given
    val transport = createTestTransport("Original Transport")
    val savedTransport = transportRepository.save(transport)

    val updateRequest = ContainerTransportRequest(
      consignorPartyId = testCompany.id,
      pickupDateTime = LocalDateTime.now().plusDays(3),
      deliveryDateTime = LocalDateTime.now().plusDays(4),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.EXCHANGE,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      pickupLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      deliveryLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "Updated Container Transport"
    )

    // When & Then
    securedMockMvc.put(
      "/transport/container/${savedTransport.id}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))

    // Verify transport was updated in the database
    val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id)
    assertThat(updatedTransport).isNotNull
    assertThat(updatedTransport?.note).isEqualTo("Updated Container Transport")
    assertThat(updatedTransport?.containerOperation).isEqualTo(ContainerOperation.EXCHANGE)
  }

  @Test
  fun `should update waste transport`() {
    // Given - create initial waste transport
    val createRequest = WasteTransportRequest(
      pickupDateTime = LocalDateTime.now().plusDays(1),
      deliveryDateTime = LocalDateTime.now().plusDays(2),
      transportType = TransportType.WASTE,
      containerOperation = ContainerOperation.PICKUP,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "Original Waste Transport",
      goods = listOf(
        GoodsRequest(
          wasteStreamNumber = testWasteStream.number,
          weight = 1000.0,
          unit = "KG",
          quantity = 1,
        )
      ),
    )

    val createResponse = securedMockMvc.post(
      "/transport/waste",
      objectMapper.writeValueAsString(createRequest)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val createResult = objectMapper.readValue(
      createResponse.response.contentAsString,
      CreateWasteTransportResponse::class.java
    )

    // Update the transport
    val updateRequest = WasteTransportRequest(
      pickupDateTime = LocalDateTime.now().plusDays(3),
      deliveryDateTime = LocalDateTime.now().plusDays(4),
      transportType = TransportType.WASTE,
      containerOperation = ContainerOperation.DELIVERY,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "Updated Waste Transport",
      goods = listOf(
        GoodsRequest(
          wasteStreamNumber = testWasteStream.number,
          weight = 2000.0,
          unit = "KG",
          quantity = 2,
        )
      ),
    )

    // When & Then
    securedMockMvc.put(
      "/transport/waste/${createResult.transportId}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.transportId").value(createResult.transportId.toString()))
      .andExpect(jsonPath("$.status").exists())

    // Verify transport was updated in the database
    val updatedTransport = transportRepository.findByIdOrNull(createResult.transportId)
    assertThat(updatedTransport).isNotNull
    assertThat(updatedTransport?.note).isEqualTo("Updated Waste Transport")
    assertThat(updatedTransport?.goods?.first()?.wasteStreamNumber).isEqualTo(testWasteStream.number)
    assertThat(updatedTransport?.goods?.first()?.netNetWeight).isEqualTo(2000.0)
    assertThat(updatedTransport?.goods?.first()?.quantity).isEqualTo(2)
    assertThat(updatedTransport?.containerOperation).isEqualTo(ContainerOperation.DELIVERY)
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
    assertThat(transportRepository.findByIdOrNull(savedTransport.id)).isNull()
  }

  @Test
  fun `should return not found when updating non-existent transport`() {
    // Given
    val nonExistentId = UUID.randomUUID()
    val updateRequest = ContainerTransportRequest(
      consignorPartyId = testCompany.id,
      pickupDateTime = LocalDateTime.now().plusDays(1),
      deliveryDateTime = LocalDateTime.now().plusDays(2),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.DELIVERY,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      pickupLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      deliveryLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
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
    val request = TransportController.TransportFinishedRequest(hours = 2.5, driverNote = "Driver note")

    // When & Then
    securedMockMvc.post(
      "/transport/${savedTransport.id}/finished",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isOk)

    // Verify transport was marked as finished in the database
    val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id)
    assertThat(updatedTransport).isNotNull
    assertThat(updatedTransport?.transportHours).isEqualTo(2.5)
    assertThat(updatedTransport?.getStatus()).isEqualTo(TransportDto.Status.FINISHED)
  }

  @Test
  fun `should return not found when marking non-existent transport as finished`() {
    // Given
    val nonExistentId = UUID.randomUUID()
    val request = TransportController.TransportFinishedRequest(hours = 2.0, "Driver note")

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
    val request = TransportController.TransportFinishedRequest(hours = 1.5, driverNote = "Driver note")

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
    val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id)
    assertThat(updatedTransport).isNotNull
    assertThat(updatedTransport?.transportHours).isNull()
    assertThat(updatedTransport?.getStatus()).isNotEqualTo(TransportDto.Status.FINISHED)
  }

  @Test
  fun `should allow admin to mark any transport as finished`() {
    // Given
    val transport = createTestTransport("Admin Access Transport")
    val savedTransport = transportRepository.save(transport.copy(driver = testDriver))
    val request = TransportController.TransportFinishedRequest(hours = 4.0, driverNote = "Driver note")

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
    val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id)
    assertThat(updatedTransport).isNotNull
    assertThat(updatedTransport?.transportHours).isEqualTo(4.0)
    assertThat(updatedTransport?.getStatus()).isEqualTo(TransportDto.Status.FINISHED)
  }

  @Test
  fun `should allow planner to mark any transport as finished`() {
    // Given
    val transport = createTestTransport("Planner Access Transport")
    val savedTransport = transportRepository.save(transport.copy(driver = testDriver))
    val request = TransportController.TransportFinishedRequest(hours = 2.75, driverNote = "Driver note")

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
    val updatedTransport = transportRepository.findByIdOrNull(savedTransport.id)
    assertThat(updatedTransport).isNotNull
    assertThat(updatedTransport?.transportHours).isEqualTo(2.75)
    assertThat(updatedTransport?.getStatus()).isEqualTo(TransportDto.Status.FINISHED)
  }

  @Test
  fun `should update container transport with branch references`() {
    // Given
    val createRequest = ContainerTransportRequest(
      consignorPartyId = testCompany.id,
      pickupDateTime = LocalDateTime.now().plusDays(1),
      deliveryDateTime = LocalDateTime.now().plusDays(2),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.DELIVERY,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      pickupLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      deliveryLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "Original Transport with Branches"
    )

    // Create the transport
    val createResponse = securedMockMvc.post(
      "/transport/container",
      objectMapper.writeValueAsString(createRequest)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val createResult = objectMapper.readValue(
      createResponse.response.contentAsString,
      CreateContainerTransportResponse::class.java
    )

    val updateRequest = ContainerTransportRequest(
      consignorPartyId = testCompany.id,
      pickupDateTime = LocalDateTime.now().plusDays(3),
      deliveryDateTime = LocalDateTime.now().plusDays(4),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.EXCHANGE,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      pickupLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      deliveryLocation = PickupLocationRequest.PickupCompanyRequest(
        companyId = testCompany.id,
      ),
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "Updated Transport with Branches"
    )

    // When & Then - Update the transport
    securedMockMvc.put(
      "/transport/container/${createResult.transportId}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))

    // Retrieve and verify the updated transport
    securedMockMvc.get("/transport/${createResult.transportId}")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.note").value("Updated Transport with Branches"))
      .andExpect(jsonPath("$.containerOperation").value("EXCHANGE"))
      .andExpect(jsonPath("$.transportType").value("CONTAINER"))
  }

  @Test
  fun `should fail when branch does not belong to company`() {
    // Given
    // Create a second company
    val anotherCompany = CompanyDto(
      id = UUID.randomUUID(),
      name = "Another Company",
      address = AddressDto(
        streetName = "Another Street",
        buildingNumber = "789",
        postalCode = "9012 EF",
        city = "Another City",
        country = "Nederland"
      )
    )
    companyRepository.save(anotherCompany)

    val request = ContainerTransportRequest(
      consignorPartyId = testCompany.id,
      pickupDateTime = LocalDateTime.now().plusDays(1),
      deliveryDateTime = LocalDateTime.now().plusDays(2),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.DELIVERY,
      driverId = testDriver.id,
      carrierPartyId = testCompany.id,
      pickupLocation = PickupLocationRequest.ProjectLocationRequest(
        id = anotherCompany.id,
        companyId = testCompany.id,
        streetName = "Branch Street",
        buildingNumber = "456",
        buildingNumberAddition = null,
        postalCode = "5678 CD",
        city = "Branch City",
        country = "Nederland",
      ),
      deliveryLocation = PickupLocationRequest.ProjectLocationRequest(
        id = testBranch.id,
        companyId = testCompany.id,
        streetName = "Branch Street",
        buildingNumber = "456",
        buildingNumberAddition = null,
        postalCode = "5678 CD",
        city = "Branch City",
        country = "Nederland",
      ),
      truckId = testTruck.licensePlate,
      containerId = testContainer.id,
      note = "Invalid Branch-Company Relationship"
    )

    // When & Then
    securedMockMvc.post(
      "/transport/container",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isNotFound)
      .andExpect(jsonPath("$.message").value(containsString("Geen projectlocatie gevonden met id")))

    // Verify no transport was saved
    val savedTransports = transportRepository.findAll()
    assertThat(savedTransports).isEmpty()
  }

  private fun createTestTransport(note: String, goodsItem: TransportGoodsDto? = null): TransportDto {
    return TransportDto(
      id = UUID.randomUUID(),
      consignorParty = testCompany,
      carrierParty = testCompany,
      pickupLocation = testLocation,
      pickupDateTime = LocalDateTime.now().plusDays(1).atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
      deliveryLocation = testLocation,
      deliveryDateTime = LocalDateTime.now().plusDays(2).atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
      transportType = TransportType.CONTAINER,
      containerOperation = ContainerOperation.DELIVERY,
      truck = testTruck,
      driver = testDriver,
      note = note,
      goods = goodsItem?.let { listOf(it) } ?: emptyList(),
      sequenceNumber = 1,
    )
  }
}
