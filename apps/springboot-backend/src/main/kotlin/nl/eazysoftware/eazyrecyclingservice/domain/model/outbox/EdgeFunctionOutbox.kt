package nl.eazysoftware.eazyrecyclingservice.domain.model.outbox

import kotlin.time.Clock
import kotlin.time.Instant

class EdgeFunctionOutbox(
    val id: EdgeFunctionOutboxId,
    val functionName: EdgeFunctionName,
    val httpMethod: HttpMethod,
    val payload: String?,
    var status: OutboxStatus = OutboxStatus.PENDING,
    var attempts: Int = 0,
    var lastAttemptAt: Instant? = null,
    var errorMessage: String? = null,
    var processedAt: Instant? = null,
    val createdAt: Instant = Clock.System.now(),
    val aggregateType: String?,
    val aggregateId: String?,
) {
    fun markAsProcessing() {
        this.status = OutboxStatus.PROCESSING
        this.attempts += 1
        this.lastAttemptAt = Clock.System.now()
    }

    fun markAsCompleted() {
        this.status = OutboxStatus.COMPLETED
        this.processedAt = Clock.System.now()
        this.errorMessage = null
    }

    fun markAsFailed(errorMessage: String) {
        this.status = if (attempts >= MAX_ATTEMPTS) OutboxStatus.FAILED else OutboxStatus.PENDING
        this.errorMessage = errorMessage
    }

    companion object {
        const val MAX_ATTEMPTS = 3
    }
}

@JvmInline
value class EdgeFunctionOutboxId(val value: Long)

enum class EdgeFunctionName {
    INVOICE_PDF_GENERATOR,
}

enum class HttpMethod {
    GET,
    POST,
    PUT,
    DELETE,
}

enum class OutboxStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
}
