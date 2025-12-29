package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import kotlinx.datetime.YearMonth
import kotlinx.datetime.number
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

interface WeightTicketJpaRepository : JpaRepository<WeightTicketDto, Long> {

    @Query(value = "SELECT nextval('weight_tickets_id_seq')", nativeQuery = true)
    fun getNextSequenceValue(): Long

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
        return WeightTicketId(nextValue)
    }

    override fun save(aggregate: WeightTicket): WeightTicket {
        val dto = mapper.toDto(aggregate)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun findById(id: WeightTicketId): WeightTicket? {
        return jpaRepository.findById(id.number)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun markLinesAsDeclared(
        wasteStreamNumber: WasteStreamNumber,
        weightTicketIds: List<Long>,
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
        val query = """
            SELECT
                wt.id as weight_ticket_id,
                row_number() OVER (PARTITION BY wt.id ORDER BY wtl.waste_stream_number) - 1 as line_index,
                wtl.waste_stream_number,
                wtl.weight_value,
                wt.weighted_at
            FROM weight_ticket_lines wtl
            JOIN weight_tickets wt ON wt.id = wtl.weight_ticket_id
            WHERE wt.status IN ('COMPLETED', 'INVOICED')
              AND wt.weighted_at < :cutoffDate
              AND (wtl.declared_weight IS NULL OR wtl.declared_weight != wtl.weight_value)
            ORDER BY wt.weighted_at, wt.id
        """.trimIndent()

        @Suppress("UNCHECKED_CAST")
        val results = entityManager.createNativeQuery(query)
            .setParameter("cutoffDate", java.time.YearMonth.of(cutoffDate.year, cutoffDate.month.number).atDay(1).atStartOfDay())
            .resultList as List<Array<Any>>

        return results.map { row ->
            UndeclaredWeightTicketLine(
                weightTicketId = WeightTicketId((row[0] as Number).toLong()),
                weightTicketLineIndex = (row[1] as Number).toInt(),
                wasteStreamNumber = WasteStreamNumber(row[2] as String),
                weightValue = row[3] as BigDecimal,
                weightedAt = (row[4] as java.time.Instant).toKotlinInstant(),
            )
        }
    }

    override fun nullifyLinkedInvoiceId(invoiceId: UUID): Int {
        return jpaRepository.nullifyLinkedInvoiceId(invoiceId)
    }
}
