package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.Material
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface MaterialJpaRepository : JpaRepository<MaterialDto, Long>

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

    override fun createMaterial(material: Material): Material {
        val dto = mapper.toDto(material.copy(id = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateMaterial(id: Long, material: Material): Material {
        val dto = mapper.toDto(material.copy(id = id, updatedAt = kotlin.time.Clock.System.now()))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun deleteMaterial(id: Long) {
        jpaRepository.deleteById(id)
    }
}
