package nl.eazysoftware.eazyrecyclingservice.controller.material

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
class MaterialPriceControllerIntegrationTest : BaseIntegrationTest() {

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
    fun `should return all materials with prices`() {
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
                defaultPrice = BigDecimal("25.00"),
                status = "ACTIVE"
            )
        )

        catalogItemJpaRepository.save(
            CatalogItemDto(
                id = UUID.randomUUID(),
                type = CatalogItemType.MATERIAL,
                code = "MAT002",
                name = "Copper Wire",
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

        // When & Then
        securedMockMvc.get("/material-prices")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].defaultPrice").value(25.00))
            .andExpect(jsonPath("$[1].defaultPrice").isEmpty)
    }

    @Test
    fun `should return material price by id`() {
        // Given
        val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
        val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

        val material = catalogItemJpaRepository.save(
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
                defaultPrice = BigDecimal("25.00"),
                status = "ACTIVE"
            )
        )

        // When & Then
        securedMockMvc.get("/material-prices/${material.id}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(material.id.toString()))
            .andExpect(jsonPath("$.code").value("MAT001"))
            .andExpect(jsonPath("$.name").value("Steel Pipes"))
            .andExpect(jsonPath("$.defaultPrice").value(25.00))
    }

    @Test
    fun `should return 404 when material not found`() {
        // When & Then
        securedMockMvc.get("/material-prices/${UUID.randomUUID()}")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should create price for material without price`() {
        // Given
        val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
        val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

        val material = catalogItemJpaRepository.save(
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

        val requestBody = """{"price": 30.50}"""

        // When & Then
        securedMockMvc.post("/material-prices/${material.id}", requestBody)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(material.id.toString()))
            .andExpect(jsonPath("$.defaultPrice").value(30.50))
    }

    @Test
    fun `should return 409 when creating price for material that already has price`() {
        // Given
        val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
        val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

        val material = catalogItemJpaRepository.save(
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
                defaultPrice = BigDecimal("25.00"),
                status = "ACTIVE"
            )
        )

        val requestBody = """{"price": 30.50}"""

        // When & Then
        securedMockMvc.post("/material-prices/${material.id}", requestBody)
            .andExpect(status().isConflict)
    }

    @Test
    fun `should update price for material`() {
        // Given
        val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
        val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

        val material = catalogItemJpaRepository.save(
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
                defaultPrice = BigDecimal("25.00"),
                status = "ACTIVE"
            )
        )

        val requestBody = """{"price": 35.75}"""

        // When & Then
        securedMockMvc.put("/material-prices/${material.id}", requestBody)
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(material.id.toString()))
            .andExpect(jsonPath("$.defaultPrice").value(35.75))
    }

    @Test
    fun `should delete price for material`() {
        // Given
        val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
        val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

        val material = catalogItemJpaRepository.save(
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
                defaultPrice = BigDecimal("25.00"),
                status = "ACTIVE"
            )
        )

        // When & Then
        securedMockMvc.delete("/material-prices/${material.id}")
            .andExpect(status().isNoContent)

        // Verify price is deleted
        securedMockMvc.get("/material-prices/${material.id}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.defaultPrice").isEmpty)
    }

    @Test
    fun `should return 400 when price is negative`() {
        // Given
        val category = catalogItemCategoryJpaRepository.findById(testCategoryId!!).get()
        val vatRate = vatRateJpaRepository.findById(testVatRateId).get()

        val material = catalogItemJpaRepository.save(
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

        val requestBody = """{"price": -10.00}"""

        // When & Then
        securedMockMvc.post("/material-prices/${material.id}", requestBody)
            .andExpect(status().isBadRequest)
    }
}
