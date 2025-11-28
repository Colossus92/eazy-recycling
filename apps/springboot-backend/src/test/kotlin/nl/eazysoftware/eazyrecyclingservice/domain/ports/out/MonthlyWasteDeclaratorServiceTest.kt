package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.minusMonth
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob.JobType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import kotlin.time.Clock

@ExtendWith(MockitoExtension::class)
class MonthlyWasteDeclaratorServiceTest {

  @Mock
  private lateinit var monthlyWasteDeclarationJobs: MonthlyWasteDeclarationJobs

  @InjectMocks
  private lateinit var service: MonthlyWasteDeclaratorService

  @Captor
  private lateinit var jobsCaptor: ArgumentCaptor<MonthlyWasteDeclarationJob>

  @Test
  fun `should schedule jobs for previous month`() {
    // When
    service.declare()

    // Then - Capture all jobs passed to save
    verify(monthlyWasteDeclarationJobs).save(
      jobsCaptor.capture(),
      jobsCaptor.capture()
    )

    val capturedJobs = jobsCaptor.allValues
    assertEquals(2, capturedJobs.size)

    // Calculate expected previous month
    val now = Clock.System.now()
    val currentYearMonth = now.toYearMonth()
    val expectedPreviousMonth = currentYearMonth.minusMonth()

    // Verify first receivals job
    val firstReceivalJob = capturedJobs.find { it.jobType == JobType.FIRST_RECEIVALS }!!
    assertEquals(expectedPreviousMonth, firstReceivalJob.yearMonth)
    assertEquals(Status.PENDING, firstReceivalJob.status)
    assertNull(firstReceivalJob.fulfilled)

    // Verify monthly receivals job
    val monthlyReceivalJob = capturedJobs.find { it.jobType == JobType.MONTHLY_RECEIVALS }!!
    assertEquals(expectedPreviousMonth, monthlyReceivalJob.yearMonth)
    assertEquals(Status.PENDING, monthlyReceivalJob.status)
    assertNull(monthlyReceivalJob.fulfilled)
  }

  @Test
  fun `should create both FIRST_RECEIVALS and MONTHLY_RECEIVALS jobs`() {
    // When
    service.declare()

    // Then
    verify(monthlyWasteDeclarationJobs).save(
      jobsCaptor.capture(),
      jobsCaptor.capture()
    )

    val capturedJobs = jobsCaptor.allValues
    val jobTypes = capturedJobs.map { it.jobType }.toSet()

    assertEquals(setOf(JobType.FIRST_RECEIVALS, JobType.MONTHLY_RECEIVALS), jobTypes)
  }

  @Test
  fun `should set status to PENDING for new jobs`() {
    // When
    service.declare()

    // Then
    verify(monthlyWasteDeclarationJobs).save(
      jobsCaptor.capture(),
      jobsCaptor.capture()
    )

    val capturedJobs = jobsCaptor.allValues
    capturedJobs.forEach { job ->
      assertEquals(Status.PENDING, job.status)
    }
  }

  @Test
  fun `should set fulfilled to null for new jobs`() {
    // When
    service.declare()

    // Then
    verify(monthlyWasteDeclarationJobs).save(
      jobsCaptor.capture(),
      jobsCaptor.capture()
    )

    val capturedJobs = jobsCaptor.allValues
    capturedJobs.forEach { job ->
      assertNull(job.fulfilled)
    }
  }

  @Test
  fun `should set created timestamp to current time`() {
    // Given
    val beforeCall = Clock.System.now()

    // When
    service.declare()

    // Then
    val afterCall = Clock.System.now()

    verify(monthlyWasteDeclarationJobs).save(
      jobsCaptor.capture(),
      jobsCaptor.capture()
    )

    val capturedJobs = jobsCaptor.allValues
    capturedJobs.forEach { job ->
      // Verify created timestamp is between before and after the call
      assert(job.created >= beforeCall && job.created <= afterCall) {
        "Expected created timestamp to be between $beforeCall and $afterCall, but was ${job.created}"
      }
    }
  }
}
