package nl.eazysoftware.eazyrecyclingservice.repository.product

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto
import java.math.BigDecimal

@Entity
@Table(name = "products")
data class ProductDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "code", nullable = false, unique = true)
    val code: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "product_category_id")
    val category: ProductCategoryDto?,

    @Column(name = "unit_of_measure", nullable = false)
    val unitOfMeasure: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_code", nullable = false, referencedColumnName = "vat_code")
    val vatRate: VatRateDto,

    @Column(name = "gl_account_code")
    val glAccountCode: String?,

    @Column(name = "status", nullable = false)
    val status: String,

    @Column(name = "default_price", precision = 15, scale = 4)
    val defaultPrice: BigDecimal?,

    @Column(name = "description")
    val description: String?,
) : AuditableEntity()
