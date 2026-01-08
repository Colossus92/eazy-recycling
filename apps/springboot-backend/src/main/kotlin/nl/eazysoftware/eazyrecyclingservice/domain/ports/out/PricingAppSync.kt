package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import java.math.BigDecimal
import java.util.*

/**
 * Port for synchronizing material prices to the external pricing app.
 */
interface PricingAppSync {
    /**
     * Fetches all products currently in the external pricing app.
     */
    fun fetchExternalProducts(): List<ExternalProduct>

    /**
     * Creates a new product in the external pricing app.
     * @return The ID assigned by the external app
     */
    fun createProduct(request: CreateProductRequest): Int

    /**
     * Updates an existing product in the external pricing app.
     */
    fun updateProduct(externalId: Int, request: UpdateProductRequest)

    /**
     * Deletes a product from the external pricing app.
     */
    fun deleteProduct(externalId: Int)
}

/**
 * Product as returned by the external pricing app.
 */
data class ExternalProduct(
    val id: Int,
    val name: String,
    val price: BigDecimal,
    val priceStatus: Int, // 0 = unchanged, 1 = increased, 2 = decreased
    val createdAt: String,
    val updatedAt: String
)

data class CreateProductRequest(
    val name: String,
    val price: BigDecimal,
    val priceStatus: Int
)

data class UpdateProductRequest(
    val name: String,
    val price: BigDecimal,
    val priceStatus: Int
)

/**
 * Result of a sync preview - shows what changes would be made.
 */
data class SyncPreview(
    val toCreate: List<MaterialToSync>,
    val toUpdate: List<MaterialToSync>,
    val toDelete: List<ExternalProductToDelete>,
    val unchanged: List<MaterialToSync>
)

data class MaterialToSync(
    val materialId: UUID,
    val materialName: String,
    val currentPrice: BigDecimal,
    val lastSyncedPrice: BigDecimal?,
    val externalProductId: Int?,
    val priceStatus: Int // 0 = unchanged, 1 = increased, 2 = decreased
)

data class ExternalProductToDelete(
    val externalProductId: Int,
    val productName: String
)

/**
 * Result of enqueueing sync jobs asynchronously via JobRunr.
 */
data class SyncJobsEnqueued(
    val createJobsEnqueued: Int,
    val updateJobsEnqueued: Int,
    val deleteJobsEnqueued: Int,
    val totalJobsEnqueued: Int
)
