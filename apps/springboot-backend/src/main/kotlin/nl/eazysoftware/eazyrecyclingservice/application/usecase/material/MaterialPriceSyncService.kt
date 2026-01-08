package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import nl.eazysoftware.eazyrecyclingservice.application.jobs.MaterialPriceSyncJobService
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.*
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPricingAppSyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPricingAppSyncRepository
import org.jobrunr.scheduling.BackgroundJob
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * Service for synchronizing material prices to the external pricing app.
 */
@Service
class MaterialPriceSyncService(
    private val pricingAppSync: PricingAppSync,
    private val syncRepository: MaterialPricingAppSyncRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Generate a preview of what changes would be made during sync.
     * This fetches current prices and compares with external app state.
     */
    fun generateSyncPreview(): SyncPreview {
        logger.info("Generating sync preview")

        // Get materials marked for publishing from the sync table
        val syncRecords = syncRepository.findAllForPublishing()

        // Get current prices for each material (from defaultPrice on CatalogItemDto)
        val materialsWithPrices = syncRecords.mapNotNull { syncRecord ->
            val material = syncRecord.material
            val currentPrice = material.defaultPrice
            if (currentPrice != null) {
                SyncRecordWithPrice(syncRecord, currentPrice)
            } else {
                logger.warn("No current price found for material ${material.id}")
                null
            }
        }

        // Fetch external products
        val externalProducts = pricingAppSync.fetchExternalProducts()
        val externalProductsById = externalProducts.associateBy { it.id }

        // Categorize materials
        val toCreate = mutableListOf<MaterialToSync>()
        val toUpdate = mutableListOf<MaterialToSync>()
        val unchanged = mutableListOf<MaterialToSync>()

        for ((syncRecord, currentPrice) in materialsWithPrices) {
            val material = syncRecord.material
            val externalId = syncRecord.externalPricingAppId
            val lastSyncedPrice = syncRecord.lastSyncedPrice
            val priceStatus = calculatePriceStatus(currentPrice, lastSyncedPrice)

            val materialToSync = MaterialToSync(
                materialId = material.id,
                materialName = syncRecord.externalPricingAppName,
                currentPrice = currentPrice,
                lastSyncedPrice = lastSyncedPrice,
                externalProductId = externalId,
                priceStatus = priceStatus
            )

            if (externalId == null) {
                // New product to create
                toCreate.add(materialToSync)
            } else {
                val externalProduct = externalProductsById[externalId]
                if (externalProduct == null) {
                    // External product was deleted, need to recreate
                    toCreate.add(materialToSync.copy(externalProductId = null))
                } else if (currentPrice.compareTo(externalProduct.price) != 0 ||
                    syncRecord.externalPricingAppName != externalProduct.name
                ) {
                    // Price or name changed, need to update
                    toUpdate.add(materialToSync)
                } else {
                    // No changes needed
                    unchanged.add(materialToSync)
                }
            }
        }

        // Find products to delete (in external app but not marked for publishing)
        val publishedExternalIds = syncRecords
            .mapNotNull { it.externalPricingAppId }
            .toSet()

        val toDelete = externalProducts
            .filter { it.id !in publishedExternalIds }
            .map { ExternalProductToDelete(it.id, it.name) }

        logger.info(
            "Sync preview: ${toCreate.size} to create, ${toUpdate.size} to update, " +
                "${toDelete.size} to delete, ${unchanged.size} unchanged"
        )

        return SyncPreview(
            toCreate = toCreate,
            toUpdate = toUpdate,
            toDelete = toDelete,
            unchanged = unchanged
        )
    }

    /**
     * Execute the sync operation asynchronously using JobRunr.
     * Each sync operation (create, update, delete) is enqueued as a separate job
     * for durability - if the server restarts, pending jobs will resume.
     *
     * @return SyncJobsEnqueued with count of jobs enqueued for each operation type
     */
    fun executeSyncAsync(): SyncJobsEnqueued {
        logger.info("Enqueueing pricing app sync jobs")

        val preview = generateSyncPreview()
        var createJobsEnqueued = 0
        var updateJobsEnqueued = 0
        var deleteJobsEnqueued = 0

        // Enqueue create jobs
        for (material in preview.toCreate) {
            BackgroundJob.enqueue<MaterialPriceSyncJobService> { service ->
                service.syncCreateProduct(
                    material.materialId,
                    material.materialName,
                    material.currentPrice,
                    material.priceStatus
                )
            }
            createJobsEnqueued++
        }

        // Enqueue update jobs
        for (material in preview.toUpdate) {
            BackgroundJob.enqueue<MaterialPriceSyncJobService> { service ->
                service.syncUpdateProduct(
                    material.materialId,
                    material.externalProductId!!,
                    material.materialName,
                    material.currentPrice,
                    material.priceStatus
                )
            }
            updateJobsEnqueued++
        }

        // Enqueue delete jobs
        for (product in preview.toDelete) {
            BackgroundJob.enqueue<MaterialPriceSyncJobService> { service ->
                service.syncDeleteProduct(product.externalProductId)
            }
            deleteJobsEnqueued++
        }

        logger.info(
            "Sync jobs enqueued: $createJobsEnqueued create, $updateJobsEnqueued update, $deleteJobsEnqueued delete"
        )

        return SyncJobsEnqueued(
            createJobsEnqueued = createJobsEnqueued,
            updateJobsEnqueued = updateJobsEnqueued,
            deleteJobsEnqueued = deleteJobsEnqueued,
            totalJobsEnqueued = createJobsEnqueued + updateJobsEnqueued + deleteJobsEnqueued
        )
    }

    /**
     * Calculate price status for the external app.
     * 0 = unchanged, 1 = increased, 2 = decreased
     */
    private fun calculatePriceStatus(currentPrice: BigDecimal, lastSyncedPrice: BigDecimal?): Int {
        if (lastSyncedPrice == null) return 0 // First sync, no change indicator
        return when {
            currentPrice > lastSyncedPrice -> 1 // Increased
            currentPrice < lastSyncedPrice -> 2 // Decreased
            else -> 0 // Unchanged
        }
    }

    private data class SyncRecordWithPrice(
        val syncRecord: MaterialPricingAppSyncDto,
        val currentPrice: BigDecimal
    )
}
