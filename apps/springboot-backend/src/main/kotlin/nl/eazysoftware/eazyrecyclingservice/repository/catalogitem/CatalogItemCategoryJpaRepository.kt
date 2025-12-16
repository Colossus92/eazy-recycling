package nl.eazysoftware.eazyrecyclingservice.repository.catalogitem

import org.springframework.data.jpa.repository.JpaRepository

interface CatalogItemCategoryJpaRepository : JpaRepository<CatalogItemCategoryDto, Long> {
    fun findByType(type: String): List<CatalogItemCategoryDto>
    fun findByIdAndType(id: Long, type: String): CatalogItemCategoryDto?
}
