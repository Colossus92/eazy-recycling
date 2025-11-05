package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

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

  private lateinit var testCompany: CompanyDto

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    wasteStreamRepository.deleteAll()

    // Create test company for each test (will be rolled back after each test due to @Transactional)
    testCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
      processorId = "12345"
    ))
  }

  @Test
  fun `should create waste stream`() {
    // Given
    val wasteStreamRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id!!,
      name = "Glass"
    )

    // When & Then
    val result = securedMockMvc.post(
      "/waste-streams",
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
      companyId = testCompany.id!!,
      name = "Glass"
    )

    // When - create first waste stream
    val result1 = securedMockMvc.post(
      "/waste-streams",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val number1 = objectMapper.readTree(result1.response.contentAsString).get("wasteStreamNumber").asText()

    // When - create second waste stream (should get next sequential number)
    val result2 = securedMockMvc.post(
      "/waste-streams",
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
      companyId = testCompany.id!!,
      name = "Glass"
    )
    val secondRequest = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id!!,
      name = "Plastic"
    )

    securedMockMvc.post(
      "/waste-streams",
      objectMapper.writeValueAsString(firstRequest)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/waste-streams",
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
      companyId = testCompany.id!!,
      name = "Glass"
    )
    val createResult = securedMockMvc.post(
      "/waste-streams",
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
      companyId = testCompany.id!!,
      name = "Glass"
    )
    val createResult = securedMockMvc.post(
      "/waste-streams",
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
      "/waste-streams/${wasteStreamNumber}",
      objectMapper.writeValueAsString(updatedRequest)
    )
      .andExpect(status().isNoContent)
  }

  @Test
  fun `can delete created waste stream`() {
    // Given - create waste stream and extract generated number
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id!!,
      name = "Glass"
    )
    val createResult = securedMockMvc.post(
      "/waste-streams",
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

}
