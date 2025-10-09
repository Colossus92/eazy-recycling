package nl.eazysoftware.eazyrecyclingservice.domain.transport

/**
 * Dutch VIHB number (Vergunning Inzamelen Huishoudelijke en Bedrijfsafvalstoffen)
 *
 * Format: 6 digits followed by 4 letters (X, V, I, H, or B)
 * Example: 000100VXXX, 000200VXXX
 *
 * Validation rules:
 * - Exactly 6 digits
 * - Followed by exactly 4 letters (only X, V, I, H, or B allowed)
 * - Maximum 3 X's in combination with at least one other letter
 */
data class VihbNumber(val value: String) {
  init {
    require(value.matches(Regex("\\d{6}[XVIHB]{4}"))) {
      "VIHB nummer moet bestaan uit exact 6 cijfers gevolgd door 4 letters (X, V, I, H of B), maar was: $value"
    }

    val letters = value.substring(6)
    val xCount = letters.count { it == 'X' }

    require(xCount <= 3) {
      "VIHB nummer mag maximaal 3 keer een X bevatten, maar had $xCount X-en: $value"
    }
  }
}
