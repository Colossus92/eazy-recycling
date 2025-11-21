package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastedeclaration.FirstReceivalDeclaration
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.FirstReceivalWasteStreamQuery
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.ReceivalDeclarationFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

/**
 * Adapter implementation for querying waste streams that need to be declared for the first time.
 *
 * Queries waste streams based on:
 * - Weight tickets received in the given month (via weight_ticket_lines and weight_tickets)
 * - Only waste streams with processor_id '08797' (current tenant)
 * - Declaration history to identify first-time declarations (no existing lma_declarations)
 * - Aggregation of weight from weight_ticket_lines and shipment counts from weight_tickets
 * - Transporter information from carrier companies
 */
@Repository
class FirstReceivalWasteStreamQueryAdapter(
  private val entityManager: EntityManager,
  private val wasteStreamMapper: WasteStreamMapper,
  private val companyRepository: CompanyJpaRepository,
  private val receivalDeclarationFactory: ReceivalDeclarationFactory,
) : FirstReceivalWasteStreamQuery {

  private val logger = LoggerFactory.getLogger(FirstReceivalWasteStreamQueryAdapter::class.java)

  @Transactional
  override fun findFirstReceivalDeclarations(yearMonth: YearMonth): List<FirstReceivalDeclaration> {
    logger.info("Finding first receival declarations for yearMonth={}", yearMonth)

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
        ws.name,
        ws.eural_code,
        ws.processing_method_code,
        ws.waste_collection_type,
        ws.pickup_location_id,
        ws.consignor_party_id,
        ws.pickup_party_id,
        ws.dealer_party_id,
        ws.collector_party_id,
        ws.broker_party_id,
        ws.processor_party_id,
        ws.status,
        ws.last_activity_at,
        ws.consignor_classification,
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
      LEFT JOIN lma_declarations d ON d.waste_stream_number = ws.number
      WHERE proc.processor_id = '08797'
        AND d.id IS NULL
        AND wt.id IS NOT NULL
      GROUP BY ws.number, ws.name, ws.eural_code, ws.processing_method_code,
               ws.waste_collection_type, ws.pickup_location_id, ws.consignor_party_id,
               ws.pickup_party_id, ws.dealer_party_id, ws.collector_party_id,
               ws.broker_party_id, ws.processor_party_id, ws.status,
               ws.last_activity_at, ws.consignor_classification
      HAVING COUNT(DISTINCT wt.id) > 0
    """.trimIndent()

    @Suppress("UNCHECKED_CAST")
    val results = entityManager.createNativeQuery(query, FirstReceivalWasteStreamQueryResult::class.java)
      .setParameter("startOfMonth", startOfMonth)
      .setParameter("endOfMonth", endOfMonth)
      .resultList as List<FirstReceivalWasteStreamQueryResult>

    logger.info("Found {} waste streams for first receival declarations", results.size)

    return results.map { result ->
      // Map the waste stream DTO
      val wasteStreamDto = WasteStreamDto(
        number = result.number,
        name = result.name,
        euralCode = entityManager.getReference(
          nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural::class.java,
          result.euralCode
        ),
        processingMethodCode = entityManager.getReference(
          ProcessingMethodDto::class.java,
          result.processingMethodCode
        ),
        wasteCollectionType = result.wasteCollectionType,
        pickupLocation = entityManager.getReference(
          PickupLocationDto::class.java,
          result.pickupLocationId
        ),
        consignorParty = entityManager.getReference(
          CompanyDto::class.java,
          result.consignorPartyId
        ),
        pickupParty = entityManager.getReference(
          CompanyDto::class.java,
          result.pickupPartyId
        ),
        dealerParty = result.dealerPartyId?.let {
          entityManager.getReference(
            CompanyDto::class.java,
            it
          )
        },
        collectorParty = result.collectorPartyId?.let {
          entityManager.getReference(
            CompanyDto::class.java,
            it
          )
        },
        brokerParty = result.brokerPartyId?.let {
          entityManager.getReference(
            CompanyDto::class.java,
            it
          )
        },
        processorParty = companyRepository.findByProcessorIdAndDeletedAtIsNull(result.processorPartyId)
          ?: throw IllegalArgumentException("Processor company not found for processor_id: ${result.processorPartyId}"),
        status = result.status,
        lastActivityAt = result.lastActivityAt,
        consignorClassification = result.consignorClassification
      )

      val wasteStream = wasteStreamMapper.toDomain(wasteStreamDto)
      val totalWeight = result.totalWeight.toInt()
      val totalShipments = result.totalShipments.toShort()
      val transporters = result.transporters?.toList() ?: emptyList()

      receivalDeclarationFactory.create(
        wasteStream = wasteStream,
        transporters = transporters,
        totalWeight = totalWeight,
        totalShipments = totalShipments,
        yearMonth = yearMonth
      )
    }
  }
}

/**
 * Result of the first receival waste stream query.
 * Represents a row from the native SQL query with all waste stream details and aggregated metrics.
 */
data class FirstReceivalWasteStreamQueryResult(
  val number: String,
  val name: String,
  val euralCode: String,
  val processingMethodCode: String,
  val wasteCollectionType: String,
  val pickupLocationId: String?,
  val consignorPartyId: UUID,
  val pickupPartyId: UUID,
  val dealerPartyId: UUID?,
  val collectorPartyId: UUID?,
  val brokerPartyId: UUID?,
  val processorPartyId: String,
  val status: String,
  val lastActivityAt: Instant,
  val consignorClassification: Int,
  val totalWeight: BigDecimal,
  val totalShipments: Long,
  val transporters: Array<String>?
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as FirstReceivalWasteStreamQueryResult

    if (number != other.number) return false
    if (name != other.name) return false
    if (euralCode != other.euralCode) return false
    if (processingMethodCode != other.processingMethodCode) return false
    if (wasteCollectionType != other.wasteCollectionType) return false
    if (pickupLocationId != other.pickupLocationId) return false
    if (consignorPartyId != other.consignorPartyId) return false
    if (pickupPartyId != other.pickupPartyId) return false
    if (dealerPartyId != other.dealerPartyId) return false
    if (collectorPartyId != other.collectorPartyId) return false
    if (brokerPartyId != other.brokerPartyId) return false
    if (processorPartyId != other.processorPartyId) return false
    if (status != other.status) return false
    if (lastActivityAt != other.lastActivityAt) return false
    if (consignorClassification != other.consignorClassification) return false
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
    result = 31 * result + name.hashCode()
    result = 31 * result + euralCode.hashCode()
    result = 31 * result + processingMethodCode.hashCode()
    result = 31 * result + wasteCollectionType.hashCode()
    result = 31 * result + (pickupLocationId?.hashCode() ?: 0)
    result = 31 * result + consignorPartyId.hashCode()
    result = 31 * result + pickupPartyId.hashCode()
    result = 31 * result + (dealerPartyId?.hashCode() ?: 0)
    result = 31 * result + (collectorPartyId?.hashCode() ?: 0)
    result = 31 * result + (brokerPartyId?.hashCode() ?: 0)
    result = 31 * result + processorPartyId.hashCode()
    result = 31 * result + status.hashCode()
    result = 31 * result + lastActivityAt.hashCode()
    result = 31 * result + consignorClassification
    result = 31 * result + totalWeight.hashCode()
    result = 31 * result + totalShipments.hashCode()
    result = 31 * result + (transporters?.contentHashCode() ?: 0)
    return result
  }
}
