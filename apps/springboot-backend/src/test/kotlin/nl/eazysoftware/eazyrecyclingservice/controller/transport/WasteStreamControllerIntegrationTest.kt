package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWeightTicketFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class WasteStreamControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreamJpaRepository

  @Autowired
  private lateinit var companyRepository: CompanyJpaRepository

  @Autowired
  private lateinit var weightTicketRepository: WeightTicketJpaRepository

  @Autowired
  private lateinit var catalogItemRepository: CatalogItemJpaRepository

  @Autowired
  private lateinit var vatRateRepository: VatRateJpaRepository

  private lateinit var testCompany: CompanyDto
  private lateinit var tenantCompany: CompanyDto
  private lateinit var testCatalogItem: CatalogItemDto
  private lateinit var testVatRate: VatRateDto

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    weightTicketRepository.deleteAll()
    wasteStreamRepository.deleteAll()

    // Create tenant company (processor ID 08797)
    tenantCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "08797",
      vihbId = "087970VIHB"
    ))

    // Create test company for each test (will be rolled back after each test due to @Transactional)
    testCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "12345",
      vihbId = "123456VIHB"
    ))

    // Create test VAT rate and catalog item for weight ticket lines
    testVatRate = vatRateRepository.save(
      VatRateDto(
        vatCode = "VAT21",
        percentage = BigDecimal("21"),
        validFrom = Instant.now().minusSeconds(86400),
        validTo = null,
        countryCode = "NL",
        description = "Standard VAT 21%",
        taxScenario = "STANDARD",
      )
    )

    testCatalogItem = catalogItemRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        code = "WASTE001",
        name = "Test Waste Stream",
        type = CatalogItemType.WASTE_STREAM,
        unitOfMeasure = "kg",
        vatRate = testVatRate,
        category = null,
        consignorParty = null,
        defaultPrice = BigDecimal("10.00"),
        status = "ACTIVE",
        purchaseAccountNumber = "7000",
        salesAccountNumber = "8000",
      )
    )
  }

  @Test
  fun `should create waste stream`() {
    // Given
    val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )

    // When & Then
    val result = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamRequest)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.wasteStreamNumber").exists())
      .andReturn()

    // Extract the generated waste stream number from response
    val response = objectMapper.readTree(result.response.contentAsString)
    val generatedNumber = response.get("wasteStreamNumber").asText()

    // Verify waste stream was saved in the database
    val savedWasteStream = wasteStreamRepository.findById(generatedNumber)
    assertThat(savedWasteStream).isPresent
    assertThat(savedWasteStream.get().number).isEqualTo(generatedNumber)
    assertThat(savedWasteStream.get().name).isEqualTo("Glass")
  }

  @Test
  fun `should not fail when creating multiple waste streams for same processor`() {
    // Given - waste stream numbers are now auto-generated sequentially
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )

    // When - create first waste stream
    val result1 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val number1 = objectMapper.readTree(result1.response.contentAsString).get("wasteStreamNumber").asText()

    // When - create second waste stream (should get next sequential number)
    val result2 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val number2 = objectMapper.readTree(result2.response.contentAsString).get("wasteStreamNumber").asText()

    // Then - numbers should be sequential
    assertThat(number1).isNotEqualTo(number2)
    assertThat(number1.substring(0, 5)).isEqualTo(number2.substring(0, 5)) // Same processor prefix
  }

  @Test
  fun `can get all waste streams`() {
    // Given - create multiple waste streams
    val firstRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val secondRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Plastic"
    )

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(firstRequest)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(secondRequest)
    ).andExpect(status().isCreated)

    // When & Then
    securedMockMvc.get("/waste-streams")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].wasteStreamNumber").exists())
      .andExpect(jsonPath("$[1].wasteStreamNumber").exists())
  }

  @Test
  fun `can get waste stream by number with full details`() {
    // Given - create waste stream and extract generated number
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val createResult = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val wasteStreamNumber = objectMapper.readTree(createResult.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then
    securedMockMvc.get("/waste-streams/${wasteStreamNumber}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.wasteStreamNumber").value(wasteStreamNumber))
      .andExpect(jsonPath("$.wasteType.name").value("Glass"))
      .andExpect(jsonPath("$.wasteType.euralCode.code").exists())
      .andExpect(jsonPath("$.wasteType.processingMethod.code").exists())
      .andExpect(jsonPath("$.consignorParty.type").value("company"))
      .andExpect(jsonPath("$.pickupParty.name").exists())
      .andExpect(jsonPath("$.deliveryLocation.processor.name").exists())
  }

  @Test
  fun `can update created waste stream`() {
    // Given - create waste stream and extract generated number
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val createResult = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val wasteStreamNumber = objectMapper.readTree(createResult.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When
    val updatedRequest = wasteStreamDto.copy(name = "Updated Glass")

    // Then
    securedMockMvc.put(
      "/waste-streams/${wasteStreamNumber}/concept",
      objectMapper.writeValueAsString(updatedRequest)
    )
      .andExpect(status().isNoContent)
  }

  @Test
  fun `can delete created waste stream`() {
    // Given - create waste stream and extract generated number
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val createResult = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val wasteStreamNumber = objectMapper.readTree(createResult.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then
    securedMockMvc.delete(
      "/waste-streams/${wasteStreamNumber}"
    )
      .andExpect(status().isNoContent)
  }

  @Test
  fun `can filter waste streams by consignor`() {
    // Given - create two companies
    val company1 = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "11111",
      chamberOfCommerceId = "07082025",
      vihbId = "856974VIXX",
    ))
    val company2 = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "22222",
      chamberOfCommerceId = "01022021",
      vihbId = "856974VIXB"
    ))

    // Create waste streams with different consignors
    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass",
      consignorPartyId = company1.id
    )
    val request2 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Plastic",
      consignorPartyId = company2.id
    )

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request2)
    ).andExpect(status().isCreated)

    // When & Then - filter by company1
    securedMockMvc.get("/waste-streams?consignor=${company1.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].consignorPartyId").value(company1.id.toString()))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))

    // When & Then - filter by company2
    securedMockMvc.get("/waste-streams?consignor=${company2.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].consignorPartyId").value(company2.id.toString()))
      .andExpect(jsonPath("$[0].wasteName").value("Plastic"))
  }

  @Test
  fun `can filter waste streams by status`() {
    // Given - create waste stream in DRAFT status
    val draftRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(draftRequest)
    ).andExpect(status().isCreated)

    // When & Then - filter by DRAFT status
    securedMockMvc.get("/waste-streams?status=DRAFT")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].status").value("DRAFT"))
  }

  @Test
  fun `can filter waste streams by both consignor and status`() {
    // Given - create two companies
    val company1 = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "11111",
      chamberOfCommerceId = "07082025",
      vihbId = "856974VIXX",
    ))
    val company2 = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "22222",
      chamberOfCommerceId = "01022021",
      vihbId = "856974VIXB"
    ))

    // Create waste streams with different consignors
    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass",
      consignorPartyId = company1.id
    )
    val request2 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Plastic",
      consignorPartyId = company2.id
    )

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request2)
    ).andExpect(status().isCreated)

    // When & Then - filter by company1 and DRAFT status
    securedMockMvc.get("/waste-streams?consignor=${company1.id}&status=DRAFT")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].consignorPartyId").value(company1.id.toString()))
      .andExpect(jsonPath("$[0].status").value("DRAFT"))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))

    // When & Then - filter by company2 and DRAFT status
    securedMockMvc.get("/waste-streams?consignor=${company2.id}&status=DRAFT")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].consignorPartyId").value(company2.id.toString()))
      .andExpect(jsonPath("$[0].status").value("DRAFT"))
      .andExpect(jsonPath("$[0].wasteName").value("Plastic"))
  }

  @Test
  fun `returns empty list when filtering by non-existent consignor`() {
    // Given - create a waste stream
    val request = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request)
    ).andExpect(status().isCreated)

    // When & Then - filter by non-existent consignor
    val nonExistentId = "00000000-0000-0000-0000-000000000000"
    securedMockMvc.get("/waste-streams?consignor=$nonExistentId")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(0))
  }

  @Test
  fun `returns empty list when filtering by status that does not match`() {
    // Given - create a waste stream in DRAFT status
    val request = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request)
    ).andExpect(status().isCreated)

    // When & Then - filter by ACTIVE status (waste stream is DRAFT)
    securedMockMvc.get("/waste-streams?status=ACTIVE")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(0))
  }

  @Test
  fun `can get compatible waste streams with same pickup location, processor and consignor`() {
    // Given - create a waste stream
    val pickupLocation = PickupLocationRequest.DutchAddressRequest(
      streetName = "Main Street",
      buildingNumber = "100",
      buildingNumberAddition = null,
      postalCode = "1234AB",
      city = "Amsterdam",
      country = "Nederland"
    )

    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000001",
      name = "Glass",
      pickupLocation = pickupLocation,
      processorPartyId = "12345",
      consignorPartyId = testCompany.id
    )

    val result1 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated).andReturn()

    val wasteStreamNumber1 = objectMapper.readTree(result1.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then - get compatible waste streams (should return itself since it matches all criteria)
    securedMockMvc.get("/waste-streams/$wasteStreamNumber1/compatible")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))
      .andExpect(jsonPath("$[0].wasteStreamNumber").value(wasteStreamNumber1))
  }

  @Test
  fun `returns only waste streams with same pickup location when getting compatible waste streams`() {
    // Given - create two waste streams with different pickup locations
    val pickupLocation1 = PickupLocationRequest.DutchAddressRequest(
      streetName = "Main Street",
      buildingNumber = "100",
      buildingNumberAddition = null,
      postalCode = "1234AB",
      city = "Amsterdam",
      country = "Nederland"
    )
    val pickupLocation2 = PickupLocationRequest.DutchAddressRequest(
      streetName = "Other Street",
      buildingNumber = "200",
      buildingNumberAddition = null,
      postalCode = "5678CD",
      city = "Rotterdam",
      country = "Nederland"
    )

    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000002",
      name = "Glass",
      pickupLocation = pickupLocation1,
      processorPartyId = "12345",
      consignorPartyId = testCompany.id
    )
    val request2 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000003",
      name = "Plastic",
      pickupLocation = pickupLocation2, // Different location
      processorPartyId = "12345",
      consignorPartyId = testCompany.id
    )

    val result1 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated).andReturn()

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request2)
    ).andExpect(status().isCreated)

    val wasteStreamNumber1 = objectMapper.readTree(result1.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then - get compatible waste streams (should only return the first one)
    securedMockMvc.get("/waste-streams/$wasteStreamNumber1/compatible")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))
  }

  @Test
  fun `returns only waste streams with same processor when getting compatible waste streams`() {
    // Given - create two waste streams with different processor IDs
    val pickupLocation = PickupLocationRequest.DutchAddressRequest(
      streetName = "Main Street",
      buildingNumber = "100",
      buildingNumberAddition = null,
      postalCode = "1234AB",
      city = "Amsterdam",
      country = "Nederland"
    )

    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000004",
      name = "Glass",
      pickupLocation = pickupLocation,
      processorPartyId = "12345",
      consignorPartyId = testCompany.id
    )
    val request2 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "999990000001",
      name = "Plastic",
      pickupLocation = pickupLocation,
      processorPartyId = "99999", // Different processor
      consignorPartyId = testCompany.id
    )

    val result1 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated).andReturn()

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request2)
    )

    val wasteStreamNumber1 = objectMapper.readTree(result1.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then - get compatible waste streams (should only return the first one due to different processor)
    securedMockMvc.get("/waste-streams/$wasteStreamNumber1/compatible")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))
  }

  @Test
  fun `returns only waste streams with same consignor when getting compatible waste streams`() {
    // Given - create two companies as consignors
    val consignor1 = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "11111",
      chamberOfCommerceId = "11111111",
      vihbId = "11111VIXXX"
    ))
    val consignor2 = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "22222",
      chamberOfCommerceId = "22222222",
      vihbId = "22222VIXXX"
    ))

    val pickupLocation = PickupLocationRequest.DutchAddressRequest(
      streetName = "Main Street",
      buildingNumber = "100",
      buildingNumberAddition = null,
      postalCode = "1234AB",
      city = "Amsterdam",
      country = "Nederland"
    )

    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000005",
      name = "Glass",
      pickupLocation = pickupLocation,
      processorPartyId = "12345",
      consignorPartyId = consignor1.id
    )
    val request2 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000006",
      name = "Plastic",
      pickupLocation = pickupLocation,
      processorPartyId = "12345",
      consignorPartyId = consignor2.id // Different consignor
    )

    val result1 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated).andReturn()

    securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request2)
    ).andExpect(status().isCreated)

    val wasteStreamNumber1 = objectMapper.readTree(result1.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then - get compatible waste streams (should only return the first one)
    securedMockMvc.get("/waste-streams/$wasteStreamNumber1/compatible")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))
  }

  @Test
  fun `returns 404 when getting compatible waste streams for non-existent waste stream number`() {
    // Given - a non-existent waste stream number
    val nonExistentNumber = "999999999999"

    // When & Then
    securedMockMvc.get("/waste-streams/$nonExistentNumber/compatible")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `returns 400 when getting compatible waste streams with invalid waste stream number format`() {
    // Given - an invalid waste stream number (not 12 digits)
    val invalidNumber = "12345"

    // When & Then
    securedMockMvc.get("/waste-streams/$invalidNumber/compatible")
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `compatible waste streams include waste streams with different eural codes but same location and parties`() {
    // Given - create a waste stream
    val pickupLocation = PickupLocationRequest.DutchAddressRequest(
      streetName = "Main Street",
      buildingNumber = "100",
      buildingNumberAddition = null,
      postalCode = "1234AB",
      city = "Amsterdam",
      country = "Nederland"
    )

    val request1 = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      wasteStreamNumber = "123450000007",
      name = "Glass",
      euralCode = "16 01 17",
      pickupLocation = pickupLocation,
      processorPartyId = "12345",
      consignorPartyId = testCompany.id
    )

    val result1 = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated).andReturn()

    val wasteStreamNumber1 = objectMapper.readTree(result1.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then - should return itself (eural code doesn't affect compatibility, only location and parties)
    securedMockMvc.get("/waste-streams/$wasteStreamNumber1/compatible")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].euralCode").value("16 01 17"))
      .andExpect(jsonPath("$[0].wasteName").value("Glass"))
  }

  @Test
  fun `can get weight tickets by waste stream number`() {
    // Given - create a waste stream
    val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val createWasteStreamResult = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamRequest)
    ).andExpect(status().isCreated).andReturn()

    val wasteStreamNumber = objectMapper.readTree(createWasteStreamResult.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // Create a weight ticket with a line for this waste stream
    val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
      consignorCompanyId = testCompany.id,
      lines = listOf(
        TestWeightTicketFactory.createTestWeightTicketLine(
          wasteStreamNumber = wasteStreamNumber,
          weightValue = "150.75",
          catalogItemId = testCatalogItem.id
        )
      )
    )

    securedMockMvc.post(
      "/weight-tickets",
      objectMapper.writeValueAsString(weightTicketRequest)
    ).andExpect(status().isCreated)

    // When & Then - get weight tickets for this waste stream
    securedMockMvc.get("/waste-streams/$wasteStreamNumber/weight-tickets")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].weightTicketNumber").exists())
      .andExpect(jsonPath("$[0].amount").value(150.75))
  }

  @Test
  fun `returns empty list when no weight tickets exist for waste stream`() {
    // Given - create a waste stream without any weight tickets
    val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val createWasteStreamResult = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamRequest)
    ).andExpect(status().isCreated).andReturn()

    val wasteStreamNumber = objectMapper.readTree(createWasteStreamResult.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // When & Then - get weight tickets for this waste stream
    securedMockMvc.get("/waste-streams/$wasteStreamNumber/weight-tickets")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(0))
  }

  @Test
  fun `returns multiple weight tickets for same waste stream`() {
    // Given - create a waste stream
    val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id,
      name = "Glass"
    )
    val createWasteStreamResult = securedMockMvc.post(
      "/waste-streams/concept",
      objectMapper.writeValueAsString(wasteStreamRequest)
    ).andExpect(status().isCreated).andReturn()

    val wasteStreamNumber = objectMapper.readTree(createWasteStreamResult.response.contentAsString)
      .get("wasteStreamNumber").asText()

    // Create multiple weight tickets with lines for this waste stream
    val weightTicketRequest1 = TestWeightTicketFactory.createTestWeightTicketRequest(
      consignorCompanyId = testCompany.id,
      lines = listOf(
        TestWeightTicketFactory.createTestWeightTicketLine(
          wasteStreamNumber = wasteStreamNumber,
          weightValue = "100.00",
          catalogItemId = testCatalogItem.id
        )
      )
    )
    val weightTicketRequest2 = TestWeightTicketFactory.createTestWeightTicketRequest(
      consignorCompanyId = testCompany.id,
      lines = listOf(
        TestWeightTicketFactory.createTestWeightTicketLine(
          wasteStreamNumber = wasteStreamNumber,
          weightValue = "200.00",
          catalogItemId = testCatalogItem.id
        )
      )
    )

    securedMockMvc.post(
      "/weight-tickets",
      objectMapper.writeValueAsString(weightTicketRequest1)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/weight-tickets",
      objectMapper.writeValueAsString(weightTicketRequest2)
    ).andExpect(status().isCreated)

    // When & Then - get weight tickets for this waste stream
    securedMockMvc.get("/waste-streams/$wasteStreamNumber/weight-tickets")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(2))
  }

}
