package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import java.math.BigDecimal
import java.time.Instant

/**
 * Entity for tracking material synchronization to the external pricing app.
 * Kept separate from MaterialDto to isolate company-specific features.
 */
@Entity
@Table(name = "material_pricing_app_sync")
data class MaterialPricingAppSyncDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false, unique = true)
    val material: CatalogItemDto,

    @Column(name = "publish_to_pricing_app", nullable = false)
    val publishToPricingApp: Boolean = false,

    @Column(name = "external_pricing_app_id")
    val externalPricingAppId: Int? = null,

    @Column(name = "external_pricing_app_name", nullable = false)
    val externalPricingAppName: String,

    @Column(name = "external_pricing_app_synced_at")
    val externalPricingAppSyncedAt: Instant? = null,

    @Column(name = "last_synced_price", precision = 19, scale = 4)
    val lastSyncedPrice: BigDecimal? = null,

) : AuditableEntity()
