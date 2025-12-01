package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.MonthlyReceivalDeclaration

/**
 * Port for querying waste streams that need monthly receival declarations.
 *
 * A waste stream should have a monthly receival declaration when:
 * - It has active transports (weight tickets) in the given month
 * - It has already been declared before (has existing declarations)
 * - The waste stream is in ACTIVE status
 * - Only for processor_id of current tenant
 */
interface MonthlyReceivalWasteStreamQuery {

  /**
   * Finds all monthly receival declarations for waste streams in the given year-month period.
   *
   * @param yearMonth The year-month period to query for
   * @return List of monthly receival declarations
   */
  fun findMonthlyReceivalDeclarations(yearMonth: YearMonth): List<MonthlyReceivalDeclaration>
}
