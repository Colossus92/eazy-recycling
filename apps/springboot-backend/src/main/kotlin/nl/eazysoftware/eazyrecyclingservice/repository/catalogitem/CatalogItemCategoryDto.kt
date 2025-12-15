package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity

@Entity
@Table(name = "catalog_item_categories")
data class CatalogItemCategoryDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "type", nullable = false)
    val type: String,

    @Column(name = "code", nullable = false)
    val code: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @Column(name = "description")
    val description: String? = null,
) : AuditableEntity()
