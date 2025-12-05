package nl.eazysoftware.eazyrecyclingservice.domain.model.declaration

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import java.math.BigDecimal
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Snapshot of a weight ticket line at the time of declaration.
 * Used to track what was declared and enable delta calculation for corrective declarations.
 */
data class WeightTicketDeclarationSnapshot(
  val id: Long,
  val weightTicketId: WeightTicketId,
  val weightTicketLineIndex: Int,
  val wasteStreamNumber: WasteStreamNumber,
  val declaredWeightValue: BigDecimal,
  val declarationId: String,
  val declaredAt: Instant,
  val declarationPeriod: YearMonth,
) {
  companion object {
    fun create(
      id: Long,
      weightTicketId: WeightTicketId,
      weightTicketLineIndex: Int,
      wasteStreamNumber: WasteStreamNumber,
      declaredWeightValue: BigDecimal,
      declarationId: String,
      declarationPeriod: YearMonth,
    ): WeightTicketDeclarationSnapshot {
      return WeightTicketDeclarationSnapshot(
        id = id,
        weightTicketId = weightTicketId,
        weightTicketLineIndex = weightTicketLineIndex,
        wasteStreamNumber = wasteStreamNumber,
        declaredWeightValue = declaredWeightValue,
        declarationId = declarationId,
        declaredAt = Clock.System.now(),
        declarationPeriod = declarationPeriod,
      )
    }
  }
}

/**
 * Represents a weight ticket line that needs a corrective declaration.
 * Contains both the current weight and the previously declared weight for delta calculation.
 */
data class WeightTicketLineCorrection(
  val weightTicketId: WeightTicketId,
  val weightTicketLineIndex: Int,
  val wasteStreamNumber: WasteStreamNumber,
  val previouslyDeclaredWeight: BigDecimal,
  val currentWeight: BigDecimal,
  val declarationPeriod: YearMonth,
) {
  /**
   * The weight delta (positive = additional weight, negative = weight reduction)
   */
  val delta: BigDecimal
    get() = currentWeight - previouslyDeclaredWeight
}

/**
 * Represents a weight ticket line that has not yet been declared.
 */
data class UndeclaredWeightTicketLine(
  val weightTicketId: WeightTicketId,
  val weightTicketLineIndex: Int,
  val wasteStreamNumber: WasteStreamNumber,
  val weightValue: BigDecimal,
  val weightedAt: Instant,
)
