package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.YearMonth
import kotlinx.datetime.minusMonth
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob.JobType
import org.springframework.stereotype.Service
import java.util.*
import kotlin.time.Clock
import kotlin.time.Instant

interface MonthlyWasteDeclarator {

  fun declare()
}

@Service
class MonthlyWasteDeclaratorService(
  private val monthlyWasteDeclarationJobs: MonthlyWasteDeclarationJobs
): MonthlyWasteDeclarator {

  /**
   * Declares the monthly waste declaration for the current month.
   * This method triggers a job that will save the monthly jobs in the outbox.
   */
  override fun declare() {
    val now = Clock.System.now()
    val previousMonth = now.toYearMonth().minusMonth()
    val firstReceivalJob = MonthlyWasteDeclarationJob(
      jobType = JobType.FIRST_RECEIVALS,
      yearMonth = previousMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = now,
      fulfilled = null
    )

    val monthlyReceivalJob = MonthlyWasteDeclarationJob(
      jobType = JobType.MONTHLY_RECEIVALS,
      yearMonth = previousMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = now,
      fulfilled = null
    )

    monthlyWasteDeclarationJobs.save(firstReceivalJob, monthlyReceivalJob)
  }
}



data class MonthlyWasteDeclarationJob(
  val id: UUID = UUID.randomUUID(),
  val jobType: JobType,
  val yearMonth: YearMonth,
  val status: Status,
  val created: Instant,
  val fulfilled: Instant?,
) {

  enum class Status {
    PENDING,
    COMPLETED,
    FAILED
  }

  fun markCompleted(): MonthlyWasteDeclarationJob {
    return copy(status = Status.COMPLETED, fulfilled = Clock.System.now())
  }

  fun markFailed(): MonthlyWasteDeclarationJob {
    return copy(status = Status.FAILED, fulfilled = Clock.System.now())
  }

  enum class JobType {
    FIRST_RECEIVALS,
    MONTHLY_RECEIVALS,
    LATE_WEIGHT_TICKETS,
  }
}
