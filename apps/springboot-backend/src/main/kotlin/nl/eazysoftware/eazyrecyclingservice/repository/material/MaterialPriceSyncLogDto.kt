package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

/**
 * Entity for logging material price sync operations to the external pricing app.
 * Provides audit trail for all sync activities.
 */
@Entity
@Table(name = "material_price_sync_log")
data class MaterialPriceSyncLogDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "material_id")
    val materialId: UUID? = null,

    @Column(name = "external_product_id")
    val externalProductId: Int? = null,

    @Column(name = "action", nullable = false)
    val action: String, // 'create', 'update', 'delete'

    @Column(name = "price_synced", precision = 19, scale = 4)
    val priceSynced: BigDecimal? = null,

    @Column(name = "price_status_sent")
    val priceStatusSent: Int? = null, // 0, 1, or 2

    @Column(name = "status", nullable = false)
    val status: String, // 'success', 'error'

    @Column(name = "error_message")
    val errorMessage: String? = null,

    @Column(name = "synced_at", nullable = false)
    val syncedAt: Instant = Instant.now(),

    @Column(name = "synced_by")
    val syncedBy: String? = null
)

@Repository
interface MaterialPriceSyncLogJpaRepository : JpaRepository<MaterialPriceSyncLogDto, Long>
