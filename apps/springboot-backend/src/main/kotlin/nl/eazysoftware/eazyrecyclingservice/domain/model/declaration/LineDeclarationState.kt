package nl.eazysoftware.eazyrecyclingservice.domain.model.declaration

import java.math.BigDecimal
import kotlin.time.Instant

/**
 * Value object representing the declaration state of a weight ticket line.
 * Tracks whether a line has been declared and what weight was declared.
 */
data class LineDeclarationState(
  val declaredWeight: BigDecimal?,
  val lastDeclaredAt: Instant?
) {

  /**
   * Returns true if this line needs a declaration (either initial or correction).
   * A line needs declaration if it has never been declared, or if the current weight
   * differs from the declared weight.
   */
  fun needsDeclaration(currentWeight: BigDecimal): Boolean =
    declaredWeight == null || declaredWeight.compareTo(currentWeight) != 0


  companion object {
    /**
     * Creates an undeclared state.
     */
    fun undeclared(): LineDeclarationState = LineDeclarationState(null, null)
  }
}
