package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface WeightTicketJpaRepository : JpaRepository<WeightTicketDto, Int> {

    @Query(value = "SELECT nextval('weight_tickets_id_seq')", nativeQuery = true)
    fun getNextSequenceValue(): Long
}

@Repository
class WeightTicketRepository(
    private val jpaRepository: WeightTicketJpaRepository,
    private val mapper: WeightTicketMapper
) : WeightTickets {

    override fun nextId(): WeightTicketId {
        val nextValue = jpaRepository.getNextSequenceValue()
        return WeightTicketId(nextValue.toInt())
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
}
