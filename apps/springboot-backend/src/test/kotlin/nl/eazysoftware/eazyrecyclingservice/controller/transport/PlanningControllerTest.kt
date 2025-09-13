package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.service.PlanningService
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test

@ExtendWith(
  MockitoExtension::class)
class PlanningControllerTest {

    @Mock
    private lateinit var planningService: PlanningService

    private lateinit var planningController: PlanningController

    @BeforeEach
    fun setUp() {
        planningController = PlanningController(planningService)
    }

    @Test
    fun `reorderTransports should deconstruct request body correctly`() {
        // Given
        val date = LocalDate.of(2023, 12, 15)
        val licensePlate = "AB-123-CD"
        val transportIds = listOf(UUID.randomUUID(), UUID.randomUUID())
        val reorderRequest = TransportReorderRequest(date, licensePlate, transportIds)
        val expectedPlanningView = PlanningView(emptyList(), emptyList())

        whenever(planningService.reorderTransports(date, licensePlate, transportIds))
            .thenReturn(expectedPlanningView)

        // When
        val result = planningController.reorderTransports(reorderRequest)

        // Then
        verify(planningService).reorderTransports(date, licensePlate, transportIds)
        assertEquals(expectedPlanningView, result)
    }
}
