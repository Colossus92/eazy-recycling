package nl.eazysoftware.eazyrecyclingservice.controller.catalog

import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductDto
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductJpaRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var materialJpaRepository: MaterialJpaRepository

  @Autowired
  private lateinit var catalogItemCategoryJpaRepository: CatalogItemCategoryJpaRepository

  @Autowired
  private lateinit var productJpaRepository: ProductJpaRepository

  @Autowired
  private lateinit var productCategoryJpaRepository: ProductCategoryJpaRepository

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  private var testMaterialCategoryId: Long? = null
  private var testProductCategoryId: Long? = null
  private val testVatCode = "VAT21"

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    materialJpaRepository.deleteAll()
    productJpaRepository.deleteAll()
    productCategoryJpaRepository.deleteAll()
    catalogItemCategoryJpaRepository.deleteAll()

    if (!vatRateJpaRepository.existsById(testVatCode)) {
      vatRateJpaRepository.save(
        VatRateDto(
          vatCode = testVatCode,
          percentage = BigDecimal("21.00"),
          validFrom = Instant.now(),
          validTo = null,
          countryCode = "NL",
          description = "21% BTW"
        )
      )
    }

    val materialCategory = catalogItemCategoryJpaRepository.save(
      CatalogItemCategoryDto(
        type = "MATERIAL",
        code = "TEST_GROUP",
        name = "Test Material Group",
        description = "Test Description"
      )
    )
    testMaterialCategoryId = materialCategory.id

    val productCategory = productCategoryJpaRepository.save(
      ProductCategoryDto(
        code = "TEST_CAT",
        name = "Test Product Category",
        description = "Test Category Description"
      )
    )
    testProductCategoryId = productCategory.id
  }

  @Test
  fun `should search catalog and return both materials and products`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    materialJpaRepository.save(
      CatalogItemDto(
        type = "MATERIAL",
        code = "MAT001",
        name = "Steel Pipes",
        category = materialCategory,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = "8000",
        purchaseAccountNumber = null,
        defaultPrice = null,
        status = "ACTIVE"
      )
    )

    productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Steel Service",
        category = productCategory,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = "8100",
        status = "ACTIVE",
        defaultPrice = BigDecimal("50.00"),
        description = "Steel processing service"
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/items?query=Steel")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(2))
  }

  @Test
  fun `should search catalog with limit`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    repeat(5) { i ->
      materialJpaRepository.save(
        CatalogItemDto(
          type = "MATERIAL",
          code = "MAT00$i",
          name = "Material $i",
          category = materialCategory,
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
    securedMockMvc.get("/catalog/items?query=Material&limit=3")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(3))
  }

  @Test
  fun `should get material by id from catalog`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val material = materialJpaRepository.save(
      CatalogItemDto(
        type = "MATERIAL",
        code = "MAT001",
        name = "Steel Pipes",
        category = materialCategory,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = "8000",
        purchaseAccountNumber = null,
        defaultPrice = null,
        status = "ACTIVE"
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/materials/${material.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(material.id))
      .andExpect(jsonPath("$.code").value("MAT001"))
      .andExpect(jsonPath("$.name").value("Steel Pipes"))
      .andExpect(jsonPath("$.itemType").value("MATERIAL"))
      .andExpect(jsonPath("$.glAccountCode").value("8000"))
      .andExpect(jsonPath("$.categoryName").value("Test Material Group"))
  }

  @Test
  fun `should get product by id from catalog`() {
    // Given
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    val product = productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Steel Service",
        category = productCategory,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = "8100",
        status = "ACTIVE",
        defaultPrice = BigDecimal("50.00"),
        description = "Steel processing service"
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/products/${product.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(product.id))
      .andExpect(jsonPath("$.code").value("PROD001"))
      .andExpect(jsonPath("$.name").value("Steel Service"))
      .andExpect(jsonPath("$.itemType").value("PRODUCT"))
      .andExpect(jsonPath("$.glAccountCode").value("8100"))
      .andExpect(jsonPath("$.productCategoryId").value(testProductCategoryId))
  }

  @Test
  fun `should return 404 when material not found`() {
    // When & Then
    securedMockMvc.get("/catalog/materials/99999")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should return 404 when product not found`() {
    // When & Then
    securedMockMvc.get("/catalog/products/99999")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should return empty list when no matches found`() {
    // When & Then
    securedMockMvc.get("/catalog/items?query=NonExistent")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(0))
  }

  @Test
  fun `should filter by type MATERIAL`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    materialJpaRepository.save(
      CatalogItemDto(
        type = "MATERIAL",
        code = "MAT001",
        name = "Test Item",
        category = materialCategory,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        defaultPrice = null,
        status = "ACTIVE"
      )
    )

    productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Test Item",
        category = productCategory,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = null,
        status = "ACTIVE",
        defaultPrice = null,
        description = null
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/items?query=Test&itemTypes=MATERIAL")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].itemType").value("MATERIAL"))
  }

  @Test
  fun `should filter by type PRODUCT`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    materialJpaRepository.save(
      CatalogItemDto(
        type = "MATERIAL",
        code = "MAT001",
        name = "Test Item",
        category = materialCategory,
        consignorParty = null,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        defaultPrice = null,
        status = "ACTIVE"
      )
    )

    productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Test Item",
        category = productCategory,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = null,
        status = "ACTIVE",
        defaultPrice = null,
        description = null
      )
    )

    // When & Then
    securedMockMvc.get("/catalog/items?query=Test&itemTypes=PRODUCT")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(1))
      .andExpect(jsonPath("$[0].itemType").value("PRODUCT"))
  }
}
