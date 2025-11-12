package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.ReceivalDeclaration

/**
 * Port for querying waste streams that need to be declared for the first time.
 *
 * A waste stream should be declared for the first time when:
 * - It has active transports in the given month
 * - It has never been declared before (no previous declarations exist)
 * - The waste stream is in ACTIVE status
 *
 * TODO: Detailed query specifications need to be defined:
 * - Exact criteria for "first receival"
 * - Relationship with transport records
 * - Declaration history tracking
 */
interface FirstReceivalWasteStreamQuery {

  /**
   * Finds all receival declarations for waste streams that need to be declared for the first time
   * in the given year-month period.
   *
   * @param yearMonth The year-month period to query for
   * @return List of receival declarations for first-time waste streams
   */
  fun findFirstReceivalDeclarations(yearMonth: YearMonth): List<ReceivalDeclaration>
}
