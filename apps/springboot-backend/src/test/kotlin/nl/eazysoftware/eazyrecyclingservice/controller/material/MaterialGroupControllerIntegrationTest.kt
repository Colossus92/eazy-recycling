package nl.eazysoftware.eazyrecyclingservice.controller.material

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.MaterialGroupRequest
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialGroupMapper.Companion.MATERIAL_TYPE
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialGroupControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var catalogItemCategoryJpaRepository: CatalogItemCategoryJpaRepository

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
  }

  @AfterEach
  fun cleanup() {
    catalogItemCategoryJpaRepository.findByType(MATERIAL_TYPE).forEach {
      catalogItemCategoryJpaRepository.delete(it)
    }
  }

  @Test
  fun `should successfully create a material group`() {
    // Given
    val request = MaterialGroupRequest(
      code = "PLASTICS",
      name = "Plastics",
      description = "All plastic materials"
    )

    // When & Then
    securedMockMvc.post(
      "/material-groups",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").isString)
      .andExpect(jsonPath("$.code").value("PLASTICS"))
      .andExpect(jsonPath("$.name").value("Plastics"))
      .andExpect(jsonPath("$.description").value("All plastic materials"))
      .andExpect(jsonPath("$.createdAt").exists())

    // Verify material group was saved in the database
    val savedGroups = catalogItemCategoryJpaRepository.findByType(MATERIAL_TYPE)
    assertThat(savedGroups).hasSize(1)
    assertThat(savedGroups[0].code).isEqualTo("PLASTICS")
  }

  @Test
  fun `should get all material groups`() {
    // Given
    val group1 = CatalogItemCategoryDto(
      id = UUID.randomUUID(),
      type = MATERIAL_TYPE,
      code = "METALS",
      name = "Metals",
      description = "All metal materials",
    )
    val group2 = CatalogItemCategoryDto(
      id = UUID.randomUUID(),
      type = MATERIAL_TYPE,
      code = "WOOD",
      name = "Wood",
      description = "All wood materials",
    )
    catalogItemCategoryJpaRepository.saveAll(listOf(group1, group2))

    // When & Then
    securedMockMvc.get("/material-groups")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[?(@.code == 'METALS')]").exists())
      .andExpect(jsonPath("$[?(@.code == 'WOOD')]").exists())
  }

  @Test
  fun `should get material group by id`() {
    // Given
    val group = CatalogItemCategoryDto(
      id = UUID.randomUUID(),
      type = MATERIAL_TYPE,
      code = "GLASS",
      name = "Glass",
      description = "All glass materials",
    )
    val saved = catalogItemCategoryJpaRepository.save(group)

    // When & Then
    securedMockMvc.get("/material-groups/${saved.id}")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").value(saved.id.toString()))
      .andExpect(jsonPath("$.code").value("GLASS"))
      .andExpect(jsonPath("$.name").value("Glass"))
      .andExpect(jsonPath("$.description").value("All glass materials"))
  }

  @Test
  fun `should return not found when getting material group with non-existent id`() {
    // When & Then
    securedMockMvc.get("/material-groups/${UUID.randomUUID()}")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should update material group`() {
    // Given
    val originalGroup = CatalogItemCategoryDto(
      id = UUID.randomUUID(),
      type = MATERIAL_TYPE,
      code = "PAPER",
      name = "Paper",
      description = "Paper materials",
    )
    val saved = catalogItemCategoryJpaRepository.save(originalGroup)

    val updatedRequest = MaterialGroupRequest(
      code = "PAPER_UPDATED",
      name = "Paper and Cardboard",
      description = "All paper and cardboard materials"
    )

    // When & Then
    securedMockMvc.put(
      "/material-groups/${saved.id}",
      objectMapper.writeValueAsString(updatedRequest)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(saved.id.toString()))
      .andExpect(jsonPath("$.code").value("PAPER_UPDATED"))
      .andExpect(jsonPath("$.name").value("Paper and Cardboard"))
      .andExpect(jsonPath("$.description").value("All paper and cardboard materials"))
      .andExpect(jsonPath("$.updatedAt").exists())

    // Verify material group was updated in the database
    val savedId = saved.id
    val updatedGroup = catalogItemCategoryJpaRepository.findByIdAndType(savedId, MATERIAL_TYPE)
    assertThat(updatedGroup).isNotNull
    assertThat(updatedGroup?.code).isEqualTo("PAPER_UPDATED")
    assertThat(updatedGroup?.name).isEqualTo("Paper and Cardboard")
    assertThat(updatedGroup?.updatedAt).isNotNull()
  }

  @Test
  fun `should return not found when updating non-existent material group`() {
    // Given
    val request = MaterialGroupRequest(
      code = "NON_EXISTENT",
      name = "Non Existent",
      description = "This does not exist"
    )

    // When & Then
    securedMockMvc.put(
      "/material-groups/${UUID.randomUUID()}",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should delete material group`() {
    // Given
    val group = CatalogItemCategoryDto(
      id = UUID.randomUUID(),
      type = MATERIAL_TYPE,
      code = "DELETE_ME",
      name = "Delete Me",
      description = "This will be deleted",
    )
    val saved = catalogItemCategoryJpaRepository.save(group)

    // When & Then
    securedMockMvc.delete("/material-groups/${saved.id}")
      .andExpect(status().isNoContent)

    // Verify material group was deleted
    assertThat(catalogItemCategoryJpaRepository.findByIdAndType(saved.id, MATERIAL_TYPE)).isNull()
  }

  @Test
  fun `should return not found when deleting non-existent material group`() {
    // When & Then
    securedMockMvc.delete("/material-groups/${UUID.randomUUID()}")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should validate required fields when creating material group`() {
    // Given - missing required fields
    val invalidRequest = """
      {
        "code": "",
        "name": "",
        "description": ""
      }
    """.trimIndent()

    // When & Then
    securedMockMvc.post(
      "/material-groups",
      invalidRequest
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should create multiple material groups`() {
    // Given
    val request1 = MaterialGroupRequest(
      code = "TEXTILES",
      name = "Textiles",
      description = "Textile materials"
    )
    val request2 = MaterialGroupRequest(
      code = "ELECTRONICS",
      name = "Electronics",
      description = "Electronic materials"
    )

    // When
    securedMockMvc.post(
      "/material-groups",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/material-groups",
      objectMapper.writeValueAsString(request2)
    ).andExpect(status().isCreated)

    // Then
    val allGroups = catalogItemCategoryJpaRepository.findByType(MATERIAL_TYPE)
    assertThat(allGroups).hasSize(2)
    assertThat(allGroups.map { it.code }).containsExactlyInAnyOrder("TEXTILES", "ELECTRONICS")
  }

}
