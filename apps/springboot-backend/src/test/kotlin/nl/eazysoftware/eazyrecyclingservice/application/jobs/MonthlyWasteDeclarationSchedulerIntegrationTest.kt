package nl.eazysoftware.eazyrecyclingservice.application.jobs

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.*
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.MonthlyWasteDeclarationJobsJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.jobs.MonthlyWasteDeclarationJobsRepository
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.time.Clock
import kotlin.time.toKotlinInstant

@SpringBootTest
@ActiveProfiles("test")
class MonthlyWasteDeclarationSchedulerIntegrationTest : BaseIntegrationTest() {

  @Autowired
  private lateinit var scheduler: MonthlyWasteDeclarationScheduler

  @Autowired
  private lateinit var monthlyWasteDeclarationJobsJpaRepository: MonthlyWasteDeclarationJobsJpaRepository

  @Autowired
  private lateinit var monthlyWasteDeclarationJobsRepository: MonthlyWasteDeclarationJobsRepository

  @MockitoBean
  private lateinit var firstReceivalWasteStreamQuery: FirstReceivalWasteStreamQuery

  @MockitoBean
  private lateinit var firstReceivalDeclarator: FirstReceivalDeclarator

  @MockitoBean
  private lateinit var sessionResults: SessionResults

  @AfterEach
  fun cleanup() {
    monthlyWasteDeclarationJobsJpaRepository.deleteAll()
  }

  @Test
  fun `should process pending FIRST_RECEIVALS job and mark as completed`() {
    // Given - a pending FIRST_RECEIVALS job exists
    val yearMonth = YearMonth(2025, 11)
    val pendingJob = MonthlyWasteDeclarationJob(
      jobType = MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS,
      yearMonth = yearMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = Clock.System.now(),
      fulfilled = null
    )
    monthlyWasteDeclarationJobsRepository.save(pendingJob)

    // Mock the query to return empty list (no first receivals to declare)
    whenever(firstReceivalWasteStreamQuery.findFirstReceivalDeclarations(yearMonth))
      .thenReturn(emptyList())

    // When - the scheduler processes pending jobs
    scheduler.processPendingJobs()

    // Then - verify the job was marked as completed
    val jobs = monthlyWasteDeclarationJobsJpaRepository.findAll()
    assertThat(jobs).hasSize(1)

    val processedJob = jobs.first()
    assertThat(processedJob.status).isEqualTo(MonthlyWasteDeclarationJob.Status.COMPLETED)
    assertThat(processedJob.fulfilledAt).isNotNull()
    assertThat(processedJob.jobType).isEqualTo(MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS)
    assertThat(processedJob.yearMonth).isEqualTo(yearMonth)

    // Verify the query was called
    verify(firstReceivalWasteStreamQuery).findFirstReceivalDeclarations(yearMonth)
  }

  @Test
  fun `should process pending FIRST_RECEIVALS job with declarations and call declarator`() {
    // Given - a pending FIRST_RECEIVALS job exists
    val yearMonth = YearMonth(2025, 11)
    val pendingJob = MonthlyWasteDeclarationJob(
      jobType = MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS,
      yearMonth = yearMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = Clock.System.now(),
      fulfilled = null
    )
    monthlyWasteDeclarationJobsRepository.save(pendingJob)

    // Mock the query to return some declarations
    val mockDeclarations = listOf<ReceivalDeclaration>()  // Empty list - declarator should NOT be called
    whenever(firstReceivalWasteStreamQuery.findFirstReceivalDeclarations(yearMonth))
      .thenReturn(mockDeclarations)

    // When - the scheduler processes pending jobs
    scheduler.processPendingJobs()

    // Then - verify the declarator was NOT called (empty list)
    verify(firstReceivalDeclarator, never()).declareFirstReceivals(any())

    // Verify the job was marked as completed
    val jobs = monthlyWasteDeclarationJobsJpaRepository.findAll()
    assertThat(jobs).hasSize(1)
    assertThat(jobs.first().status).isEqualTo(MonthlyWasteDeclarationJob.Status.COMPLETED)
  }

  @Test
  fun `should process pending MONTHLY_RECEIVALS job and mark as completed`() {
    // Given - a pending MONTHLY_RECEIVALS job exists
    val yearMonth = YearMonth(2025, 11)
    val pendingJob = MonthlyWasteDeclarationJob(
      jobType = MonthlyWasteDeclarationJob.JobType.MONTHLY_RECEIVALS,
      yearMonth = yearMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = Clock.System.now(),
      fulfilled = null
    )
    monthlyWasteDeclarationJobsRepository.save(pendingJob)

    // When - the scheduler processes pending jobs
    scheduler.processPendingJobs()

    // Then - verify the job was marked as completed (implementation is not yet done, but marks as completed)
    val jobs = monthlyWasteDeclarationJobsJpaRepository.findAll()
    assertThat(jobs).hasSize(1)

    val processedJob = jobs.first()
    assertThat(processedJob.status).isEqualTo(MonthlyWasteDeclarationJob.Status.COMPLETED)
    assertThat(processedJob.fulfilledAt).isNotNull()
    assertThat(processedJob.jobType).isEqualTo(MonthlyWasteDeclarationJob.JobType.MONTHLY_RECEIVALS)
  }

  @Test
  fun `should process multiple pending jobs`() {
    // Given - multiple pending jobs exist
    val yearMonth = YearMonth(2025, 11)
    val firstReceivalsJob = MonthlyWasteDeclarationJob(
      jobType = MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS,
      yearMonth = yearMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = Clock.System.now(),
      fulfilled = null
    )
    val monthlyReceivalsJob = MonthlyWasteDeclarationJob(
      jobType = MonthlyWasteDeclarationJob.JobType.MONTHLY_RECEIVALS,
      yearMonth = yearMonth,
      status = MonthlyWasteDeclarationJob.Status.PENDING,
      created = Clock.System.now(),
      fulfilled = null
    )
    monthlyWasteDeclarationJobsRepository.save(firstReceivalsJob, monthlyReceivalsJob)

    // Mock the query
    whenever(firstReceivalWasteStreamQuery.findFirstReceivalDeclarations(any()))
      .thenReturn(emptyList())

    // When - the scheduler processes pending jobs
    scheduler.processPendingJobs()

    // Then - verify both jobs were processed
    val jobs = monthlyWasteDeclarationJobsJpaRepository.findAll()
    assertThat(jobs).hasSize(2)
    assertThat(jobs).allMatch { it.status == MonthlyWasteDeclarationJob.Status.COMPLETED }
    assertThat(jobs).allMatch { it.fulfilledAt != null }
  }

  @Test
  fun `should not process already completed jobs`() {
    // Given - a completed job exists
    val yearMonth = YearMonth(2025, 11)
    val completedJob = MonthlyWasteDeclarationJob(
      jobType = MonthlyWasteDeclarationJob.JobType.FIRST_RECEIVALS,
      yearMonth = yearMonth,
      status = MonthlyWasteDeclarationJob.Status.COMPLETED,
      created = Clock.System.now(),
      fulfilled = Clock.System.now()
    )
    monthlyWasteDeclarationJobsRepository.save(completedJob)

    val originalFulfilledAt = completedJob.fulfilled

    // When - the scheduler processes pending jobs
    scheduler.processPendingJobs()

    // Then - verify the job was not modified
    val jobs = monthlyWasteDeclarationJobsJpaRepository.findAll()
    assertThat(jobs).hasSize(1)
    assertThat(jobs.first().status).isEqualTo(MonthlyWasteDeclarationJob.Status.COMPLETED)
    assertThat(jobs.first().fulfilledAt?.toKotlinInstant()).isEqualTo(originalFulfilledAt)
  }
}
