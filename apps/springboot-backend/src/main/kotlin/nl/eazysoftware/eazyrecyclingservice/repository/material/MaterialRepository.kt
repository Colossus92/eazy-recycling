package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface MaterialJpaRepository : JpaRepository<MaterialDto, Long> {

    @Query(
        value = """
            SELECT
                m.id as id,
                m.code as code,
                m.name as name,
                mg.id as materialGroupId,
                mg.code as materialGroupCode,
                mg.name as materialGroupName,
                m.unit_of_measure as unitOfMeasure,
                m.vat_code as vatCode,
                m.status as status,
                m.created_at as createdAt,
                m.created_by as createdBy,
                m.last_modified_at as updatedAt,
                m.last_modified_by as updatedBy
            FROM materials m
            INNER JOIN material_groups mg ON m.material_group_id = mg.id
        """,
        nativeQuery = true
    )
    fun findAllMaterialsWithGroupDetails(): List<MaterialQueryResult>

    @Query(
        value = """
            SELECT
                m.id as id,
                m.code as code,
                m.name as name,
                mg.id as materialGroupId,
                mg.code as materialGroupCode,
                mg.name as materialGroupName,
                m.unit_of_measure as unitOfMeasure,
                m.vat_code as vatCode,
                m.status as status,
                m.created_at as createdAt,
                m.created_by as createdBy,
                m.last_modified_at as updatedAt,
                m.last_modified_by as updatedBy
            FROM materials m
            INNER JOIN material_groups mg ON m.material_group_id = mg.id
            WHERE m.id = :id
        """,
        nativeQuery = true
    )
    fun findMaterialWithGroupDetailsById(id: Long): MaterialQueryResult?
}

@Repository
class MaterialRepository(
    private val jpaRepository: MaterialJpaRepository,
    private val mapper: MaterialMapper
) : Materials {

    override fun getAllMaterials(): List<Material> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

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
}
