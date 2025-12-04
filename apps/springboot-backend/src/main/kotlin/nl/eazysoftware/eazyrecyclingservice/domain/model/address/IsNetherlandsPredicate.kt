package nl.eazysoftware.eazyrecyclingservice.domain.model.address

fun String.isNetherlands(): Boolean {
  val normalized = this.trim()
  return normalized.equals("Nederland", ignoreCase = true) ||
    normalized.equals("NL", ignoreCase = true)
}
