package nl.eazysoftware.eazyrecyclingservice.controller.material

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.MaterialRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialJpaRepository
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
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialControllerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var mockMvc: MockMvc

  private lateinit var securedMockMvc: SecuredMockMvc

  @Autowired
  private lateinit var objectMapper: ObjectMapper

  @Autowired
  private lateinit var materialJpaRepository: MaterialJpaRepository

  @Autowired
  private lateinit var catalogItemCategoryJpaRepository: CatalogItemCategoryJpaRepository

  @Autowired
  private lateinit var vatRateJpaRepository: VatRateJpaRepository

  private var testMaterialCategoryId: UUID? = null
  private var testVatCode1: String = "TEST_VAT_1"
  private var testVatCode2: String = "TEST_VAT_2"

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)

    // Create test VAT rates for foreign key constraint
    val vatRate1 = VatRateDto(
      vatCode = testVatCode1,
      percentage = BigDecimal("21.00"),
      validFrom = Instant.now(),
      validTo = null,
      countryCode = "NL",
      description = "Test VAT rate 1"
    )
    val vatRate2 = VatRateDto(
      vatCode = testVatCode2,
      percentage = BigDecimal("9.00"),
      validFrom = Instant.now(),
      validTo = null,
      countryCode = "NL",
      description = "Test VAT rate 2"
    )
    vatRateJpaRepository.saveAll(listOf(vatRate1, vatRate2))

    // Create a test material category for foreign key constraint
    val materialCategory = CatalogItemCategoryDto(
      id = UUID.randomUUID(),
      type = "MATERIAL",
      code = "TEST_GROUP",
      name = "Test Material Group",
      description = "For testing materials",
    )
    val savedCategory = catalogItemCategoryJpaRepository.save(materialCategory)
    testMaterialCategoryId = savedCategory.id
  }

  @AfterEach
  fun cleanup() {
    materialJpaRepository.deleteAll()
    catalogItemCategoryJpaRepository.deleteAll()
    vatRateJpaRepository.deleteAll()
  }

  @Test
  fun `should successfully create a material`() {
    // Given
    val request = MaterialRequest(
      code = "MAT001",
      name = "Steel Pipes",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "KG",
      vatCode = testVatCode1,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE"
    )

    // When & Then
    securedMockMvc.post(
      "/materials",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isCreated)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").isString)
      .andExpect(jsonPath("$.code").value("MAT001"))
      .andExpect(jsonPath("$.name").value("Steel Pipes"))
      .andExpect(jsonPath("$.materialGroupCode").value("TEST_GROUP"))
      .andExpect(jsonPath("$.materialGroupName").value("Test Material Group"))
      .andExpect(jsonPath("$.unitOfMeasure").value("KG"))
      .andExpect(jsonPath("$.vatCode").value(testVatCode1))
      .andExpect(jsonPath("$.status").value("ACTIVE"))
      .andExpect(jsonPath("$.createdAt").exists())

    // Verify material was saved in the database
    val savedMaterials = materialJpaRepository.findAll()
    assertThat(savedMaterials).hasSize(1)
    assertThat(savedMaterials[0].code).isEqualTo("MAT001")
  }

  @Test
  fun `should get all materials`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode1).get()

    val material1 = CatalogItemDto(
      id = UUID.randomUUID(),
      type = CatalogItemType.MATERIAL,
      code = "MAT002",
      name = "Aluminum",
      category = materialCategory,
      consignorParty = null,
      unitOfMeasure = "KG",
      vatRate = vatRate,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      defaultPrice = null,
      status = "ACTIVE",
    )
    val material2 = CatalogItemDto(
      id = UUID.randomUUID(),
      type = CatalogItemType.MATERIAL,
      code = "MAT003",
      name = "Copper",
      category = materialCategory,
      consignorParty = null,
      unitOfMeasure = "KG",
      vatRate = vatRate,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      defaultPrice = null,
      status = "ACTIVE",
    )
    materialJpaRepository.saveAll(listOf(material1, material2))

    // When & Then
    securedMockMvc.get("/materials")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$").isArray)
      .andExpect(jsonPath("$.length()").value(2))
      .andExpect(jsonPath("$[?(@.code == 'MAT002')]").exists())
      .andExpect(jsonPath("$[?(@.code == 'MAT003')]").exists())
  }

  @Test
  fun `should get material by id`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode1).get()

    val material = CatalogItemDto(
      id = UUID.randomUUID(),
      type = CatalogItemType.MATERIAL,
      code = "MAT004",
      name = "Bronze",
      category = materialCategory,
      consignorParty = null,
      unitOfMeasure = "KG",
      vatRate = vatRate,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      defaultPrice = null,
      status = "ACTIVE",
    )
    val saved = materialJpaRepository.save(material)

    // When & Then
    securedMockMvc.get("/materials/${saved.id}")
      .andExpect(status().isOk)
      .andExpect(content().contentType(MediaType.APPLICATION_JSON))
      .andExpect(jsonPath("$.id").value(saved.id.toString()))
      .andExpect(jsonPath("$.code").value("MAT004"))
      .andExpect(jsonPath("$.name").value("Bronze"))
  }

  @Test
  fun `should return not found when getting material with non-existent id`() {
    // When & Then
    securedMockMvc.get("/materials/${UUID.randomUUID()}")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should update material`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode1).get()

    val originalMaterial = CatalogItemDto(
      id = UUID.randomUUID(),
      type = CatalogItemType.MATERIAL,
      code = "MAT005",
      name = "Iron",
      category = materialCategory,
      consignorParty = null,
      unitOfMeasure = "KG",
      vatRate = vatRate,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      defaultPrice = null,
      status = "ACTIVE",
    )
    val saved = materialJpaRepository.save(originalMaterial)

    val updatedRequest = MaterialRequest(
      code = "MAT005_UPDATED",
      name = "Cast Iron",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "TON",
      vatCode = testVatCode2,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "INACTIVE"
    )

    // When & Then
    val savedId = saved.id
    securedMockMvc.put(
      "/materials/$savedId",
      objectMapper.writeValueAsString(updatedRequest)
    )
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(savedId.toString()))
      .andExpect(jsonPath("$.code").value("MAT005_UPDATED"))
      .andExpect(jsonPath("$.name").value("Cast Iron"))
      .andExpect(jsonPath("$.unitOfMeasure").value("TON"))
      .andExpect(jsonPath("$.vatCode").value(testVatCode2))
      .andExpect(jsonPath("$.status").value("INACTIVE"))
      .andExpect(jsonPath("$.updatedAt").exists())

    // Verify material was updated in the database
    val updatedMaterial = materialJpaRepository.findByIdOrNull(savedId)
    assertThat(updatedMaterial).isNotNull
    assertThat(updatedMaterial?.code).isEqualTo("MAT005_UPDATED")
    assertThat(updatedMaterial?.name).isEqualTo("Cast Iron")
    assertThat(updatedMaterial?.updatedAt).isNotNull()
  }

  @Test
  fun `should return not found when updating non-existent material`() {
    // Given
    val request = MaterialRequest(
      code = "NON_EXISTENT",
      name = "Non Existent",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "KG",
      vatCode = testVatCode1,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE"
    )

    // When & Then
    securedMockMvc.put(
      "/materials/${UUID.randomUUID()}",
      objectMapper.writeValueAsString(request)
    )
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should delete material`() {
    // Given
    val materialCategory = catalogItemCategoryJpaRepository.findById(testMaterialCategoryId!!).get()
    val vatRate = vatRateJpaRepository.findById(testVatCode1).get()

    val material = CatalogItemDto(
      id = UUID.randomUUID(),
      type = CatalogItemType.MATERIAL,
      code = "DELETE_ME",
      name = "Delete Me",
      category = materialCategory,
      consignorParty = null,
      unitOfMeasure = "KG",
      vatRate = vatRate,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      defaultPrice = null,
      status = "ACTIVE",
    )
    val saved = materialJpaRepository.save(material)

    // When & Then
    securedMockMvc.delete("/materials/${saved.id}")
      .andExpect(status().isNoContent)

    // Verify material was deleted
    assertThat(materialJpaRepository.findByIdOrNull(saved.id)).isNull()
  }

  @Test
  fun `should return not found when deleting non-existent material`() {
    // When & Then
    securedMockMvc.delete("/materials/${UUID.randomUUID()}")
      .andExpect(status().isNotFound)
  }

  @Test
  fun `should validate required fields when creating material`() {
    // Given - missing required fields
    val invalidRequest = """
      {
        "code": "",
        "name": "",
        "materialGroupId": null,
        "unitOfMeasure": "",
        "vatCode": null,
        "status": ""
      }
    """.trimIndent()

    // When & Then
    securedMockMvc.post(
      "/materials",
      invalidRequest
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should validate vat code is not blank`() {
    // Given
    val invalidRequest = MaterialRequest(
      code = "MAT_INVALID",
      name = "Invalid Material",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "KG",
      vatCode = "",
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE"
    )

    // When & Then
    securedMockMvc.post(
      "/materials",
      objectMapper.writeValueAsString(invalidRequest)
    )
      .andExpect(status().isBadRequest)
  }

  @Test
  fun `should create multiple materials with same material group`() {
    // Given
    val request1 = MaterialRequest(
      code = "MAT_MULTI_1",
      name = "Material 1",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "KG",
      vatCode = testVatCode1,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE"
    )
    val request2 = MaterialRequest(
      code = "MAT_MULTI_2",
      name = "Material 2",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "L",
      vatCode = testVatCode2,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE"
    )

    // When
    securedMockMvc.post(
      "/materials",
      objectMapper.writeValueAsString(request1)
    ).andExpect(status().isCreated)

    securedMockMvc.post(
      "/materials",
      objectMapper.writeValueAsString(request2)
    ).andExpect(status().isCreated)

    // Then
    val allMaterials = materialJpaRepository.findAll()
    assertThat(allMaterials).hasSize(2)
    assertThat(allMaterials.map { it.code }).containsExactlyInAnyOrder("MAT_MULTI_1", "MAT_MULTI_2")
    assertThat(allMaterials.map { it.category?.id }).allMatch { it == testMaterialCategoryId }
  }

  @Test
  fun `should handle different units of measure`() {
    // Given
    val materials = listOf("KG", "L", "M", "TON", "PIECE").mapIndexed { index, unit ->
      MaterialRequest(
        code = "MAT_UNIT_$index",
        name = "Material with unit $unit",
        materialGroupId = testMaterialCategoryId!!,
        unitOfMeasure = unit,
        vatCode = testVatCode1,
        salesAccountNumber = null,
        purchaseAccountNumber = null,
        status = "ACTIVE"
      )
    }

    // When
    materials.forEach { request ->
      securedMockMvc.post(
        "/materials",
        objectMapper.writeValueAsString(request)
      ).andExpect(status().isCreated)
    }

    // Then
    val savedMaterials = materialJpaRepository.findAll()
    assertThat(savedMaterials).hasSize(5)
    assertThat(savedMaterials.map { it.unitOfMeasure })
      .containsExactlyInAnyOrder("KG", "L", "M", "TON", "PIECE")
  }

  @Test
  fun `should persist and return audit fields when creating material`() {
    // Given
    val request = MaterialRequest(
      code = "MAT_AUDIT",
      name = "Audit Test Material",
      materialGroupId = testMaterialCategoryId!!,
      unitOfMeasure = "KG",
      vatCode = testVatCode1,
      salesAccountNumber = null,
      purchaseAccountNumber = null,
      status = "ACTIVE"
    )

    // When - Create material
    securedMockMvc.post(
      "/materials",
      objectMapper.writeValueAsString(request)
    ).andExpect(status().isCreated)
      .andExpect(jsonPath("$.createdAt").isNotEmpty)
      .andExpect(jsonPath("$.createdByName").value("Test User"))
      .andExpect(jsonPath("$.updatedAt").isNotEmpty)
      .andExpect(jsonPath("$.updatedByName").value("Test User"))

    // Then - Verify audit fields are persisted in database
    val savedMaterial = materialJpaRepository.findAll().first { it.code == "MAT_AUDIT" }
    assertThat(savedMaterial.createdAt).isNotNull
    assertThat(savedMaterial.createdBy).isEqualTo("Test User")
    assertThat(savedMaterial.updatedAt).isNotNull
    assertThat(savedMaterial.updatedBy).isEqualTo("Test User")
  }
}
