package nl.eazysoftware.eazyrecyclingservice.repository.vat

import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate
import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRateId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.VatRates
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.*

interface VatRateJpaRepository : JpaRepository<VatRateDto, UUID> {
    fun findByVatCode(vatCode: String): VatRateDto?
}

@Repository
class VatRateRepository(
    private val jpaRepository: VatRateJpaRepository,
    private val mapper: VatRateMapper
) : VatRates {

    override fun getAllVatRates(): List<VatRate> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

    override fun getVatRateById(id: VatRateId): VatRate? {
        return jpaRepository.findByIdOrNull(id.value)?.let { mapper.toDomain(it) }
    }

    override fun getVatRateByCode(vatCode: String): VatRate? {
        return jpaRepository.findByVatCode(vatCode)?.let { mapper.toDomain(it) }
    }

    override fun createVatRate(vatRate: VatRate): VatRate {
        val dto = mapper.toDto(vatRate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun updateVatRate(id: VatRateId, vatRate: VatRate): VatRate {
        val dto = mapper.toDto(vatRate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun deleteVatRate(id: VatRateId) {
        jpaRepository.deleteById(id.value)
    }
}
