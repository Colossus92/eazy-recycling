package nl.eazysoftware.eazyrecyclingservice.repository.outbox

import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutbox
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionOutboxId
import org.springframework.stereotype.Component
import java.time.Instant
import kotlin.time.Instant as KotlinInstant

@Component
class EdgeFunctionOutboxMapper {

    fun toDomain(dto: EdgeFunctionOutboxDto): EdgeFunctionOutbox {
        return EdgeFunctionOutbox(
            id = EdgeFunctionOutboxId(dto.id),
            functionName = dto.functionName,
            httpMethod = dto.httpMethod,
            payload = dto.payload,
            status = dto.status,
            attempts = dto.attempts,
            lastAttemptAt = dto.lastAttemptAt?.let { KotlinInstant.fromEpochMilliseconds(it.toEpochMilli()) },
            errorMessage = dto.errorMessage,
            processedAt = dto.processedAt?.let { KotlinInstant.fromEpochMilliseconds(it.toEpochMilli()) },
            createdAt = KotlinInstant.fromEpochMilliseconds(dto.createdAt.toEpochMilli()),
            aggregateType = dto.aggregateType,
            aggregateId = dto.aggregateId,
        )
    }

    fun toDto(domain: EdgeFunctionOutbox): EdgeFunctionOutboxDto {
        return EdgeFunctionOutboxDto(
            id = domain.id.value,
            functionName = domain.functionName,
            httpMethod = domain.httpMethod,
            payload = domain.payload,
            status = domain.status,
            attempts = domain.attempts,
            lastAttemptAt = domain.lastAttemptAt?.let { Instant.ofEpochMilli(it.toEpochMilliseconds()) },
            errorMessage = domain.errorMessage,
            processedAt = domain.processedAt?.let { Instant.ofEpochMilli(it.toEpochMilliseconds()) },
            createdAt = Instant.ofEpochMilli(domain.createdAt.toEpochMilliseconds()),
            aggregateType = domain.aggregateType,
            aggregateId = domain.aggregateId,
        )
    }
}
