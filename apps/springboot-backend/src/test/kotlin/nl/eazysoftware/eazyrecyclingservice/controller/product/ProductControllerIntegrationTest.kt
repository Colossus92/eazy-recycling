package nl.eazysoftware.eazyrecyclingservice.controller.product

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.ProductRequest
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductDto
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
class ProductControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var productJpaRepository: ProductJpaRepository

  @Autowired
  private lateinit var productCategoryJpaRepository: ProductCategoryJpaRepository

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  private var testCategoryId: Long? = null
  private val testVatCode = "VAT21"

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    productJpaRepository.deleteAll()
    productCategoryJpaRepository.deleteAll()

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

    val category = productCategoryJpaRepository.save(
      ProductCategoryDto(
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
      glAccountCode = "8100",
      status = "ACTIVE",
      defaultPrice = BigDecimal("50.00"),
      description = "Test product description"
    )

    // When & Then
    securedMockMvc.post(
      "/products",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").isNumber)
      .andExpect(jsonPath("$.code").value("PROD001"))
      .andExpect(jsonPath("$.name").value("Test Product"))
      .andExpect(jsonPath("$.categoryId").value(testCategoryId))
      .andExpect(jsonPath("$.unitOfMeasure").value("HOUR"))
      .andExpect(jsonPath("$.vatCode").value(testVatCode))
      .andExpect(jsonPath("$.glAccountCode").value("8100"))
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.defaultPrice").value(50.00))
      .andExpect(jsonPath("$.description").value("Test product description"))
      .andExpect(jsonPath("$.createdAt").exists())

    // Verify product was saved in the database
    val savedProducts = productJpaRepository.findAll()
    assertThat(savedProducts).hasSize(1)
    assertThat(savedProducts[0].code).isEqualTo("PROD001")
  }

  @Test
  fun `should get all products`() {
    // Given
    val category = productCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Product 1",
        category = category,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = null,
        status = "ACTIVE",
        defaultPrice = null,
        description = null
      )
    )
    productJpaRepository.save(
      ProductDto(
        code = "PROD002",
        name = "Product 2",
        category = category,
        unitOfMeasure = "PIECE",
        vatRate = vatRate,
        glAccountCode = null,
        status = "ACTIVE",
        defaultPrice = null,
        description = null
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
    val category = productCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val product = productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Test Product",
        category = category,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = "8100",
        status = "ACTIVE",
        defaultPrice = BigDecimal("75.00"),
        description = "Test description"
      )
    )

    // When & Then
    securedMockMvc.get("/products/${product.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(product.id))
      .andExpect(jsonPath("$.code").value("PROD001"))
      .andExpect(jsonPath("$.name").value("Test Product"))
      .andExpect(jsonPath("$.glAccountCode").value("8100"))
      .andExpect(jsonPath("$.defaultPrice").value(75.00))
  }

  @Test
  fun `should return not found when getting non-existent product`() {
    // When & Then
    securedMockMvc.get("/products/99999")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should update product`() {
    // Given
    val category = productCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val product = productJpaRepository.save(
      ProductDto(
        code = "PROD001",
        name = "Original Product",
        category = category,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = null,
        status = "ACTIVE",
        defaultPrice = null,
        description = null
      )
    )

    val updateRequest = ProductRequest(
      code = "PROD001_UPDATED",
      name = "Updated Product",
      categoryId = testCategoryId,
      unitOfMeasure = "PIECE",
      vatCode = testVatCode,
      glAccountCode = "8200",
      status = "INACTIVE",
      defaultPrice = BigDecimal("100.00"),
      description = "Updated description"
    )

    // When & Then
    securedMockMvc.put(
      "/products/${product.id}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.code").value("PROD001_UPDATED"))
      .andExpect(jsonPath("$.name").value("Updated Product"))
      .andExpect(jsonPath("$.glAccountCode").value("8200"))
      .andExpect(jsonPath("$.status").value("INACTIVE"))

    // Verify product was updated in the database
    val updatedProduct = productJpaRepository.findByIdOrNull(product.id!!)
    assertThat(updatedProduct).isNotNull
    assertThat(updatedProduct?.code).isEqualTo("PROD001_UPDATED")
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
      glAccountCode = null,
      status = "ACTIVE",
      defaultPrice = null,
      description = null
    )

    // When & Then
    securedMockMvc.put(
      "/products/99999",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should delete product`() {
    // Given
    val category = productCategoryJpaRepository.findById(testCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode).get()

    val product = productJpaRepository.save(
      ProductDto(
        code = "DELETE_ME",
        name = "Delete Me",
        category = category,
        unitOfMeasure = "HOUR",
        vatRate = vatRate,
        glAccountCode = null,
        status = "ACTIVE",
        defaultPrice = null,
        description = null
      )
    )

    // When & Then
    securedMockMvc.delete("/products/${product.id}")
      .andExpect(status().isNoContent)

    // Verify product was deleted
    assertThat(productJpaRepository.findByIdOrNull(product.id!!)).isNull()
  }

  @Test
  fun `should return not found when deleting non-existent product`() {
    // When & Then
    securedMockMvc.delete("/products/99999")
      .andExpect(status().isNotFound)
  }
}
