package nl.eazysoftware.eazyrecyclingservice.controller.material

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.MaterialPriceRequest
import nl.eazysoftware.eazyrecyclingservice.repository.material.*
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
import java.time.temporal.ChronoUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialPriceControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var materialPriceJpaRepository: MaterialPriceJpaRepository

    @Autowired
    private lateinit var materialJpaRepository: MaterialJpaRepository

    @Autowired
    private lateinit var materialGroupJpaRepository: MaterialGroupJpaRepository

    @Autowired
    private lateinit var vatRateJpaRepository: VatRateJpaRepository

    private var testMaterialId: Long? = null
    private var testVatCode: String = "TEST_VAT"

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)

        // Create test VAT rate
        val vatRate = VatRateDto(
            vatCode = testVatCode,
            percentage = BigDecimal("21.00"),
            validFrom = Instant.now(),
            validTo = null,
            countryCode = "NL",
            description = "Test VAT rate"
        )
        vatRateJpaRepository.save(vatRate)

        // Create test material group
        val materialGroup = MaterialGroupDto(
            code = "TEST_GROUP",
            name = "Test Material Group",
            description = "For testing material prices",
            createdAt = Instant.now()
        )
        val savedGroup = materialGroupJpaRepository.save(materialGroup)

        // Create test material
        val material = MaterialDto(
            code = "MAT_PRICE_TEST",
            name = "Material for Price Testing",
            materialGroup = savedGroup,
            unitOfMeasure = "KG",
            vatRate = vatRate,
            status = "ACTIVE",
            createdAt = Instant.now()
        )
        val savedMaterial = materialJpaRepository.save(material)
        testMaterialId = savedMaterial.id
    }

    @AfterEach
    fun cleanup() {
        materialPriceJpaRepository.deleteAll()
        materialJpaRepository.deleteAll()
        materialGroupJpaRepository.deleteAll()
        vatRateJpaRepository.deleteAll()
    }

    @Test
    fun `should successfully create a material price with valid_to null`() {
        // Given
        val request = MaterialPriceRequest(
            materialId = testMaterialId!!,
            price = BigDecimal("10.50"),
            currency = "EUR"
        )

        // When & Then
        securedMockMvc.post(
            "/material-prices",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").isNumber)
            .andExpect(jsonPath("$.materialId").value(testMaterialId!!))
            .andExpect(jsonPath("$.price").value(10.50))
            .andExpect(jsonPath("$.currency").value("EUR"))
            .andExpect(jsonPath("$.validFrom").exists())
            .andExpect(jsonPath("$.validTo").isEmpty)

        // Verify price was saved with valid_to = null
        val savedPrices = materialPriceJpaRepository.findAll()
        assertThat(savedPrices).hasSize(1)
        assertThat(savedPrices[0].price).isEqualByComparingTo(BigDecimal("10.50"))
        assertThat(savedPrices[0].validTo).isNull()
    }

    @Test
    fun `should get all active prices - only returns prices with valid_to null or in future`() {
        // Given
        val material = materialJpaRepository.findById(testMaterialId!!).get()
        val now = Instant.now()

        // Active price (valid_to = null)
        val activePrice1 = MaterialPriceDto(
            material = material,
            price = BigDecimal("15.00"),
            currency = "EUR",
            validFrom = now.minus(10, ChronoUnit.DAYS),
            validTo = null
        )

        // Active price (valid_to in future)
        val activePrice2 = MaterialPriceDto(
            material = material,
            price = BigDecimal("20.00"),
            currency = "EUR",
            validFrom = now.minus(5, ChronoUnit.DAYS),
            validTo = now.plus(10, ChronoUnit.DAYS)
        )

        // Expired price (valid_to in past)
        val expiredPrice = MaterialPriceDto(
            material = material,
            price = BigDecimal("12.00"),
            currency = "EUR",
            validFrom = now.minus(20, ChronoUnit.DAYS),
            validTo = now.minus(1, ChronoUnit.DAYS)
        )

        materialPriceJpaRepository.saveAll(listOf(activePrice1, activePrice2, expiredPrice))

        // When & Then
        securedMockMvc.get("/material-prices")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.price == 15.00)]").exists())
            .andExpect(jsonPath("$[?(@.price == 20.00)]").exists())
            .andExpect(jsonPath("$[?(@.price == 12.00)]").doesNotExist())
    }

    @Test
    fun `should get price by id`() {
        // Given
        val material = materialJpaRepository.findById(testMaterialId!!).get()
        val price = MaterialPriceDto(
            material = material,
            price = BigDecimal("25.75"),
            currency = "USD",
            validFrom = Instant.now(),
            validTo = null
        )
        val saved = materialPriceJpaRepository.save(price)

        // When & Then
        securedMockMvc.get("/material-prices/${saved.id}")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(saved.id))
            .andExpect(jsonPath("$.price").value(25.75))
            .andExpect(jsonPath("$.currency").value("USD"))
    }

    @Test
    fun `should return not found when getting price with non-existent id`() {
        // When & Then
        securedMockMvc.get("/material-prices/99999")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should get active prices by material id`() {
        // Given
        val material = materialJpaRepository.findById(testMaterialId!!).get()
        val now = Instant.now()

        val activePrice = MaterialPriceDto(
            material = material,
            price = BigDecimal("30.00"),
            currency = "EUR",
            validFrom = now,
            validTo = null
        )

        val expiredPrice = MaterialPriceDto(
            material = material,
            price = BigDecimal("25.00"),
            currency = "EUR",
            validFrom = now.minus(10, ChronoUnit.DAYS),
            validTo = now.minus(1, ChronoUnit.DAYS)
        )

        materialPriceJpaRepository.saveAll(listOf(activePrice, expiredPrice))

        // When & Then
        securedMockMvc.get("/material-prices/material/${testMaterialId}")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].price").value(30.00))
    }

    @Test
    fun `should update price - set old valid_to to now and create new entry`() {
        // Given
        val material = materialJpaRepository.findById(testMaterialId!!).get()
        val originalPrice = MaterialPriceDto(
            material = material,
            price = BigDecimal("100.00"),
            currency = "EUR",
            validFrom = Instant.now().minus(5, ChronoUnit.DAYS),
            validTo = null
        )
        val saved = materialPriceJpaRepository.save(originalPrice)

        val updateRequest = MaterialPriceRequest(
            materialId = testMaterialId!!,
            price = BigDecimal("120.00"),
            currency = "EUR"
        )

        // When & Then
        val savedId = saved.id!!
        securedMockMvc.put(
            "/material-prices/$savedId",
            objectMapper.writeValueAsString(updateRequest)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.price").value(120.00))
            .andExpect(jsonPath("$.validTo").isEmpty)

        // Verify old price has valid_to set
        val oldPrice = materialPriceJpaRepository.findByIdOrNull(savedId)
        assertThat(oldPrice).isNotNull
        assertThat(oldPrice?.validTo).isNotNull()

        // Verify new price entry was created
        val allPrices = materialPriceJpaRepository.findAll()
        assertThat(allPrices).hasSize(2)
        
        val newPrice = allPrices.find { it.id != savedId }
        assertThat(newPrice).isNotNull
        assertThat(newPrice?.price).isEqualByComparingTo(BigDecimal("120.00"))
        assertThat(newPrice?.validTo).isNull()
    }

    @Test
    fun `should return not found when updating non-existent price`() {
        // Given
        val request = MaterialPriceRequest(
            materialId = testMaterialId!!,
            price = BigDecimal("50.00"),
            currency = "EUR"
        )

        // When & Then
        securedMockMvc.put(
            "/material-prices/99999",
            objectMapper.writeValueAsString(request)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete price - set valid_to to now`() {
        // Given
        val material = materialJpaRepository.findById(testMaterialId!!).get()
        val price = MaterialPriceDto(
            material = material,
            price = BigDecimal("75.00"),
            currency = "EUR",
            validFrom = Instant.now().minus(3, ChronoUnit.DAYS),
            validTo = null
        )
        val saved = materialPriceJpaRepository.save(price)

        // When & Then
        securedMockMvc.delete("/material-prices/${saved.id!!}")
            .andExpect(status().isNoContent)

        // Verify price still exists but has valid_to set
        val deletedPrice = materialPriceJpaRepository.findByIdOrNull(saved.id!!)
        assertThat(deletedPrice).isNotNull
        assertThat(deletedPrice?.validTo).isNotNull()
        assertThat(deletedPrice?.validTo).isBeforeOrEqualTo(Instant.now())
    }

    @Test
    fun `should return not found when deleting non-existent price`() {
        // When & Then
        securedMockMvc.delete("/material-prices/99999")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should validate required fields when creating price`() {
        // Given - missing required fields
        val invalidRequest = """
            {
                "materialId": null,
                "price": null,
                "currency": ""
            }
        """.trimIndent()

        // When & Then
        securedMockMvc.post(
            "/material-prices",
            invalidRequest
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should validate positive price`() {
        // Given
        val invalidRequest = MaterialPriceRequest(
            materialId = testMaterialId!!,
            price = BigDecimal("-10.00"),
            currency = "EUR"
        )

        // When & Then
        securedMockMvc.post(
            "/material-prices",
            objectMapper.writeValueAsString(invalidRequest)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should handle multiple price updates creating price history`() {
        // Given
        val material = materialJpaRepository.findById(testMaterialId!!).get()
        val initialPrice = MaterialPriceDto(
            material = material,
            price = BigDecimal("50.00"),
            currency = "EUR",
            validFrom = Instant.now().minus(10, ChronoUnit.DAYS),
            validTo = null
        )
        val saved = materialPriceJpaRepository.save(initialPrice)

        // First update
        val update1 = MaterialPriceRequest(
            materialId = testMaterialId!!,
            price = BigDecimal("60.00"),
            currency = "EUR"
        )
        val result1 = securedMockMvc.put(
            "/material-prices/${saved.id!!}",
            objectMapper.writeValueAsString(update1)
        )
            .andExpect(status().isOk)
            .andReturn()

        val newId1 = objectMapper.readTree(result1.response.contentAsString).get("id").asLong()

        // Second update
        val update2 = MaterialPriceRequest(
            materialId = testMaterialId!!,
            price = BigDecimal("70.00"),
            currency = "EUR"
        )
        securedMockMvc.put(
            "/material-prices/$newId1",
            objectMapper.writeValueAsString(update2)
        )
            .andExpect(status().isOk)

        // Verify we have 3 price entries (original + 2 updates)
        val allPrices = materialPriceJpaRepository.findAll()
        assertThat(allPrices).hasSize(3)

        // Verify only the latest has valid_to = null
        val activePrices = allPrices.filter { it.validTo == null }
        assertThat(activePrices).hasSize(1)
        assertThat(activePrices[0].price).isEqualByComparingTo(BigDecimal("70.00"))

        // Verify the other two have valid_to set
        val expiredPrices = allPrices.filter { it.validTo != null }
        assertThat(expiredPrices).hasSize(2)
    }
}
