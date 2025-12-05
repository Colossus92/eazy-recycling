package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.UndeclaredWeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.WeightTicketDeclarationSnapshot

/**
 * Port for persisting and querying weight ticket declaration snapshots.
 * Used to track what has been declared and detect changes requiring corrective declarations.
 */
interface WeightTicketDeclarationSnapshots {

  /**
   * Saves a declaration snapshot.
   */
  fun save(snapshot: WeightTicketDeclarationSnapshot)

  /**
   * Saves multiple declaration snapshots.
   */
  fun saveAll(snapshots: List<WeightTicketDeclarationSnapshot>)

  /**
   * Finds the latest snapshot for a specific weight ticket line.
   *
   * @param weightTicketId The weight ticket ID
   * @param lineIndex The index of the line within the weight ticket
   * @return The latest snapshot if exists, null otherwise
   */
  fun findLatestByWeightTicketLine(weightTicketId: Long, lineIndex: Int): WeightTicketDeclarationSnapshot?

  /**
   * Finds all weight ticket lines that have not been declared yet.
   * Only includes tickets from months that have passed their declaration deadline.
   *
   * @param cutoffDate The cutoff date - only weight tickets with weightedAt before this date are included
   * @return List of undeclared weight ticket lines
   */
  fun findUndeclaredLines(cutoffDate: YearMonth): List<UndeclaredWeightTicketLine>

}
