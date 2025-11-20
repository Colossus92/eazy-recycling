package nl.eazysoftware.eazyrecyclingservice.domain.model.vat

import kotlin.time.Instant


data class VatRate(
  val vatCode: String,
  val percentage: String,
  val validFrom: Instant,
  val validTo: Instant?,
  val countryCode: String,
  val description: String
)
