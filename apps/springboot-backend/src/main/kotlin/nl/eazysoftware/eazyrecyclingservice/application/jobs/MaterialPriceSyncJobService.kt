package nl.eazysoftware.eazyrecyclingservice.application.jobs

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.CreateProductRequest
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.PricingAppSync
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.UpdateProductRequest
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPriceSyncLogDto
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPriceSyncLogJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialPricingAppSyncRepository
import org.jobrunr.jobs.annotations.Job
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.*

/**
 * JobRunr-based service for executing material price sync operations.
 * Each sync operation (create, update, delete) is executed as a separate durable job
 * ensuring reliability even if the server restarts mid-sync.
 */
@Service
class MaterialPriceSyncJobService(
    private val pricingAppSync: PricingAppSync,
    private val syncRepository: MaterialPricingAppSyncRepository,
    private val syncLogRepository: MaterialPriceSyncLogJpaRepository
) {
    private val logger = LoggerFactory.getLogger(MaterialPriceSyncJobService::class.java)

    /**
     * Create a new product in the external pricing app.
     * This method is enqueued by JobRunr and executed asynchronously.
     */
    @Job(name = "Sync Create Product: %0", retries = 3)
    @Transactional
    fun syncCreateProduct(
        materialId: UUID,
        materialName: String,
        price: BigDecimal,
        priceStatus: Int
    ) {
        logger.info("Creating product in pricing app for material $materialId: $materialName at $price")

        try {
            val externalId = pricingAppSync.createProduct(
                CreateProductRequest(
                    name = materialName,
                    price = price,
                    priceStatus = priceStatus
                )
            )

            syncRepository.updateSyncMetadata(materialId, externalId, price)
            logSyncOperation(materialId, externalId, "create", price, priceStatus, "success", null)

            logger.info("Successfully created product in pricing app with external ID $externalId for material $materialId")
        } catch (e: Exception) {
            logger.error("Failed to create product in pricing app for material $materialId: ${e.message}", e)
            logSyncOperation(materialId, null, "create", price, priceStatus, "error", e.message)
            throw e // JobRunr will retry based on @Job annotation
        }
    }

    /**
     * Update an existing product in the external pricing app.
     * This method is enqueued by JobRunr and executed asynchronously.
     */
    @Job(name = "Sync Update Product: %0", retries = 3)
    @Transactional
    fun syncUpdateProduct(
        materialId: UUID,
        externalId: Int,
        materialName: String,
        price: BigDecimal,
        priceStatus: Int
    ) {
        logger.info("Updating product $externalId in pricing app for material $materialId: $materialName at $price")

        try {
            pricingAppSync.updateProduct(
                externalId,
                UpdateProductRequest(
                    name = materialName,
                    price = price,
                    priceStatus = priceStatus
                )
            )

            syncRepository.updateSyncMetadata(materialId, externalId, price)
            logSyncOperation(materialId, externalId, "update", price, priceStatus, "success", null)

            logger.info("Successfully updated product $externalId in pricing app for material $materialId")
        } catch (e: Exception) {
            logger.error("Failed to update product $externalId in pricing app: ${e.message}", e)
            logSyncOperation(materialId, externalId, "update", price, priceStatus, "error", e.message)
            throw e // JobRunr will retry based on @Job annotation
        }
    }

    /**
     * Delete a product from the external pricing app.
     * This method is enqueued by JobRunr and executed asynchronously.
     */
    @Job(name = "Sync Delete Product: %0", retries = 3)
    @Transactional
    fun syncDeleteProduct(externalId: Int) {
        logger.info("Deleting product $externalId from pricing app")

        try {
            pricingAppSync.deleteProduct(externalId)
            logSyncOperation(null, externalId, "delete", null, null, "success", null)

            logger.info("Successfully deleted product $externalId from pricing app")
        } catch (e: Exception) {
            logger.error("Failed to delete product $externalId from pricing app: ${e.message}", e)
            logSyncOperation(null, externalId, "delete", null, null, "error", e.message)
            throw e // JobRunr will retry based on @Job annotation
        }
    }

    private fun logSyncOperation(
        materialId: UUID?,
        externalProductId: Int?,
        action: String,
        priceSynced: BigDecimal?,
        priceStatusSent: Int?,
        status: String,
        errorMessage: String?
    ) {
        try {
            val logEntry = MaterialPriceSyncLogDto(
                materialId = materialId,
                externalProductId = externalProductId,
                action = action,
                priceSynced = priceSynced,
                priceStatusSent = priceStatusSent,
                status = status,
                errorMessage = errorMessage
            )
            syncLogRepository.save(logEntry)
        } catch (e: Exception) {
            logger.error("Failed to log sync operation: ${e.message}", e)
        }
    }
}
