package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.MonthlyReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTimezoneBoundaries
import nl.eazysoftware.eazyrecyclingservice.domain.model.Tenant
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyReceivalWasteStreamQuery
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ReceivalDeclarationIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

/**
 * Adapter implementation for querying waste streams that need monthly receival declarations.
 *
 * Queries waste streams based on:
 * - Weight tickets received in the given month (via weight_ticket_lines and weight_tickets)
 * - Only waste streams with processor_id of current tenant
 * - Declaration history to identify waste streams that have been declared in PREVIOUS periods only
 *   (excludes current period to ensure job execution order independence)
 * - Aggregation of weight from weight_ticket_lines and shipment counts from weight_tickets
 * - Transporter information from carrier companies
 */
@Repository
class MonthlyReceivalWasteStreamQueryAdapter(
  private val entityManager: EntityManager,
  private val idGenerator: ReceivalDeclarationIdGenerator
) : MonthlyReceivalWasteStreamQuery {

  private val logger = LoggerFactory.getLogger(MonthlyReceivalWasteStreamQueryAdapter::class.java)

  @Transactional
  override fun findMonthlyReceivalDeclarations(yearMonth: YearMonth): List<MonthlyReceivalDeclaration> {
    logger.info("Finding monthly receival declarations for yearMonth={}", yearMonth)

    // Calculate month boundaries in display timezone (Europe/Amsterdam) for business logic
    // This ensures weight tickets entered on the last day of a month in CET are not
    // incorrectly included in the next month's declarations
    val (startOfMonth, endOfMonth) = yearMonth.toDisplayTimezoneBoundaries()

    // Calculate the current period string (MMYYYY format)
    val currentPeriod = "${yearMonth.month.number.toString().padStart(2, '0')}${yearMonth.year}"

    val query = """
      SELECT
        ws.number,
        COALESCE(SUM(wtl.weight_value), 0) as total_weight,
        COUNT(DISTINCT wt.id) as total_shipments,
        ARRAY_AGG(DISTINCT c.vihb_id) FILTER (WHERE c.vihb_id IS NOT NULL) as transporters,
        ARRAY_AGG(DISTINCT wt.id) FILTER (WHERE wt.id IS NOT NULL) as weight_ticket_ids
      FROM waste_streams ws
      JOIN companies proc ON ws.processor_party_id = proc.processor_id
      LEFT JOIN weight_ticket_lines wtl ON wtl.waste_stream_number = ws.number
        AND (wtl.declared_weight IS NULL OR wtl.declared_weight != wtl.weight_value)
      LEFT JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
        AND wt.weighted_at >= :startOfMonth
        AND wt.weighted_at < :endOfMonth
        AND wt.status IN ('${WeightTicketStatus.COMPLETED}', '${WeightTicketStatus.INVOICED}')
      LEFT JOIN companies c ON c.id = wt.carrier_party_id
      WHERE proc.processor_id = '${Tenant.processorPartyId.number}'
        AND wt.id IS NOT NULL
        AND EXISTS (
          SELECT 1 FROM lma_declarations d
          WHERE d.waste_stream_number = ws.number
            AND d.period <= :currentPeriod
        )
      GROUP BY ws.number
      HAVING COUNT(DISTINCT wt.id) > 0
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val results = entityManager.createNativeQuery(query, MonthlyReceivalWasteStreamQueryResult::class.java)
      .setParameter("startOfMonth", startOfMonth)
      .setParameter("endOfMonth", endOfMonth)
      .setParameter("currentPeriod", currentPeriod)
      .resultList as List<MonthlyReceivalWasteStreamQueryResult>

    logger.info("Found {} waste streams for monthly receival declarations", results.size)

    return results.map { result ->
      val id = idGenerator.nextId()
      val totalWeight = result.totalWeight.toInt()
      val totalShipments = result.totalShipments.toShort()
      val transporters = result.transporters?.toList() ?: emptyList()
      val weightTicketIds = result.weightTicketIds?.toList() ?: emptyList()

      MonthlyReceivalDeclaration(
        id = id,
        wasteStreamNumber = WasteStreamNumber(result.number),
        transporters = transporters,
        totalWeight = totalWeight,
        totalShipments = totalShipments,
        yearMonth = yearMonth,
        weightTicketIds = weightTicketIds
      )
    }
  }
}

/**
 * Result of the monthly receival waste stream query.
 * Represents a row from the native SQL query with aggregated metrics.
 */
data class MonthlyReceivalWasteStreamQueryResult(
  val number: String,
  val totalWeight: BigDecimal,
  val totalShipments: Long,
  val transporters: Array<String>?,
  val weightTicketIds: Array<Long>?
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as MonthlyReceivalWasteStreamQueryResult

    if (number != other.number) return false
    if (totalWeight != other.totalWeight) return false
    if (totalShipments != other.totalShipments) return false
    if (transporters != null) {
      if (other.transporters == null) return false
      if (!transporters.contentEquals(other.transporters)) return false
    } else if (other.transporters != null) return false

    return true
  }

  override fun hashCode(): Int {
    var result = number.hashCode()
    result = 31 * result + totalWeight.hashCode()
    result = 31 * result + totalShipments.hashCode()
    result = 31 * result + (transporters?.contentHashCode() ?: 0)
    return result
  }
}
