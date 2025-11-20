package nl.eazysoftware.eazyrecyclingservice.repository.vat

import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.VatRates
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

interface VatRateJpaRepository : JpaRepository<VatRateDto, String>

@Repository
class VatRateRepository(
    private val jpaRepository: VatRateJpaRepository,
    private val mapper: VatRateMapper
) : VatRates {

    override fun getAllVatRates(): List<VatRate> {
        return jpaRepository.findAll().map { mapper.toDomain(it) }
    }

  override fun getVatRateByCode(vatCode: String): VatRate? {
        return jpaRepository.findByIdOrNull(vatCode)?.let { mapper.toDomain(it) }
    }

  override fun createVatRate(vatRate: VatRate): VatRate {
        val dto = mapper.toDto(vatRate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

  override fun updateVatRate(vatCode: String, vatRate: VatRate): VatRate {
        val dto = mapper.toDto(vatRate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

  override fun deleteVatRate(vatCode: String) {
        jpaRepository.deleteById(vatCode)
    }
}
