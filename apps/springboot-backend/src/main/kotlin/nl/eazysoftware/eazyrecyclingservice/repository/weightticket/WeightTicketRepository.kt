package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayTimezoneBoundaries
import nl.eazysoftware.eazyrecyclingservice.domain.model.Tenant
import nl.eazysoftware.eazyrecyclingservice.domain.model.declaration.UndeclaredWeightTicketLine
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.*
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

interface WeightTicketJpaRepository : JpaRepository<WeightTicketDto, UUID> {

    @Query(value = "SELECT nextval('weight_tickets_id_seq')", nativeQuery = true)
    fun getNextSequenceValue(): Long

    fun findByNumber(number: Long): WeightTicketDto?

    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE WeightTicketDto wt SET wt.linkedInvoiceId = NULL, wt.status = 'COMPLETED' WHERE wt.linkedInvoiceId = :invoiceId")
    fun nullifyLinkedInvoiceId(invoiceId: UUID): Int
}

@Repository
class WeightTicketRepository(
    private val jpaRepository: WeightTicketJpaRepository,
    private val mapper: WeightTicketMapper,
    private val entityManager: EntityManager
) : WeightTickets {

    override fun nextId(): WeightTicketId {
        val nextValue = jpaRepository.getNextSequenceValue()
        return WeightTicketId(UUID.randomUUID(), nextValue)
    }

    override fun save(aggregate: WeightTicket): WeightTicket {
        val dto = mapper.toDto(aggregate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun findById(id: WeightTicketId): WeightTicket? {
        return jpaRepository.findById(id.id)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByNumber(number: Long): WeightTicket? {
        return jpaRepository.findByNumber(number)?.let { mapper.toDomain(it) }
    }

    override fun markLinesAsDeclared(
        wasteStreamNumber: WasteStreamNumber,
        weightTicketIds: List<UUID>,
        declaredAt: Instant
    ): Int {
        if (weightTicketIds.isEmpty()) return 0

        val query = """
            UPDATE weight_ticket_lines wtl
            SET declared_weight = wtl.weight_value,
                last_declared_at = :declaredAt
            WHERE wtl.weight_ticket_id IN :weightTicketIds
              AND wtl.waste_stream_number = :wasteStreamNumber
              AND (wtl.declared_weight IS NULL OR wtl.declared_weight != wtl.weight_value)
        """.trimIndent()

        return entityManager.createNativeQuery(query)
            .setParameter("declaredAt", declaredAt.toJavaInstant())
            .setParameter("weightTicketIds", weightTicketIds)
            .setParameter("wasteStreamNumber", wasteStreamNumber.number)
            .executeUpdate()
    }

    override fun findUndeclaredLines(cutoffDate: YearMonth): List<UndeclaredWeightTicketLine> {
        // Calculate month boundaries in display timezone (Europe/Amsterdam) for business logic
        // This ensures weight tickets entered on the last day of a month in CET are not
        // incorrectly included in the next month's declarations
        val (startOfMonth, _) = cutoffDate.toDisplayTimezoneBoundaries()

        val query = """
            SELECT
                wt.id as weight_ticket_id,
                wt.number as weight_ticket_number,
                row_number() OVER (PARTITION BY wt.id ORDER BY wtl.waste_stream_number) - 1 as line_index,
                wtl.waste_stream_number,
                wtl.weight_value,
                wt.weighted_at
            FROM weight_ticket_lines wtl
            JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
            JOIN waste_streams ws ON wtl.waste_stream_number = ws.number
            JOIN companies proc ON ws.processor_party_id = proc.processor_id
            WHERE proc.processor_id = '${Tenant.processorPartyId.number}'
              AND wt.status IN ('COMPLETED', 'INVOICED')
              AND wt.weighted_at < :cutoffDate
              AND (wtl.declared_weight IS NULL OR wtl.declared_weight != wtl.weight_value)
              AND wtl.waste_stream_number IS NOT NULL
            ORDER BY wt.weighted_at, wt.id
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val results = entityManager.createNativeQuery(query)
            .setParameter("cutoffDate", startOfMonth)
            .resultList as List<Array<Any>>

        return results.map { row ->
            UndeclaredWeightTicketLine(
                weightTicketId = WeightTicketId(row[0] as UUID, (row[1] as Number).toLong()),
                weightTicketLineIndex = (row[2] as Number).toInt(),
                wasteStreamNumber = WasteStreamNumber(row[3] as String),
                weightValue = row[4] as BigDecimal,
                weightedAt = (row[5] as java.time.Instant).toKotlinInstant(),
            )
        }
    }

    override fun nullifyLinkedInvoiceId(invoiceId: UUID): Int {
        return jpaRepository.nullifyLinkedInvoiceId(invoiceId)
    }
}
