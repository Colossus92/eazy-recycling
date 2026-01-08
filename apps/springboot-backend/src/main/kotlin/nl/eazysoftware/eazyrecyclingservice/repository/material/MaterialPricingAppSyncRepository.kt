package nl.eazysoftware.eazyrecyclingservice.repository.material

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.Instant
import java.util.*

interface MaterialPricingAppSyncJpaRepository : JpaRepository<MaterialPricingAppSyncDto, Long> {

    @Query(
        """
        SELECT s FROM MaterialPricingAppSyncDto s
        JOIN FETCH s.material m
        LEFT JOIN FETCH m.category
        LEFT JOIN FETCH m.vatRate
        WHERE s.publishToPricingApp = true
        """
    )
    fun findAllForPublishing(): List<MaterialPricingAppSyncDto>

    @Modifying(clearAutomatically = true)
    @Query(
        """
        UPDATE MaterialPricingAppSyncDto s
        SET s.externalPricingAppId = :externalId,
            s.externalPricingAppSyncedAt = :syncedAt,
            s.lastSyncedPrice = :syncedPrice
        WHERE s.material.id = :materialId
        """
    )
    fun updateSyncMetadata(
        materialId: UUID,
        externalId: Int,
        syncedAt: Instant,
        syncedPrice: BigDecimal
    )
}

@Repository
class MaterialPricingAppSyncRepository(
    private val jpaRepository: MaterialPricingAppSyncJpaRepository
) {
    fun findAllForPublishing(): List<MaterialPricingAppSyncDto> {
        return jpaRepository.findAllForPublishing()
    }

    fun save(dto: MaterialPricingAppSyncDto): MaterialPricingAppSyncDto {
        return jpaRepository.save(dto)
    }

    fun updateSyncMetadata(
        materialId: UUID,
        externalId: Int,
        syncedPrice: BigDecimal
    ) {
        val now = Instant.now()
        jpaRepository.updateSyncMetadata(materialId, externalId, now, syncedPrice)
    }
}
