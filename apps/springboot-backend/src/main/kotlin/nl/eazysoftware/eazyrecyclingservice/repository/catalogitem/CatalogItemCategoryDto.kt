package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import java.util.*

@Entity
@Table(name = "catalog_item_categories")
data class CatalogItemCategoryDto(
    @Id
    @Column(name = "id")
    val id: UUID,

    @Column(name = "type", nullable = false)
    val type: String,

    @Column(name = "code", nullable = false)
    var code: String,

    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "description")
    var description: String? = null,
) : AuditableEntity()
