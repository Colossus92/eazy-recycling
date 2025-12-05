package nl.eazysoftware.eazyrecyclingservice.application.jobs

import kotlinx.datetime.YearMonth
import nl.eazysoftware.eazyrecyclingservice.application.util.DeclarationCutoffDateCalculator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJob
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MonthlyWasteDeclarationJobs
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTicketDeclarationSnapshots
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.toKotlinInstant

class LateDeclarationSchedulerIntegrationTest {

  private lateinit var monthlyWasteDeclarationJobs: MonthlyWasteDeclarationJobs
  private lateinit var weightTicketDeclarationSnapshots: WeightTicketDeclarationSnapshots
  private lateinit var scheduler: LateAndCorrectiveDeclarationScheduler

  @BeforeEach
  fun setUp() {
    monthlyWasteDeclarationJobs = mock()
    weightTicketDeclarationSnapshots = mock()
    scheduler = LateAndCorrectiveDeclarationScheduler(
      monthlyWasteDeclarationJobs,
      weightTicketDeclarationSnapshots
    )
  }

  @Test
  fun `calculateDeclarationCutoffDate returns previous month when day is before 20th`() {
    // Given: December 4th, 2025
    val december4th = ZonedDateTime.of(2025, 12, 4, 10, 0, 0, 0, ZoneId.of("Europe/Amsterdam"))
      .toInstant()
      .toKotlinInstant()

    // When
    val cutoffDate = DeclarationCutoffDateCalculator.calculateDeclarationCutoffDate(december4th)

    // Then: Cutoff should be November 2025 (so October and earlier are included)
    assertEquals(YearMonth(2025, 11), cutoffDate)
  }

  @Test
  fun `calculateDeclarationCutoffDate returns current month when day is on 20th`() {
    // Given: December 20th, 2025
    val december20th = ZonedDateTime.of(2025, 12, 20, 10, 0, 0, 0, ZoneId.of("Europe/Amsterdam"))
      .toInstant()
      .toKotlinInstant()

    // When
    val cutoffDate = DeclarationCutoffDateCalculator.calculateDeclarationCutoffDate(december20th)

    // Then: Cutoff should be December 2025 (so November and earlier are included)
    assertEquals(YearMonth(2025, 12), cutoffDate)
  }

  @Test
  fun `calculateDeclarationCutoffDate returns current month when day is after 20th`() {
    // Given: December 21st, 2025
    val december21st = ZonedDateTime.of(2025, 12, 21, 10, 0, 0, 0, ZoneId.of("Europe/Amsterdam"))
      .toInstant()
      .toKotlinInstant()

    // When
    val cutoffDate = DeclarationCutoffDateCalculator.calculateDeclarationCutoffDate(december21st)

    // Then: Cutoff should be December 2025 (so November and earlier are included)
    assertEquals(YearMonth(2025, 12), cutoffDate)
  }

  @Test
  fun `calculateDeclarationCutoffDate handles January correctly`() {
    // Given: January 4th, 2026
    val january4th = ZonedDateTime.of(2026, 1, 4, 10, 0, 0, 0, ZoneId.of("Europe/Amsterdam"))
      .toInstant()
      .toKotlinInstant()

    // When
    val cutoffDate = DeclarationCutoffDateCalculator.calculateDeclarationCutoffDate(january4th)

    // Then: Cutoff should be December 2025 (so November and earlier are included)
    assertEquals(YearMonth(2025, 12), cutoffDate)
  }

  @Test
  fun `triggerLateDeclarations creates late declaration jobs when undeclared lines exist`() {
    // Given
    whenever(weightTicketDeclarationSnapshots.findUndeclaredLines(any()))
      .thenReturn(listOf(mock()))

    // When
    scheduler.triggerLateDeclarations()

    // Then: Should save late declaration jobs
    verify(monthlyWasteDeclarationJobs).save(
      argThat { jobType == MonthlyWasteDeclarationJob.JobType.LATE_WEIGHT_TICKETS },
    )
  }

  @Test
  fun `triggerLateDeclarations does not create jobs when no work needed`() {
    // Given
    whenever(weightTicketDeclarationSnapshots.findUndeclaredLines(any()))
      .thenReturn(emptyList())

    // When
    scheduler.triggerLateDeclarations()

    // Then: Should not save any jobs
    verify(monthlyWasteDeclarationJobs, never()).save(any())
  }
}
