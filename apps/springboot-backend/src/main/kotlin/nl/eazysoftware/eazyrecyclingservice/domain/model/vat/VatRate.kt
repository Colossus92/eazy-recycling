package nl.eazysoftware.eazyrecyclingservice.domain.model.vat

import java.util.*
import kotlin.time.Instant


data class VatRate(
  val id: VatRateId,
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

data class VatRateId(val value: UUID)

enum class VatTaxScenario(val displayName: String) {
  STANDARD("Standaard"),
  REVERSE_CHARGE("Verlegd"),
  ZERO_RATED_EXPORT("Export 0%"),
  EXEMPT("Vrijgesteld"),
}
