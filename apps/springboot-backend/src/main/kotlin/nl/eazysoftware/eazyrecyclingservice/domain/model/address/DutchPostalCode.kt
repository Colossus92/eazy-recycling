package nl.eazysoftware.eazyrecyclingservice.domain.model.address

/**
 * Dutch postal code value object (4 digits + 2 letters, e.g., "1234AB")
 * Accepts input with or without space and normalizes to format without space.
 * 
 * Domain constraints relaxed per ADR-0017 to allow Exact Online sync with incomplete data.
 * Validation is enforced at controller level for user-created data and by LMA validation for declarations.
 */
data class DutchPostalCode(val input: String) {
  val value: String = input.replace(" ", "").uppercase()
}
