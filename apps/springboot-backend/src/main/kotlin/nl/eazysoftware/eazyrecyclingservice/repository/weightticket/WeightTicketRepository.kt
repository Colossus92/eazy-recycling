package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import kotlin.time.Instant
import kotlin.time.toJavaInstant

interface WeightTicketJpaRepository : JpaRepository<WeightTicketDto, Long> {

    @Query(value = "SELECT nextval('weight_tickets_id_seq')", nativeQuery = true)
    fun getNextSequenceValue(): Long
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
}
