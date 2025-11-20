package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "material_prices")
data class MaterialPriceDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_id", nullable = false)
    val material: MaterialDto,

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    val price: BigDecimal,

    @Column(name = "currency", nullable = false)
    val currency: String,

    @Column(name = "valid_from", nullable = false)
    val validFrom: Instant,

    @Column(name = "valid_to")
    val validTo: Instant? = null
)
