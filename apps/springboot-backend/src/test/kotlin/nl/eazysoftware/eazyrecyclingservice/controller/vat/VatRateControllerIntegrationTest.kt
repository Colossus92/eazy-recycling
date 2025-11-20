package nl.eazysoftware.eazyrecyclingservice.controller.vat

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.VatRateRequest
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
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
import java.math.BigDecimal
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VatRateControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
  }

  @AfterEach
  fun cleanup() {
    vatRateJpaRepository.deleteAll()
  }

  @Test
  fun `should successfully create a VAT rate`() {
    // Given
    val vatRateRequest = VatRateRequest(
      vatCode = "NL_HIGH",
      percentage = "21.00",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = null,
      countryCode = "NL",
      description = "Dutch high VAT rate"
    )

    // When & Then
    securedMockMvc.post(
      "/vat-rates",
      objectMapper.writeValueAsString(vatRateRequest)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.vatCode").value("NL_HIGH"))
      .andExpect(jsonPath("$.percentage").value("21.00"))
      .andExpect(jsonPath("$.countryCode").value("NL"))
      .andExpect(jsonPath("$.description").value("Dutch high VAT rate"))

    // Verify VAT rate was saved in the database
    val savedVatRate = vatRateJpaRepository.findByIdOrNull("NL_HIGH")
    assertThat(savedVatRate).isNotNull
    assertThat(savedVatRate?.percentage).isEqualTo(BigDecimal("21.00"))
    assertThat(savedVatRate?.countryCode).isEqualTo("NL")
  }

  @Test
  fun `should get all VAT rates`() {
    // Given
    val vatRate1 = VatRateDto(
      vatCode = "NL_HIGH",
      percentage = BigDecimal("21.00"),
      validFrom = Instant.parse("2024-01-01T00:00:00Z"),
      validTo = null,
      countryCode = "NL",
      description = "Dutch high VAT rate"
    )
    val vatRate2 = VatRateDto(
      vatCode = "NL_LOW",
      percentage = BigDecimal("9.00"),
      validFrom = Instant.parse("2024-01-01T00:00:00Z"),
      validTo = null,
      countryCode = "NL",
      description = "Dutch low VAT rate"
    )
    vatRateJpaRepository.saveAll(listOf(vatRate1, vatRate2))

    // When & Then
    securedMockMvc.get("/vat-rates")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[?(@.vatCode == 'NL_HIGH')]").exists())
      .andExpect(jsonPath("$[?(@.vatCode == 'NL_LOW')]").exists())
  }

  @Test
  fun `should get VAT rate by code`() {
    // Given
    val vatRate = VatRateDto(
      vatCode = "BE_HIGH",
      percentage = BigDecimal("21.00"),
      validFrom = Instant.parse("2024-01-01T00:00:00Z"),
      validTo = null,
      countryCode = "BE",
      description = "Belgian high VAT rate"
    )
    vatRateJpaRepository.save(vatRate)

    // When & Then
    securedMockMvc.get("/vat-rates/BE_HIGH")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.vatCode").value("BE_HIGH"))
      .andExpect(jsonPath("$.percentage").value("21.00"))
      .andExpect(jsonPath("$.countryCode").value("BE"))
      .andExpect(jsonPath("$.description").value("Belgian high VAT rate"))
  }

  @Test
  fun `should return not found when getting VAT rate with non-existent code`() {
    // When & Then
    securedMockMvc.get("/vat-rates/NON_EXISTENT")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should update VAT rate`() {
    // Given
    val originalVatRate = VatRateDto(
      vatCode = "DE_HIGH",
      percentage = BigDecimal("19.00"),
      validFrom = Instant.parse("2024-01-01T00:00:00Z"),
      validTo = null,
      countryCode = "DE",
      description = "German high VAT rate"
    )
    vatRateJpaRepository.save(originalVatRate)

    val updatedVatRateRequest = VatRateRequest(
      vatCode = "DE_HIGH",
      percentage = "20.00",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = "2024-12-31T23:59:59Z",
      countryCode = "DE",
      description = "German high VAT rate (updated)"
    )

    // When & Then
    securedMockMvc.put(
      "/vat-rates/DE_HIGH",
      objectMapper.writeValueAsString(updatedVatRateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.vatCode").value("DE_HIGH"))
      .andExpect(jsonPath("$.percentage").value("20.00"))
      .andExpect(jsonPath("$.description").value("German high VAT rate (updated)"))

    // Verify VAT rate was updated in the database
    val savedVatRate = vatRateJpaRepository.findByIdOrNull("DE_HIGH")
    assertThat(savedVatRate).isNotNull
    assertThat(savedVatRate?.percentage).isEqualTo(BigDecimal("20.00"))
    assertThat(savedVatRate?.description).isEqualTo("German high VAT rate (updated)")
    assertThat(savedVatRate?.validTo).isNotNull()
  }

  @Test
  fun `should return not found when updating non-existent VAT rate`() {
    // Given
    val vatRateRequest = VatRateRequest(
      vatCode = "NON_EXISTENT",
      percentage = "21.00",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = null,
      countryCode = "NL",
      description = "Non-existent VAT rate"
    )

    // When & Then
    securedMockMvc.put(
      "/vat-rates/NON_EXISTENT",
      objectMapper.writeValueAsString(vatRateRequest)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should delete VAT rate`() {
    // Given
    val vatRate = VatRateDto(
      vatCode = "DELETE_ME",
      percentage = BigDecimal("21.00"),
      validFrom = Instant.parse("2024-01-01T00:00:00Z"),
      validTo = null,
      countryCode = "NL",
      description = "VAT rate to be deleted"
    )
    vatRateJpaRepository.save(vatRate)

    // When & Then
    securedMockMvc.delete("/vat-rates/DELETE_ME")
      .andExpect(status().isNoContent)

    // Verify VAT rate was deleted
    assertThat(vatRateJpaRepository.findByIdOrNull("DELETE_ME")).isNull()
  }

  @Test
  fun `should return not found when deleting non-existent VAT rate`() {
    // When & Then
    securedMockMvc.delete("/vat-rates/NON_EXISTENT")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should create VAT rate with valid_to date`() {
    // Given
    val vatRateRequest = VatRateRequest(
      vatCode = "FR_OLD",
      percentage = "19.60",
      validFrom = "2014-01-01T00:00:00Z",
      validTo = "2023-12-31T23:59:59Z",
      countryCode = "FR",
      description = "Old French VAT rate"
    )

    // When & Then
    securedMockMvc.post(
      "/vat-rates",
      objectMapper.writeValueAsString(vatRateRequest)
    )
      .andExpect(status().isCreated)
      .andExpect(jsonPath("$.vatCode").value("FR_OLD"))
      .andExpect(jsonPath("$.validTo").exists())

    // Verify VAT rate was saved with validTo
    val savedVatRate = vatRateJpaRepository.findByIdOrNull("FR_OLD")
    assertThat(savedVatRate).isNotNull
    assertThat(savedVatRate?.validTo).isNotNull()
  }

  @Test
  fun `should validate required fields when creating VAT rate`() {
    // Given - missing required fields
    val invalidVatRateRequest = """
      {
        "vatCode": "",
        "percentage": "",
        "validFrom": "",
        "countryCode": "",
        "description": ""
      }
    """.trimIndent()

    // When & Then
    securedMockMvc.post(
      "/vat-rates",
      invalidVatRateRequest
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should validate percentage format`() {
    // Given - invalid percentage format
    val invalidVatRateRequest = VatRateRequest(
      vatCode = "INVALID",
      percentage = "not-a-number",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = null,
      countryCode = "NL",
      description = "Invalid percentage"
    )

    // When & Then
    securedMockMvc.post(
      "/vat-rates",
      objectMapper.writeValueAsString(invalidVatRateRequest)
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should handle multiple VAT rates for same country`() {
    // Given
    val highRate = VatRateRequest(
      vatCode = "NL_HIGH_2024",
      percentage = "21.00",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = null,
      countryCode = "NL",
      description = "Dutch high VAT rate 2024"
    )
    val lowRate = VatRateRequest(
      vatCode = "NL_LOW_2024",
      percentage = "9.00",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = null,
      countryCode = "NL",
      description = "Dutch low VAT rate 2024"
    )

    // When
    securedMockMvc.post(
      "/vat-rates",
      objectMapper.writeValueAsString(highRate)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/vat-rates",
      objectMapper.writeValueAsString(lowRate)
    ).andExpect(status().isCreated)

    // Then - verify both rates exist
    val allRates = vatRateJpaRepository.findAll()
    assertThat(allRates).hasSize(2)
    assertThat(allRates.map { it.vatCode }).containsExactlyInAnyOrder("NL_HIGH_2024", "NL_LOW_2024")
  }

  @Test
  fun `should handle decimal percentages correctly`() {
    // Given
    val vatRateRequest = VatRateRequest(
      vatCode = "DECIMAL_TEST",
      percentage = "21.50",
      validFrom = "2024-01-01T00:00:00Z",
      validTo = null,
      countryCode = "NL",
      description = "Decimal percentage test"
    )

    // When & Then
    securedMockMvc.post(
      "/vat-rates",
      objectMapper.writeValueAsString(vatRateRequest)
    )
      .andExpect(status().isCreated)
      .andExpect(jsonPath("$.percentage").value("21.50"))

    // Verify precision is maintained
    val savedVatRate = vatRateJpaRepository.findByIdOrNull("DECIMAL_TEST")
    assertThat(savedVatRate?.percentage).isEqualTo(BigDecimal("21.50"))
  }
}
