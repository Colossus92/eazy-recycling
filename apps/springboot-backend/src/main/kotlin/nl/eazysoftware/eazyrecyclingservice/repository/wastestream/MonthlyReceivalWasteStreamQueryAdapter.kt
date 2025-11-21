package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.MonthlyReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyReceivalWasteStreamQuery
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ReceivalDeclarationIdGenerator
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Adapter implementation for querying waste streams that need monthly receival declarations.
 *
 * Queries waste streams based on:
 * - Weight tickets received in the given month (via weight_ticket_lines and weight_tickets)
 * - Only waste streams with processor_id '08797' (current tenant)
 * - Declaration history to identify waste streams that have been declared before
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

    // Create proper OffsetDateTime objects for the month boundaries
    val startOfMonthDate = LocalDate.of(yearMonth.year, yearMonth.month.number, 1)
    val startOfMonth = OffsetDateTime.of(startOfMonthDate.atStartOfDay(), ZoneOffset.UTC)

    val endOfMonthDate = if (yearMonth.month.number == 12) {
      LocalDate.of(yearMonth.year + 1, 1, 1)
    } else {
      LocalDate.of(yearMonth.year, yearMonth.month.number + 1, 1)
    }
    val endOfMonth = OffsetDateTime.of(endOfMonthDate.atStartOfDay(), ZoneOffset.UTC)

    val query = """
      SELECT
        ws.number,
        COALESCE(SUM(wtl.weight_value), 0) as total_weight,
        COUNT(DISTINCT wt.id) as total_shipments,
        ARRAY_AGG(DISTINCT c.vihb_id) FILTER (WHERE c.vihb_id IS NOT NULL) as transporters
      FROM waste_streams ws
      JOIN companies proc ON ws.processor_party_id = proc.processor_id
      LEFT JOIN weight_ticket_lines wtl ON wtl.waste_stream_number = ws.number
      LEFT JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
        AND wt.weighted_at >= :startOfMonth
        AND wt.weighted_at < :endOfMonth
        AND wt.status IN ('COMPLETED', 'INVOICED')
      LEFT JOIN companies c ON c.id = wt.carrier_party_id
      WHERE proc.processor_id = '08797'
        AND wt.id IS NOT NULL
        AND EXISTS (
          SELECT 1 FROM lma_declarations d
          WHERE d.waste_stream_number = ws.number
        )
      GROUP BY ws.number
      HAVING COUNT(DISTINCT wt.id) > 0
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val results = entityManager.createNativeQuery(query, MonthlyReceivalWasteStreamQueryResult::class.java)
      .setParameter("startOfMonth", startOfMonth)
      .setParameter("endOfMonth", endOfMonth)
      .resultList as List<MonthlyReceivalWasteStreamQueryResult>

    logger.info("Found {} waste streams for monthly receival declarations", results.size)

    return results.map { result ->
      val id = idGenerator.nextId()
      val totalWeight = result.totalWeight.toInt()
      val totalShipments = result.totalShipments.toShort()
      val transporters = result.transporters?.toList() ?: emptyList()

      MonthlyReceivalDeclaration(
        id = id,
        wasteStreamNumber = WasteStreamNumber(result.number),
        transporters = transporters,
        totalWeight = totalWeight,
        totalShipments = totalShipments,
        yearMonth = yearMonth
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
  val transporters: Array<String>?
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
