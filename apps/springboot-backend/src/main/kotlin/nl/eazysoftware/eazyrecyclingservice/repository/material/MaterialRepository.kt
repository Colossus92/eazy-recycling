package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface MaterialJpaRepository : JpaRepository<CatalogItemDto, Long> {

    @Query(
        value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as materialGroupId,
                cic.code as materialGroupCode,
                cic.name as materialGroupName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.type = 'MATERIAL'
        """,
        nativeQuery = true
    )
    fun findAllMaterialsWithGroupDetails(): List<MaterialQueryResult>

    @Query(
        value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as materialGroupId,
                cic.code as materialGroupCode,
                cic.name as materialGroupName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.id = :id AND ci.type = 'MATERIAL'
        """,
        nativeQuery = true
    )
    fun findMaterialWithGroupDetailsById(id: Long): MaterialQueryResult?

    @Query(
        value = """
            SELECT
                ci.id as id,
                ci.code as code,
                ci.name as name,
                cic.id as materialGroupId,
                cic.code as materialGroupCode,
                cic.name as materialGroupName,
                ci.unit_of_measure as unitOfMeasure,
                ci.vat_code as vatCode,
                ci.sales_account_number as salesAccountNumber,
                ci.purchase_account_number as purchaseAccountNumber,
                ci.status as status,
                ci.created_at as createdAt,
                ci.created_by as createdBy,
                ci.last_modified_at as updatedAt,
                ci.last_modified_by as updatedBy
            FROM catalog_items ci
            LEFT JOIN catalog_item_categories cic ON ci.category_id = cic.id
            WHERE ci.type = 'MATERIAL'
              AND (LOWER(ci.name) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(ci.code) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY ci.name
        """,
        nativeQuery = true
    )
    fun searchMaterials(query: String): List<MaterialQueryResult>
}

@Repository
class MaterialRepository(
    private val jpaRepository: MaterialJpaRepository,
    private val mapper: MaterialMapper
) : Materials {

    override fun getMaterialById(id: Long): Material? {
        return jpaRepository.findByIdOrNull(id)?.let { mapper.toDomain(it) }
    }

    override fun getAllMaterialsWithGroupDetails(): List<MaterialQueryResult> {
        return jpaRepository.findAllMaterialsWithGroupDetails()
    }

    override fun getMaterialWithGroupDetailsById(id: Long): MaterialQueryResult? {
        return jpaRepository.findMaterialWithGroupDetailsById(id)
    }

    override fun createMaterial(material: Material): Material {
        val dto = mapper.toDto(material.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateMaterial(id: Long, material: Material): Material {
        val dto = mapper.toDto(material.copy(id = id))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun deleteMaterial(id: Long) {
        jpaRepository.deleteById(id)
    }

    override fun searchMaterials(query: String, limit: Int): List<MaterialQueryResult> {
        return jpaRepository.searchMaterials(query).take(limit)
    }
}
