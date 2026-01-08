package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import nl.eazysoftware.eazyrecyclingservice.application.usecase.material.MaterialPriceSyncService
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.PricingAppSync
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SyncJobsEnqueued
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.SyncPreview
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.math.BigDecimal
import java.util.*

@RestController
@RequestMapping("/material-prices/sync")
@PreAuthorize(HAS_ADMIN_OR_PLANNER)
class MaterialPriceSyncController(
    private val syncService: MaterialPriceSyncService,
    private val pricingAppSync: PricingAppSync
) {

    /**
     * Get a preview of what changes would be made during sync.
     * Returns lists of materials to create, update, delete, and unchanged.
     */
    @GetMapping("/preview")
    fun getSyncPreview(): SyncPreviewResponse {
        val preview = syncService.generateSyncPreview()
        return preview.toResponse()
    }

    /**
     * Execute the sync operation asynchronously using JobRunr.
     * Each operation is enqueued as a durable job that will retry on failure.
     */
    @PostMapping("/execute-async")
    fun executeSyncAsync(): SyncJobsEnqueuedResponse {
        val result = syncService.executeSyncAsync()
        return result.toResponse()
    }

    /**
     * Get list of external products currently in the pricing app.
     * Used by frontend to populate the dropdown for linking materials to existing products.
     */
    @GetMapping("/products")
    fun getExternalProducts(): List<ExternalProductResponse> {
        return pricingAppSync.fetchExternalProducts().map {
            ExternalProductResponse(
                id = it.id,
                name = it.name,
                price = it.price,
                priceStatus = it.priceStatus,
                priceStatusLabel = when (it.priceStatus) {
                    1 -> "Gestegen"
                    2 -> "Gedaald"
                    else -> "Onveranderd"
                }
            )
        }
    }
}

// Response DTOs

data class SyncPreviewResponse(
    val toCreate: List<MaterialToSyncResponse>,
    val toUpdate: List<MaterialToSyncResponse>,
    val toDelete: List<ExternalProductToDeleteResponse>,
    val unchanged: List<MaterialToSyncResponse>,
    val summary: SyncPreviewSummary
)

data class SyncPreviewSummary(
    val totalToCreate: Int,
    val totalToUpdate: Int,
    val totalToDelete: Int,
    val totalUnchanged: Int
)

data class MaterialToSyncResponse(
    val materialId: UUID,
    val materialName: String,
    val currentPrice: BigDecimal,
    val lastSyncedPrice: BigDecimal?,
    val externalProductId: Int?,
    val priceStatus: Int,
    val priceStatusLabel: String
)

data class ExternalProductToDeleteResponse(
    val externalProductId: Int,
    val productName: String
)

data class SyncJobsEnqueuedResponse(
    val createJobsEnqueued: Int,
    val updateJobsEnqueued: Int,
    val deleteJobsEnqueued: Int,
    val totalJobsEnqueued: Int
)

data class ExternalProductResponse(
    val id: Int,
    val name: String,
    val price: BigDecimal,
    val priceStatus: Int,
    val priceStatusLabel: String
)

// Extension functions for mapping

private fun SyncPreview.toResponse(): SyncPreviewResponse {
    return SyncPreviewResponse(
        toCreate = toCreate.map { it.toResponse() },
        toUpdate = toUpdate.map { it.toResponse() },
        toDelete = toDelete.map { ExternalProductToDeleteResponse(it.externalProductId, it.productName) },
        unchanged = unchanged.map { it.toResponse() },
        summary = SyncPreviewSummary(
            totalToCreate = toCreate.size,
            totalToUpdate = toUpdate.size,
            totalToDelete = toDelete.size,
            totalUnchanged = unchanged.size
        )
    )
}

private fun nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialToSync.toResponse(): MaterialToSyncResponse {
    return MaterialToSyncResponse(
        materialId = materialId,
        materialName = materialName,
        currentPrice = currentPrice,
        lastSyncedPrice = lastSyncedPrice,
        externalProductId = externalProductId,
        priceStatus = priceStatus,
        priceStatusLabel = when (priceStatus) {
            1 -> "Gestegen"
            2 -> "Gedaald"
            else -> "Onveranderd"
        }
    )
}

private fun SyncJobsEnqueued.toResponse(): SyncJobsEnqueuedResponse {
    return SyncJobsEnqueuedResponse(
        createJobsEnqueued = createJobsEnqueued,
        updateJobsEnqueued = updateJobsEnqueued,
        deleteJobsEnqueued = deleteJobsEnqueued,
        totalJobsEnqueued = totalJobsEnqueued
    )
}
