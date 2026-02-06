package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate
import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRateId
import java.util.*

interface VatRates {
  fun getAllVatRates(): List<VatRate>
  fun getVatRateById(id: VatRateId): VatRate?
  fun getVatRateByCode(vatCode: String): VatRate?
  fun createVatRate(vatRate: VatRate): VatRate
  fun updateVatRate(id: VatRateId, vatRate: VatRate): VatRate
  fun deleteVatRate(id: VatRateId)
}
