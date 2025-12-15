package nl.eazysoftware.eazyrecyclingservice.repository.product

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity

@Entity
@Table(name = "product_categories")
data class ProductCategoryDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "code", nullable = false, unique = true)
    val code: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "description")
    val description: String?,
) : AuditableEntity()
