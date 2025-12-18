package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutbox
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutboxId
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.OutboxStatus

interface EdgeFunctionOutboxRepository {
    fun nextId(): EdgeFunctionOutboxId
    fun save(entry: EdgeFunctionOutbox): EdgeFunctionOutbox
    fun findById(id: EdgeFunctionOutboxId): EdgeFunctionOutbox?
    fun findByStatus(status: OutboxStatus, limit: Int = 100): List<EdgeFunctionOutbox>
    fun findPendingEntries(limit: Int = 100): List<EdgeFunctionOutbox>
}
