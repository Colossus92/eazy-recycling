package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialGroup
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialGroups
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface MaterialGroupJpaRepository : JpaRepository<MaterialGroupDto, Long>

@Repository
class MaterialGroupRepository(
    private val jpaRepository: MaterialGroupJpaRepository,
    private val mapper: MaterialGroupMapper
) : MaterialGroups {

    override fun getAllMaterialGroups(): List<MaterialGroup> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

    override fun getMaterialGroupById(id: Long): MaterialGroup? {
        return jpaRepository.findByIdOrNull(id)?.let { mapper.toDomain(it) }
    }

    override fun createMaterialGroup(materialGroup: MaterialGroup): MaterialGroup {
        val dto = mapper.toDto(materialGroup.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateMaterialGroup(id: Long, materialGroup: MaterialGroup): MaterialGroup {
        val dto = mapper.toDto(materialGroup.copy(id = id, updatedAt = kotlin.time.Clock.System.now()))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun deleteMaterialGroup(id: Long) {
        jpaRepository.deleteById(id)
    }
}
