package nl.eazysoftware.eazyrecyclingservice.repository.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import kotlin.time.Clock
import kotlin.time.toJavaInstant

interface MaterialPriceJpaRepository : JpaRepository<MaterialPriceDto, Long> {

    @Query(
        """
        SELECT mp FROM MaterialPriceDto mp
        JOIN FETCH mp.material
        WHERE mp.id = :id
        """
    )
    fun findByIdWithMaterial(id: Long): MaterialPriceDto?

    @Query(
        """
        SELECT mp FROM MaterialPriceDto mp
        JOIN FETCH mp.material
        WHERE mp.validTo IS NULL OR mp.validTo > :now
        """
    )
    fun findAllActive(now: java.time.Instant): List<MaterialPriceDto>

    @Query(
        """
        SELECT mp FROM MaterialPriceDto mp
        JOIN FETCH mp.material
        WHERE mp.material.id = :materialId
        AND (mp.validTo IS NULL OR mp.validTo > :now)
        """
    )
    fun findActiveByMaterialId(materialId: Long, now: java.time.Instant): List<MaterialPriceDto>
}

@Repository
class MaterialPriceRepository(
    private val jpaRepository: MaterialPriceJpaRepository,
    private val mapper: MaterialPriceMapper
) : MaterialPrices {

    override fun getAllActivePrices(): List<MaterialPrice> {
        val now = Clock.System.now().toJavaInstant()
        return jpaRepository.findAllActive(now).map { mapper.toDomain(it) }
    }

    override fun getPriceById(id: Long): MaterialPrice? {
        return jpaRepository.findByIdWithMaterial(id)?.let { mapper.toDomain(it) }
    }

    override fun getActivePricesByMaterialId(materialId: Long): List<MaterialPrice> {
        val now = Clock.System.now().toJavaInstant()
        return jpaRepository.findActiveByMaterialId(materialId, now).map { mapper.toDomain(it) }
    }

    override fun createPrice(price: MaterialPrice): MaterialPrice {
        val dto = mapper.toDto(price.copy(id = null, validTo = null))
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updatePrice(id: Long, price: MaterialPrice): MaterialPrice {
        // Get the existing price
        val existing = jpaRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Price with id $id not found")

        // Set valid_to to now for the existing price
        val now = Clock.System.now()
        val expiredDto = existing.copy(validTo = now.toJavaInstant())
        jpaRepository.save(expiredDto)

        // Create new price entry with valid_from = now and valid_to = null
        val newDto = mapper.toDto(
            price.copy(
                id = null,
                validFrom = now,
                validTo = null
            )
        )
        val saved = jpaRepository.save(newDto)
        return mapper.toDomain(saved)
    }

    override fun deletePrice(id: Long) {
        val existing = jpaRepository.findByIdOrNull(id)
            ?: throw IllegalArgumentException("Price with id $id not found")

        // Set valid_to to now
        val now = Clock.System.now().toJavaInstant()
        val expiredDto = existing.copy(validTo = now)
        jpaRepository.save(expiredDto)
    }
}
