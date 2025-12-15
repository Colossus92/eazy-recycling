package nl.eazysoftware.eazyrecyclingservice.controller.catalog

import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialGroupDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialGroupJpaRepository
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
  private lateinit var materialGroupJpaRepository: MaterialGroupJpaRepository

  @Autowired
  private lateinit var productJpaRepository: ProductJpaRepository

  @Autowired
  private lateinit var productCategoryJpaRepository: ProductCategoryJpaRepository

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  private var testMaterialGroupId: Long? = null
  private var testProductCategoryId: Long? = null
  private val testVatCode = "VAT21"

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    materialJpaRepository.deleteAll()
    productJpaRepository.deleteAll()
    productCategoryJpaRepository.deleteAll()
    materialGroupJpaRepository.deleteAll()

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

    val materialGroup = materialGroupJpaRepository.save(
      MaterialGroupDto(
        code = "TEST_GROUP",
        name = "Test Material Group",
        description = "Test Description"
      )
    )
    testMaterialGroupId = materialGroup.id

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
    val materialGroup = materialGroupJpaRepository.findById(testMaterialGroupId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    materialJpaRepository.save(
      MaterialDto(
        code = "MAT001",
        name = "Steel Pipes",
        materialGroup = materialGroup,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        glAccountCode = "8000",
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
    val materialGroup = materialGroupJpaRepository.findById(testMaterialGroupId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    repeat(5) { i ->
      materialJpaRepository.save(
        MaterialDto(
          code = "MAT00$i",
          name = "Material $i",
          materialGroup = materialGroup,
          unitOfMeasure = "KG",
          vatRate = vatRate,
          glAccountCode = null,
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
    val materialGroup = materialGroupJpaRepository.findById(testMaterialGroupId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val material = materialJpaRepository.save(
      MaterialDto(
        code = "MAT001",
        name = "Steel Pipes",
        materialGroup = materialGroup,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        glAccountCode = "8000",
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
    val materialGroup = materialGroupJpaRepository.findById(testMaterialGroupId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    materialJpaRepository.save(
      MaterialDto(
        code = "MAT001",
        name = "Test Item",
        materialGroup = materialGroup,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        glAccountCode = null,
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
    val materialGroup = materialGroupJpaRepository.findById(testMaterialGroupId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()
    val productCategory = productCategoryJpaRepository.findById(testProductCategoryId!!).get()

    materialJpaRepository.save(
      MaterialDto(
        code = "MAT001",
        name = "Test Item",
        materialGroup = materialGroup,
        unitOfMeasure = "KG",
        vatRate = vatRate,
        glAccountCode = null,
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
