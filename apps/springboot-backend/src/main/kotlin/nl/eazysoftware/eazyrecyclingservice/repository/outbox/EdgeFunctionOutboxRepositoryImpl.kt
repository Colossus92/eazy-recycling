package nl.eazysoftware.eazyrecyclingservice.repository.outbox

import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutbox
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutboxId
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.OutboxStatus
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.EdgeFunctionOutboxRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

interface EdgeFunctionOutboxJpaRepository : JpaRepository<EdgeFunctionOutboxDto, Long> {

    @Query(value = "SELECT nextval('edge_function_outbox_id_seq')", nativeQuery = true)
    fun getNextSequenceValue(): Long

    fun findByStatus(status: OutboxStatus, pageable: PageRequest): List<EdgeFunctionOutboxDto>

    @Query("SELECT e FROM EdgeFunctionOutboxDto e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    fun findPendingEntries(pageable: PageRequest): List<EdgeFunctionOutboxDto>
}

@Repository
class EdgeFunctionOutboxRepositoryImpl(
    private val jpaRepository: EdgeFunctionOutboxJpaRepository,
    private val mapper: EdgeFunctionOutboxMapper,
) : EdgeFunctionOutboxRepository {

    override fun nextId(): EdgeFunctionOutboxId {
        val nextValue = jpaRepository.getNextSequenceValue()
        return EdgeFunctionOutboxId(nextValue)
    }

    override fun save(entry: EdgeFunctionOutbox): EdgeFunctionOutbox {
        val dto = mapper.toDto(entry)
        val saved = jpaRepository.save(dto)
        return mapper.toDomain(saved)
    }

    override fun findById(id: EdgeFunctionOutboxId): EdgeFunctionOutbox? {
        return jpaRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElse(null)
    }

    override fun findByStatus(status: OutboxStatus, limit: Int): List<EdgeFunctionOutbox> {
        return jpaRepository.findByStatus(status, PageRequest.of(0, limit))
            .map { mapper.toDomain(it) }
    }

    override fun findPendingEntries(limit: Int): List<EdgeFunctionOutbox> {
        return jpaRepository.findPendingEntries(PageRequest.of(0, limit))
            .map { mapper.toDomain(it) }
    }
}
