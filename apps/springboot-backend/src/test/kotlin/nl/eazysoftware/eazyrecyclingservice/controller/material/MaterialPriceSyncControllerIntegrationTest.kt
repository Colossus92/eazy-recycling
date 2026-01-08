package nl.eazysoftware.eazyrecyclingservice.controller.material

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.domain.model.catalog.CatalogItemType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExternalProduct
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.PricingAppSync
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPricingAppSyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPricingAppSyncJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MaterialPriceSyncControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var catalogItemJpaRepository: CatalogItemJpaRepository

    @Autowired
    private lateinit var catalogItemCategoryJpaRepository: CatalogItemCategoryJpaRepository

    @Autowired
    private lateinit var vatRateJpaRepository: VatRateJpaRepository

    @Autowired
    private lateinit var syncJpaRepository: MaterialPricingAppSyncJpaRepository

    @MockitoBean
    private lateinit var pricingAppSync: PricingAppSync

    private var testCategory: CatalogItemCategoryDto? = null
    private var testVatRate: VatRateDto? = null

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)

        // Create test VAT rate
        testVatRate = VatRateDto(
            vatCode = "TEST_VAT_SYNC",
            percentage = BigDecimal("21.00"),
            validFrom = Instant.now(),
            validTo = null,
            countryCode = "NL",
            description = "Test VAT rate for sync"
        )
        vatRateJpaRepository.save(testVatRate!!)

        // Create test category
        testCategory = CatalogItemCategoryDto(
            id = UUID.randomUUID(),
            type = "MATERIAL",
            code = "TEST_SYNC_CAT",
            name = "Test Sync Category"
        )
        testCategory = catalogItemCategoryJpaRepository.save(testCategory!!)
    }

    @AfterEach
    fun cleanup() {
        syncJpaRepository.deleteAll()
        catalogItemJpaRepository.deleteAll()
        catalogItemCategoryJpaRepository.deleteAll()
        vatRateJpaRepository.deleteAll()
    }

    @Test
    fun `should return sync preview with materials to create`() {
        // Given - a material marked for publishing with a price, not yet synced
        val material = createMaterial("SYNC_TEST_1", "Koper Test", publishToPricingApp = true)
        createPrice(material, BigDecimal("8.50"))

        // Mock external API to return empty list (no existing products)
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(emptyList())

        // When & Then
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.toCreate").isArray)
            .andExpect(jsonPath("$.toCreate.length()").value(1))
            .andExpect(jsonPath("$.toCreate[0].materialName").value("Koper Test"))
            .andExpect(jsonPath("$.toCreate[0].currentPrice").value(8.50))
            .andExpect(jsonPath("$.toUpdate").isEmpty)
            .andExpect(jsonPath("$.toDelete").isEmpty)
            .andExpect(jsonPath("$.summary.totalToCreate").value(1))
            .andExpect(jsonPath("$.summary.totalToUpdate").value(0))
            .andExpect(jsonPath("$.summary.totalToDelete").value(0))
    }

    @Test
    fun `should return sync preview with materials to update when price changed`() {
        // Given - a material that was previously synced but price has changed
        val material = createMaterial(
            "SYNC_TEST_2",
            "Messing Test",
            publishToPricingApp = true,
            externalPricingAppId = 100,
            lastSyncedPrice = BigDecimal("4.00")
        )
        createPrice(material, BigDecimal("4.80")) // New price

        // Mock external API to return the existing product
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 100,
                    name = "Messing Test",
                    price = BigDecimal("4.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.toUpdate").isArray)
            .andExpect(jsonPath("$.toUpdate.length()").value(1))
            .andExpect(jsonPath("$.toUpdate[0].materialName").value("Messing Test"))
            .andExpect(jsonPath("$.toUpdate[0].currentPrice").value(4.80))
            .andExpect(jsonPath("$.toUpdate[0].lastSyncedPrice").value(4.00))
            .andExpect(jsonPath("$.toUpdate[0].priceStatus").value(1)) // Increased
            .andExpect(jsonPath("$.toUpdate[0].priceStatusLabel").value("Gestegen"))
            .andExpect(jsonPath("$.toCreate").isEmpty)
            .andExpect(jsonPath("$.summary.totalToUpdate").value(1))
    }

    @Test
    fun `should return sync preview with products to delete when material unpublished`() {
        // Given - no materials marked for publishing, but external product exists
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 200,
                    name = "Old Product",
                    price = BigDecimal("5.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.toDelete").isArray)
            .andExpect(jsonPath("$.toDelete.length()").value(1))
            .andExpect(jsonPath("$.toDelete[0].externalProductId").value(200))
            .andExpect(jsonPath("$.toDelete[0].productName").value("Old Product"))
            .andExpect(jsonPath("$.summary.totalToDelete").value(1))
    }

    @Test
    fun `should enqueue async sync jobs for new products`() {
        // Given - a material marked for publishing
        val material = createMaterial("SYNC_EXEC_1", "Lood Test", publishToPricingApp = true)
        createPrice(material, BigDecimal("1.35"))

        // Mock external API
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(emptyList())

        // When & Then - async endpoint returns job counts, not sync results
        securedMockMvc.post("/material-prices/sync/execute-async", "{}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.createJobsEnqueued").value(1))
            .andExpect(jsonPath("$.updateJobsEnqueued").value(0))
            .andExpect(jsonPath("$.deleteJobsEnqueued").value(0))
            .andExpect(jsonPath("$.totalJobsEnqueued").value(1))
    }

    @Test
    fun `should enqueue async sync jobs for updates`() {
        // Given - a material that was previously synced
        val material = createMaterial(
            "SYNC_EXEC_2",
            "Zink Test",
            publishToPricingApp = true,
            externalPricingAppId = 400,
            lastSyncedPrice = BigDecimal("1.00")
        )
        createPrice(material, BigDecimal("1.50")) // New price

        // Mock external API
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 400,
                    name = "Zink Test",
                    price = BigDecimal("1.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then - async endpoint returns job counts
        securedMockMvc.post("/material-prices/sync/execute-async", "{}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.createJobsEnqueued").value(0))
            .andExpect(jsonPath("$.updateJobsEnqueued").value(1))
            .andExpect(jsonPath("$.deleteJobsEnqueued").value(0))
            .andExpect(jsonPath("$.totalJobsEnqueued").value(1))
    }

    @Test
    fun `should enqueue async sync jobs for deletes`() {
        // Given - no materials marked for publishing, but external product exists
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 500,
                    name = "Obsolete Product",
                    price = BigDecimal("2.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then - async endpoint returns job counts
        securedMockMvc.post("/material-prices/sync/execute-async", "{}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.createJobsEnqueued").value(0))
            .andExpect(jsonPath("$.updateJobsEnqueued").value(0))
            .andExpect(jsonPath("$.deleteJobsEnqueued").value(1))
            .andExpect(jsonPath("$.totalJobsEnqueued").value(1))
    }

    @Test
    fun `should enqueue jobs even when there are materials to sync`() {
        // Given - a material marked for publishing
        val material = createMaterial("SYNC_ERROR_1", "Error Test", publishToPricingApp = true)
        createPrice(material, BigDecimal("5.00"))

        // Mock external API
        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(emptyList())

        // When & Then - async endpoint enqueues jobs, errors are handled by JobRunr
        securedMockMvc.post("/material-prices/sync/execute-async", "{}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalJobsEnqueued").value(1))
    }

    @Test
    fun `should not include materials without publish flag in sync`() {
        // Given - a material NOT marked for publishing
        val material = createMaterial("NO_SYNC_1", "Private Material", publishToPricingApp = false)
        createPrice(material, BigDecimal("10.00"))

        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(emptyList())

        // When & Then
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.toCreate").isEmpty)
            .andExpect(jsonPath("$.toUpdate").isEmpty)
            .andExpect(jsonPath("$.summary.totalToCreate").value(0))
    }

    @Test
    fun `should calculate price status correctly for decreased price`() {
        // Given - a material with decreased price
        val material = createMaterial(
            "SYNC_DECREASE_1",
            "Aluminium Test",
            publishToPricingApp = true,
            externalPricingAppId = 600,
            lastSyncedPrice = BigDecimal("2.00")
        )
        createPrice(material, BigDecimal("1.50")) // Decreased

        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 600,
                    name = "Aluminium Test",
                    price = BigDecimal("2.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.toUpdate[0].priceStatus").value(2)) // Decreased
            .andExpect(jsonPath("$.toUpdate[0].priceStatusLabel").value("Gedaald"))
    }

    @Test
    fun `should calculate price status correctly for increased price`() {
        // Given - a material with decreased price
        val material = createMaterial(
            "SYNC_DECREASE_1",
            "Aluminium Test",
            publishToPricingApp = true,
            externalPricingAppId = 600,
            lastSyncedPrice = BigDecimal("2.00")
        )
        createPrice(material, BigDecimal("2.50")) // Decreased

        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 600,
                    name = "Aluminium Test",
                    price = BigDecimal("2.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.toUpdate[0].priceStatus").value(1)) // Decreased
            .andExpect(jsonPath("$.toUpdate[0].priceStatusLabel").value("Gestegen"))
    }

    @Test
    fun `should calculate price status correctly for equal price`() {
        // Given - a material with unchanged price (same as last synced)
        val material = createMaterial(
            "SYNC_UNCHANGED_1",
            "Aluminium Test",
            publishToPricingApp = true,
            externalPricingAppId = 600,
            lastSyncedPrice = BigDecimal("2.00")
        )
        createPrice(material, BigDecimal("2.00")) // Same price

        whenever(pricingAppSync.fetchExternalProducts()).thenReturn(
            listOf(
                ExternalProduct(
                    id = 600,
                    name = "Aluminium Test",
                    price = BigDecimal("2.00"),
                    priceStatus = 0,
                    createdAt = "01-12-2025",
                    updatedAt = "01-12-2025"
                )
            )
        )

        // When & Then - unchanged materials go to the unchanged list, not toUpdate
        securedMockMvc.get("/material-prices/sync/preview")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unchanged[0].priceStatus").value(0)) // Unchanged
            .andExpect(jsonPath("$.unchanged[0].priceStatusLabel").value("Onveranderd"))
    }

    // Helper methods

    private fun createMaterial(
        code: String,
        name: String,
        publishToPricingApp: Boolean = false,
        externalPricingAppId: Int? = null,
        lastSyncedPrice: BigDecimal? = null,
        defaultPrice: BigDecimal = BigDecimal("10.00")
    ): CatalogItemDto {
        val material = CatalogItemDto(
            id = UUID.randomUUID(),
            type = CatalogItemType.MATERIAL,
            code = code,
            name = name,
            category = testCategory,
            consignorParty = null,
            unitOfMeasure = "KG",
            vatRate = testVatRate!!,
            defaultPrice = defaultPrice,
            status = "ACTIVE",
            purchaseAccountNumber = null,
            salesAccountNumber = null
        )
        val savedMaterial = catalogItemJpaRepository.save(material)

        // Create sync record if publishing is enabled
        if (publishToPricingApp) {
            val syncRecord = MaterialPricingAppSyncDto(
                material = savedMaterial,
                publishToPricingApp = true,
                externalPricingAppId = externalPricingAppId,
                externalPricingAppSyncedAt = if (externalPricingAppId != null) Instant.now() else null,
                lastSyncedPrice = lastSyncedPrice
            )
            syncJpaRepository.save(syncRecord)
        }

        return savedMaterial
    }

    private fun createPrice(material: CatalogItemDto, price: BigDecimal): CatalogItemDto {
        // Update the material's default price directly
        val updated = material.copy(defaultPrice = price)
        return catalogItemJpaRepository.save(updated)
    }
}
