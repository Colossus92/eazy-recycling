package nl.eazysoftware.eazyrecyclingservice.repository.outbox

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.EdgeFunctionName
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.HttpMethod
import nl.eazysoftware.eazyrecyclingservice.domain.model.outbox.OutboxStatus
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "edge_function_outbox")
class EdgeFunctionOutboxDto(
    @Id
    @Column(name = "id")
    val id: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "function_name", nullable = false)
    val functionName: EdgeFunctionName,

    @Enumerated(EnumType.STRING)
    @Column(name = "http_method", nullable = false)
    val httpMethod: HttpMethod,

    @Column(name = "payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    val payload: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OutboxStatus,

    @Column(name = "attempts", nullable = false)
    var attempts: Int = 0,

    @Column(name = "last_attempt_at")
    var lastAttemptAt: Instant? = null,

    @Column(name = "error_message", columnDefinition = "TEXT")
    var errorMessage: String? = null,

    @Column(name = "processed_at")
    var processedAt: Instant? = null,

    @Column(name = "created_at", nullable = false)
    val createdAt: Instant,

    @Column(name = "aggregate_type")
    val aggregateType: String?,

    @Column(name = "aggregate_id")
    val aggregateId: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EdgeFunctionOutboxDto) return false
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
