package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWasteStreamFactory
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.support.TransactionTemplate

private const val WASTE_STREAM_NUMBER = "123456789012"

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WasteStreamControllerIntegrationTest {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var wasteStreamRepository: WasteStreamJpaRepository

  @Autowired
  private lateinit var companyRepository: CompanyRepository

  @Autowired
  private lateinit var transactionTemplate: TransactionTemplate

  private lateinit var testCompany: CompanyDto

  @BeforeAll
  fun setupOnce() {
    // Execute in a separate committed transaction
    transactionTemplate.execute {
      // Create and save company fresh (not detached)
      testCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
        processorId = "12345"
      ))
    }
  }

  @AfterAll
  fun cleanUpOnce() {
    // Execute in a separate committed transaction
    transactionTemplate.execute {
      companyRepository.deleteAll()
    }
  }

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    wasteStreamRepository.deleteAll()
  }

  @AfterEach
  fun cleanup() {
    wasteStreamRepository.deleteAll()
  }

  @Test
  fun `should create waste stream`() {
    // Given
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id!!,
      number = WASTE_STREAM_NUMBER,
      name = "Glass"
    )

    // When & Then
    securedMockMvc.post(
      "/waste-streams",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.wasteStreamNumber").value(WASTE_STREAM_NUMBER))

    // Verify waste stream was saved in the database
    val savedWasteStream = wasteStreamRepository.findById(WASTE_STREAM_NUMBER)
    assertThat(savedWasteStream).isPresent
    assertThat(savedWasteStream.get().number).isEqualTo(WASTE_STREAM_NUMBER)
    assertThat(savedWasteStream.get().name).isEqualTo("Glass")
  }

  @Test
  fun `should fail when duplicate waste stream`() {
    // Given
    val wasteStreamDto = TestWasteStreamFactory.createTestWasteStreamRequest(
      companyId = testCompany.id!!,
      number = WASTE_STREAM_NUMBER,
      name = "Glass"
    )
    securedMockMvc.post(
      "/waste-streams",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isCreated)

    // When & Then
    securedMockMvc.post(
      "/waste-streams",
      objectMapper.writeValueAsString(wasteStreamDto)
    )
      .andExpect(status().isConflict)
  }

}
