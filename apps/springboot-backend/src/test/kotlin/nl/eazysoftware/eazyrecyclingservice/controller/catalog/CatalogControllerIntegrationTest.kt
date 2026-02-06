package nl.eazysoftware.eazyrecyclingservice.controller.catalog

import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var catalogItemJpaRepository: CatalogItemJpaRepository

  @Autowired
  private lateinit var catalogItemCategoryJpaRepository: CatalogItemCategoryJpaRepository

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  private var testCategoryId: UUID? = null
  private val testVatRateId: UUID = UUID.randomUUID()

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    catalogItemJpaRepository.deleteAll()
    catalogItemCategoryJpaRepository.deleteAll()

    if (!vatRateJpaRepository.existsById(testVatRateId)) {
      vatRateJpaRepository.save(
        VatRateDto(
          id = testVatRateId,
          vatCode = "VAT21",
          percentage = BigDecimal("21.00"),
          validFrom = Instant.now(),
          validTo = null,
          countryCode = "NL",
          description = "21% BTW",
          taxScenario = "STANDARD",
        )
      )
    }

    val category = catalogItemCategoryJpaRepository.save(
      CatalogItemCategoryDto(
        id = UUID.randomUUID(),
        type = "MATERIAL",
        code = "TEST_GROUP",
        name = "Test Category",
        description = "Test Description"
      )
    )
    testCategoryId = category.id
  }

  @Test
  fun `should search catalog items by query`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.MATERIAL,
        code = "MAT001",
        name = "Steel Pipes",
        category = category,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = "8000",
        purchaseAccountNumber = null,
        defaultPrice = null,
        status = "ACTIVE"
      )
    )

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Steel Product",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = "8100",
        purchaseAccountNumber = null,
        defaultPrice = BigDecimal("50.00"),
        status = "ACTIVE"
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/items?query=Steel")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(2))
  }

  @Test
  fun `should return all catalog items when no query provided`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    repeat(3) { i ->
      catalogItemJpaRepository.save(
        CatalogItemDto(
          id = UUID.randomUUID(),
          type = CatalogItemType.MATERIAL,
          code = "MAT00$i",
          name = "Material $i",
          category = category,
          consignorParty = null,
          unitOfMeasure = "KG",
          vatRate = vatRate,
          salesAccountNumber = null,
          purchaseAccountNumber = null,
          defaultPrice = null,
          status = "ACTIVE"
        )
      )
    }

    // When & Then
    securedMockMvc.get("/catalog/items")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(3))
  }

  @Test
  fun `should return catalog item with correct response fields`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.MATERIAL,
        code = "MAT001",
        name = "Steel Pipes",
        category = category,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = "8000",
        purchaseAccountNumber = "7000",
        defaultPrice = BigDecimal("25.00"),
        status = "ACTIVE"
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/items?query=Steel")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].code").value("MAT001"))
      .andExpect(jsonPath("$[0].name").value("Steel Pipes"))
      .andExpect(jsonPath("$[0].itemType").value("MATERIAL"))
      .andExpect(jsonPath("$[0].unitOfMeasure").value("KG"))
      .andExpect(jsonPath("$[0].vatCode").value("VAT21"))
      .andExpect(jsonPath("$[0].categoryName").value("Test Category"))
      .andExpect(jsonPath("$[0].salesAccountNumber").value("8000"))
      .andExpect(jsonPath("$[0].purchaseAccountNumber").value("7000"))
      .andExpect(jsonPath("$[0].defaultPrice").value(25.00))
  }

  @Test
  fun `should return empty list when no matches found`() {
    // When & Then
    securedMockMvc.get("/catalog/items?query=NonExistent")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))
  }

  @Test
  fun `should negate PRODUCT prices for PURCHASE invoice type`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Transport Hours",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = "8100",
        purchaseAccountNumber = null,
        defaultPrice = BigDecimal("50.00"),
        status = "ACTIVE"
      )
    )

    // When & Then - PURCHASE invoice type should negate PRODUCT prices
    securedMockMvc.get("/catalog/items?invoiceType=PURCHASE")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].itemType").value("PRODUCT"))
      .andExpect(jsonPath("$[0].defaultPrice").value(-50.00))
  }

  @Test
  fun `should NOT negate MATERIAL prices for PURCHASE invoice type`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.MATERIAL,
        code = "MAT001",
        name = "Steel Scrap",
        category = category,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = "8000",
        purchaseAccountNumber = null,
        defaultPrice = BigDecimal("25.00"),
        status = "ACTIVE"
      )
    )

    // When & Then - PURCHASE invoice type should NOT negate MATERIAL prices
    securedMockMvc.get("/catalog/items?invoiceType=PURCHASE")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].itemType").value("MATERIAL"))
      .andExpect(jsonPath("$[0].defaultPrice").value(25.00))
  }

  @Test
  fun `should NOT negate PRODUCT prices for SALE invoice type`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Transport Hours",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = "8100",
        purchaseAccountNumber = null,
        defaultPrice = BigDecimal("50.00"),
        status = "ACTIVE"
      )
    )

    // When & Then - SALE invoice type should NOT negate any prices
    securedMockMvc.get("/catalog/items?invoiceType=SALE")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].itemType").value("PRODUCT"))
      .andExpect(jsonPath("$[0].defaultPrice").value(50.00))
  }

  @Test
  fun `should NOT negate prices when no invoice type provided`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

    catalogItemJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Transport Hours",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = "8100",
        purchaseAccountNumber = null,
        defaultPrice = BigDecimal("50.00"),
        status = "ACTIVE"
      )
    )

    // When & Then - No invoice type should NOT negate any prices
    securedMockMvc.get("/catalog/items")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].defaultPrice").value(50.00))
  }
}
