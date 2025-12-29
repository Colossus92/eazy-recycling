package nl.eazysoftware.eazyrecyclingservice.controller.product

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.ProductRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var productJpaRepository: ProductJpaRepository

  @Autowired
  private lateinit var catalogItemCategoryJpaRepository: CatalogItemCategoryJpaRepository

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  private var testCategoryId: UUID? = null
  private val testVatCode = "VAT21"

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    // Delete only PRODUCT type items
    productJpaRepository.findAllProducts().forEach {
      productJpaRepository.deleteById(it.getId())
    }

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

    val category = catalogItemCategoryJpaRepository.save(
      CatalogItemCategoryDto(
        id = UUID.randomUUID(),
        type = "PRODUCT",
        code = "TEST_CAT",
        name = "Test Category",
        description = "Test Description"
      )
    )
    testCategoryId = category.id
  }

  @Test
  fun `should successfully create a product`() {
    // Given
    val request = ProductRequest(
      code = "PROD001",
      name = "Test Product",
      categoryId = testCategoryId,
      unitOfMeasure = "HOUR",
      vatCode = testVatCode,
      salesAccountNumber = "8100",
      purchaseAccountNumber = null,
      status = "ACTIVE",
      defaultPrice = BigDecimal("50.00")
    )

    // When & Then
    securedMockMvc.post(
      "/products",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").isString)
      .andExpect(jsonPath("$.code").value("PROD001"))
      .andExpect(jsonPath("$.name").value("Test Product"))
      .andExpect(jsonPath("$.categoryId").value(testCategoryId.toString()))
      .andExpect(jsonPath("$.unitOfMeasure").value("HOUR"))
      .andExpect(jsonPath("$.vatCode").value(testVatCode))
      .andExpect(jsonPath("$.salesAccountNumber").value("8100"))
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.defaultPrice").value(50.00))
      .andExpect(jsonPath("$.createdAt").exists())

    // Verify product was saved in the database
    val savedProducts = productJpaRepository.findAllProducts()
    assertThat(savedProducts).hasSize(1)
    assertThat(savedProducts[0].getCode()).isEqualTo("PROD001")
  }

  @Test
  fun `should get all products`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    productJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Product 1",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        status = "ACTIVE",
        defaultPrice = null
      )
    )
    productJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD002",
        name = "Product 2",
        category = category,
        consignorParty = null,
        unitOfMeasure = "PIECE",
        vatRate = vatRate,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        status = "ACTIVE",
        defaultPrice = null
      )
    )

    // When & Then
    securedMockMvc.get("/products")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].code").exists())
      .andExpect(jsonPath("$[1].code").exists())
  }

  @Test
  fun `should get product by id`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val product = productJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Test Product",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = "8100",
        purchaseAccountNumber = null,
        status = "ACTIVE",
        defaultPrice = BigDecimal("75.00")
      )
    )

    // When & Then
    securedMockMvc.get("/products/${product.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(product.id.toString()))
      .andExpect(jsonPath("$.code").value("PROD001"))
      .andExpect(jsonPath("$.name").value("Test Product"))
      .andExpect(jsonPath("$.salesAccountNumber").value("8100"))
      .andExpect(jsonPath("$.defaultPrice").value(75.00))
  }

  @Test
  fun `should return not found when getting non-existent product`() {
    // When & Then
    securedMockMvc.get("/products/${UUID.randomUUID()}")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should update product`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val product = productJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "PROD001",
        name = "Original Product",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        status = "ACTIVE",
        defaultPrice = null
      )
    )

    val updateRequest = ProductRequest(
      code = "PROD001_UPDATED",
      name = "Updated Product",
      categoryId = testCategoryId,
      unitOfMeasure = "PIECE",
      vatCode = testVatCode,
      salesAccountNumber = "8200",
      purchaseAccountNumber = null,
      status = "INACTIVE",
      defaultPrice = BigDecimal("100.00")
    )

    // When & Then
    securedMockMvc.put(
      "/products/${product.id}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.code").value("PROD001_UPDATED"))
      .andExpect(jsonPath("$.name").value("Updated Product"))
      .andExpect(jsonPath("$.salesAccountNumber").value("8200"))
      .andExpect(jsonPath("$.status").value("INACTIVE"))

    // Verify product was updated in the database
    val updatedProduct = productJpaRepository.findProductById(product.id!!)
    assertThat(updatedProduct).isNotNull
    assertThat(updatedProduct?.getCode()).isEqualTo("PROD001_UPDATED")
  }

  @Test
  fun `should return not found when updating non-existent product`() {
    // Given
    val request = ProductRequest(
      code = "NON_EXISTENT",
      name = "Non Existent",
      categoryId = testCategoryId,
      unitOfMeasure = "HOUR",
      vatCode = testVatCode,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE",
      defaultPrice = null
    )

    // When & Then
    securedMockMvc.put(
      "/products/${UUID.randomUUID()}",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should delete product`() {
    // Given
    val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val product = productJpaRepository.save(
      CatalogItemDto(
        id = UUID.randomUUID(),
        type = CatalogItemType.PRODUCT,
        code = "DELETE_ME",
        name = "Delete Me",
        category = category,
        consignorParty = null,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        status = "ACTIVE",
        defaultPrice = null
      )
    )

    // When & Then
    securedMockMvc.delete("/products/${product.id}")
      .andExpect(status().isNoContent)

    // Verify product was deleted
    assertThat(productJpaRepository.findProductById(product.id!!)).isNull()
  }

  @Test
  fun `should return not found when deleting non-existent product`() {
    // When & Then
    securedMockMvc.delete("/products/${UUID.randomUUID()}")
      .andExpect(status().isNotFound)
  }
}
