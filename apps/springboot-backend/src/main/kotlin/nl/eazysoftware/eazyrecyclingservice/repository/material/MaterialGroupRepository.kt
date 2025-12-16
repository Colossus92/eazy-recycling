package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialGroups
import nl.eazysoftware.eazyrecyclingservice.repository.catalogitem.CatalogItemCategoryJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.material.MaterialGroupMapper.Companion.MATERIAL_TYPE
import org.springframework.stereotype.Repository

@Repository
class MaterialGroupRepository(
    private val jpaRepository: CatalogItemCategoryJpaRepository,
    private val mapper: MaterialGroupMapper
) : MaterialGroups {

    override fun getAllMaterialGroups(): List<MaterialGroup> {
        return jpaRepository.findByType(MATERIAL_TYPE).map { mapper.toDomain(it) }
    }

    override fun getMaterialGroupById(id: Long): MaterialGroup? {
        return jpaRepository.findByIdAndType(id, MATERIAL_TYPE)?.let { mapper.toDomain(it) }
    }

    override fun createMaterialGroup(materialGroup: MaterialGroup): MaterialGroup {
        val dto = mapper.toDto(materialGroup.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateMaterialGroup(id: Long, materialGroup: MaterialGroup): MaterialGroup {
        val existing = jpaRepository.findByIdAndType(id, MATERIAL_TYPE)
            ?: throw NoSuchElementException("MaterialGroup with id $id not found")
        existing.code = materialGroup.code
        existing.name = materialGroup.name
        existing.description = materialGroup.description
        val saved = jpaRepository.save(existing)
        return mapper.toDomain(saved)
    }

    override fun deleteMaterialGroup(id: Long) {
        val existing = jpaRepository.findByIdAndType(id, MATERIAL_TYPE)
            ?: throw NoSuchElementException("MaterialGroup with id $id not found")
        jpaRepository.delete(existing)
    }
}
