package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CatalogItemCategoryJpaRepository : JpaRepository<CatalogItemCategoryDto, UUID> {
    fun findByType(type: String): List<CatalogItemCategoryDto>
    fun findByIdAndType(id: UUID, type: String): CatalogItemCategoryDto?
}
