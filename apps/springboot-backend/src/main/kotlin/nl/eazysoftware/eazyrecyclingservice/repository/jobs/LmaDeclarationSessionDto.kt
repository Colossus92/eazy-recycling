package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import jakarta.persistence.*
import java.time.Instant
import java.util.*
import kotlin.time.Clock
import kotlin.time.toJavaInstant

@Entity
@Table(name = "lma_declaration_sessions")
data class LmaDeclarationSessionDto(
  @Id
  @Column(name = "id")
  val id: UUID,

  @Column(name = "type", nullable = false)
  @Enumerated(EnumType.STRING)
  val type: Type,

  @Column(name = "declaration_ids", nullable = false, columnDefinition = "text[]")
  val declarationIds: List<String>,

  @Column(name = "status", nullable = false)
  @Enumerated(EnumType.STRING)
  val status: Status,

  @Column(name = "created_at", nullable = false)
  val createdAt: Instant,

  @Column(name = "processed_at", nullable = true)
  val processedAt: Instant? = null,

  @Column(name = "errors", nullable = true, columnDefinition = "text[]")
  val errors: List<String>? = null
) {
  enum class Type {
    FIRST_RECEIVAL,
    MONTHLY_RECEIVAL
  }

  enum class Status {
    PENDING,
    COMPLETED,
    FAILED
  }

  fun markCompleted(): LmaDeclarationSessionDto {
    return this.copy(
      status = Status.COMPLETED,
      processedAt = Clock.System.now().toJavaInstant()
    )
  }

  fun markFailed(errors: List<String>): LmaDeclarationSessionDto {
    return this.copy(
      status = Status.FAILED,
      processedAt = Clock.System.now().toJavaInstant(),
      errors = errors
    )
  }
}

