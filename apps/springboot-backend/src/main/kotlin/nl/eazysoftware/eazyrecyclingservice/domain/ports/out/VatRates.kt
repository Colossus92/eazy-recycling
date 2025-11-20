package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate

interface VatRates {
  fun getAllVatRates(): List<VatRate>
  fun getVatRateByCode(vatCode: String): VatRate?
  fun createVatRate(vatRate: VatRate): VatRate
  fun updateVatRate(vatCode: String, vatRate: VatRate): VatRate
  fun deleteVatRate(vatCode: String)
}
