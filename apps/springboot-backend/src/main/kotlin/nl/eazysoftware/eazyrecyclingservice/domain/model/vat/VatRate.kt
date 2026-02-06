package nl.eazysoftware.eazyrecyclingservice.domain.model.vat

import kotlin.time.Instant


data class VatRate(
  val vatCode: String,
  val percentage: String,
  val validFrom: Instant,
  val validTo: Instant?,
  val countryCode: String,
  val description: String,
  val taxScenario: VatTaxScenario,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
) {
  fun isReverseCharge(): Boolean = taxScenario == VatTaxScenario.REVERSE_CHARGE
}

enum class VatTaxScenario {
  STANDARD,
  REVERSE_CHARGE,
  ZERO_RATED_EXPORT,
  EXEMPT
}
