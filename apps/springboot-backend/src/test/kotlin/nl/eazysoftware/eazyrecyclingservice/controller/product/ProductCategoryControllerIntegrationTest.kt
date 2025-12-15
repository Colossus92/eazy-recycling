package nl.eazysoftware.eazyrecyclingservice.controller.product

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.ProductCategoryRequest
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.product.ProductCategoryJpaRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductCategoryControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var productCategoryJpaRepository: ProductCategoryJpaRepository

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    productCategoryJpaRepository.deleteAll()
  }

  @Test
  fun `should successfully create a product category`() {
    // Given
    val request = ProductCategoryRequest(
      code = "CAT001",
      name = "Test Category",
      description = "Test category description"
    )

    // When & Then
    securedMockMvc.post(
      "/product-categories",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").isNumber)
      .andExpect(jsonPath("$.code").value("CAT001"))
      .andExpect(jsonPath("$.name").value("Test Category"))
      .andExpect(jsonPath("$.description").value("Test category description"))
      .andExpect(jsonPath("$.createdAt").exists())

    // Verify category was saved in the database
    val savedCategories = productCategoryJpaRepository.findAll()
    assertThat(savedCategories).hasSize(1)
    assertThat(savedCategories[0].code).isEqualTo("CAT001")
  }

  @Test
  fun `should get all product categories`() {
    // Given
    productCategoryJpaRepository.save(
      ProductCategoryDto(
        code = "CAT001",
        name = "Category 1",
        description = "Description 1"
      )
    )
    productCategoryJpaRepository.save(
      ProductCategoryDto(
        code = "CAT002",
        name = "Category 2",
        description = "Description 2"
      )
    )

    // When & Then
    securedMockMvc.get("/product-categories")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[0].code").exists())
      .andExpect(jsonPath("$[1].code").exists())
  }

  @Test
  fun `should get product category by id`() {
    // Given
    val category = productCategoryJpaRepository.save(
      ProductCategoryDto(
        code = "CAT001",
        name = "Test Category",
        description = "Test description"
      )
    )

    // When & Then
    securedMockMvc.get("/product-categories/${category.id}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(category.id))
      .andExpect(jsonPath("$.code").value("CAT001"))
      .andExpect(jsonPath("$.name").value("Test Category"))
      .andExpect(jsonPath("$.description").value("Test description"))
  }

  @Test
  fun `should return not found when getting non-existent category`() {
    // When & Then
    securedMockMvc.get("/product-categories/99999")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should update product category`() {
    // Given
    val category = productCategoryJpaRepository.save(
      ProductCategoryDto(
        code = "CAT001",
        name = "Original Category",
        description = "Original description"
      )
    )

    val updateRequest = ProductCategoryRequest(
      code = "CAT001_UPDATED",
      name = "Updated Category",
      description = "Updated description"
    )

    // When & Then
    securedMockMvc.put(
      "/product-categories/${category.id}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.code").value("CAT001_UPDATED"))
      .andExpect(jsonPath("$.name").value("Updated Category"))
      .andExpect(jsonPath("$.description").value("Updated description"))

    // Verify category was updated in the database
    val updatedCategory = productCategoryJpaRepository.findByIdOrNull(category.id!!)
    assertThat(updatedCategory).isNotNull
    assertThat(updatedCategory?.code).isEqualTo("CAT001_UPDATED")
  }

  @Test
  fun `should return not found when updating non-existent category`() {
    // Given
    val request = ProductCategoryRequest(
      code = "NON_EXISTENT",
      name = "Non Existent",
      description = null
    )

    // When & Then
    securedMockMvc.put(
      "/product-categories/99999",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should delete product category`() {
    // Given
    val category = productCategoryJpaRepository.save(
      ProductCategoryDto(
        code = "DELETE_ME",
        name = "Delete Me",
        description = "To be deleted"
      )
    )

    // When & Then
    securedMockMvc.delete("/product-categories/${category.id}")
      .andExpect(status().isNoContent)

    // Verify category was deleted
    assertThat(productCategoryJpaRepository.findByIdOrNull(category.id!!)).isNull()
  }

  @Test
  fun `should return not found when deleting non-existent category`() {
    // When & Then
    securedMockMvc.delete("/product-categories/99999")
      .andExpect(status().isNotFound)
  }
}
