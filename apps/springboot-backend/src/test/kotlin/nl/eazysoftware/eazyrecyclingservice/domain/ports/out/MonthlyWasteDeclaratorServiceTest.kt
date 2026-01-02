package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import kotlinx.datetime.minusMonth
import nl.eazysoftware.eazyrecyclingservice.config.clock.toYearMonth
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob.JobType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.check
import org.mockito.kotlin.verify
import kotlin.time.Clock

@ExtendWith(MockitoExtension::class)
class MonthlyWasteDeclaratorServiceTest {

  @Mock
  private lateinit var monthlyWasteDeclarationJobs: MonthlyWasteDeclarationJobs

  @InjectMocks
  private lateinit var service: MonthlyWasteDeclaratorService

  @Test
  fun `should schedule jobs for previous month`() {
    // Calculate expected previous month
    val now = Clock.System.now()
    val currentYearMonth = now.toYearMonth()
    val expectedPreviousMonth = currentYearMonth.minusMonth()

    // When
    service.declare()

    // Then - Verify all jobs passed to save
    verify(monthlyWasteDeclarationJobs).save(
      check { job ->
        assertEquals(JobType.FIRST_RECEIVALS, job.jobType)
        assertEquals(expectedPreviousMonth, job.yearMonth)
        assertEquals(Status.PENDING, job.status)
        assertNull(job.fulfilled)
      },
      check { job ->
        assertEquals(JobType.MONTHLY_RECEIVALS, job.jobType)
        assertEquals(expectedPreviousMonth, job.yearMonth)
        assertEquals(Status.PENDING, job.status)
        assertNull(job.fulfilled)
      }
    )
  }

  @Test
  fun `should create both FIRST_RECEIVALS and MONTHLY_RECEIVALS jobs`() {
    // When
    service.declare()

    // Then
    val jobTypes = mutableSetOf<JobType>()
    verify(monthlyWasteDeclarationJobs).save(
      check { job -> jobTypes.add(job.jobType) },
      check { job -> jobTypes.add(job.jobType) }
    )

    assertEquals(setOf(JobType.FIRST_RECEIVALS, JobType.MONTHLY_RECEIVALS), jobTypes)
  }

  @Test
  fun `should set status to PENDING for new jobs`() {
    // When
    service.declare()

    // Then
    verify(monthlyWasteDeclarationJobs).save(
      check { job -> assertEquals(Status.PENDING, job.status) },
      check { job -> assertEquals(Status.PENDING, job.status) }
    )
  }

  @Test
  fun `should set fulfilled to null for new jobs`() {
    // When
    service.declare()

    // Then
    verify(monthlyWasteDeclarationJobs).save(
      check { job -> assertNull(job.fulfilled) },
      check { job -> assertNull(job.fulfilled) }
    )
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
      check { job ->
        // Verify created timestamp is between before and after the call
        assert(job.created >= beforeCall && job.created <= afterCall) {
          "Expected created timestamp to be between $beforeCall and $afterCall, but was ${job.created}"
        }
      },
      check { job ->
        // Verify created timestamp is between before and after the call
        assert(job.created >= beforeCall && job.created <= afterCall) {
          "Expected created timestamp to be between $beforeCall and $afterCall, but was ${job.created}"
        }
      }
    )
  }
}
