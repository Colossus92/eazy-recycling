package nl.eazysoftware.eazyrecyclingservice.repository.jobs

import jakarta.persistence.*
import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJobs
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.*
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Repository
interface MonthlyWasteDeclarationJobsJpaRepository : JpaRepository<MonthlyWasteDeclarationJobDto, UUID> {
  fun findByStatus(status: MonthlyWasteDeclarationJob.Status): List<MonthlyWasteDeclarationJobDto>
}

@Repository
class MonthlyWasteDeclarationJobsRepository(
  private val jpaRepository: MonthlyWasteDeclarationJobsJpaRepository,
) : MonthlyWasteDeclarationJobs {
  override fun save(vararg jobs: MonthlyWasteDeclarationJob) {
    jpaRepository.saveAll(jobs.map { it.toEntity() })
  }

  override fun findPending(): List<MonthlyWasteDeclarationJob> =
    jpaRepository.findByStatus(MonthlyWasteDeclarationJob.Status.PENDING)
      .map { it.toDomain() }
}

@Entity
@Table(name = "monthly_waste_declaration_jobs")
data class MonthlyWasteDeclarationJobDto(
  @Id
  val id: UUID,
  @Enumerated(EnumType.STRING)
  val jobType: MonthlyWasteDeclarationJob.JobType,
  val yearMonth: YearMonth,
  @Enumerated(EnumType.STRING)
  val status: MonthlyWasteDeclarationJob.Status,
  val createdAt: Instant,
  val fulfilledAt: Instant?,

  ) {

  fun toDomain() = MonthlyWasteDeclarationJob(
    id = id,
    jobType = jobType,
    yearMonth = yearMonth,
    status = status,
    created = createdAt.toKotlinInstant(),
    fulfilled = fulfilledAt?.toKotlinInstant(),
  )
}

private fun MonthlyWasteDeclarationJob.toEntity() = MonthlyWasteDeclarationJobDto(
  id = id,
  jobType = jobType,
  yearMonth = yearMonth,
  status = status,
  createdAt = created.toJavaInstant(),
  fulfilledAt = fulfilled?.toJavaInstant(),
)
