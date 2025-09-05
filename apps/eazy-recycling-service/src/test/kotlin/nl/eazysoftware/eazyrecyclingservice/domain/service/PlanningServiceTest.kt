package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.test.helpers.TransportDtoHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

@ExtendWith(MockitoExtension::class)
class PlanningServiceTest {

    @Mock
    private lateinit var transportRepository: TransportRepository

    @Mock
    private lateinit var truckService: TruckService

    @Mock
    private lateinit var entityManager: EntityManager

    private lateinit var planningService: PlanningService

    private val truck1 = Truck(licensePlate = "ABC-123", brand = "Volvo", model = "FH16")
    private val truck2 = Truck(licensePlate = "XYZ-789", brand = "Mercedes", model = "Actros")

    @BeforeEach
    fun setUp() {
        planningService = PlanningService(transportRepository, truckService, entityManager)
    }

    @Test
    fun `getPlanningByDate should return planning view with transports for the week`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20) // Tuesday
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)


        val transport1 = TransportDtoHelper.transport(truck = truck1, pickupDateTime = LocalDateTime.of(2025, 5, 20, 10, 0))
        val transport2 = TransportDtoHelper.transport(truck = truck1, pickupDateTime = LocalDateTime.of(2025, 5, 21, 11, 0))
        val transport3 = TransportDtoHelper.transport(truck = null, driver = null, pickupDateTime = LocalDateTime.of(2025, 5, 22, 9, 0),)

        val transports = listOf(transport1, transport2, transport3)
        val trucks = listOf(truck1, truck2)

        // When
        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(transports)

        whenever(truckService.getAllTrucks()).thenReturn(trucks)

        val result = planningService.getPlanningByDate(pickupDate)

        // Then
        assertThat(result).isNotNull
        assertThat(result.dates).hasSize(7)
        assertThat(result.dates[0]).isEqualTo(mondayOfWeek.toString())
        assertThat(result.dates[6]).isEqualTo(sundayOfWeek.toString())

        // Should have 3 trucks: ABC-123, XYZ-789, and "Niet toegewezen"
        assertThat(result.transports).hasSize(3)

        // Check that transports are grouped correctly by truck
        val assignedTruckTransports = result.transports.find { it.truck == truck1.getDisplayName() }
        assertThat(assignedTruckTransports).isNotNull
        assertThat(assignedTruckTransports!!.transports).hasSize(2) // Two dates with transports

        // Check that unassigned transports are in the "Niet toegewezen" group
        val unassignedTransports = result.transports.find { it.truck == "Niet toegewezen" }
        assertThat(unassignedTransports).isNotNull
        assertThat(unassignedTransports!!.transports).hasSize(1) // One date with transports

        // Check that empty trucks are included
        val emptyTruck = result.transports.find { it.truck == truck2.getDisplayName() }
        assertThat(emptyTruck).isNotNull
        assertThat(emptyTruck!!.transports).isEmpty()
    }

    @Test
    fun `getPlanningByDate should filter by truckId when provided`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20)
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)

        val transport1 = TransportDtoHelper.transport(truck = truck1)
        val transport2 = TransportDtoHelper.transport(truck = truck1)
        val transports = listOf(transport1, transport2)

        // When
        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(transports)

        val result = planningService.getPlanningByDate(pickupDate, truck1.licensePlate)

        // Then
        assertThat(result).isNotNull
        assertThat(result.transports).hasSize(1)

        // Check that only transports for the specified truck are included
        val filteredTransports = result.transports.find { it.truck == truck1.getDisplayName() }
        assertThat(filteredTransports).isNotNull
        assertThat(filteredTransports!!.transports).hasSize(1) // One date with transports
    }

    @Test
    fun `getPlanningByDate should filter by driverId when provided`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20)
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)

        val transport1 = TransportDtoHelper.transport(truck = truck1)
        val transport2 = TransportDtoHelper.transport(truck = truck1, driver = TransportDtoHelper.driver2)

        val transports = listOf(transport1, transport2)
        val trucks = listOf(truck1)

        // When
        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(transports)

        whenever(truckService.getAllTrucks()).thenReturn(trucks)

        val result = planningService.getPlanningByDate(pickupDate, driverId = TransportDtoHelper.driver1.id)

        // Then
        assertThat(result).isNotNull

        // Check that only transports for the specified driver are included
        val truckTransports = result.transports.find { it.truck == truck1.getDisplayName() }
        assertThat(truckTransports).isNotNull
        assertThat(truckTransports!!.transports).hasSize(1) // One date with transports
        assertThat(truckTransports.transports.values.flatten()).hasSize(1) // One transport

        // Verify the transport is the one with the specified driver
        val transportView = truckTransports.transports.values.flatten().first()
        assertThat(transportView.driver?.id).isEqualTo(TransportDtoHelper.driver1.id)
    }

    @Test
    fun `getPlanningByDate should filter by status when provided`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20)
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)
        val status = "PLANNED,FINISHED"


        // PLANNED transport (has truck and driver, and delivery time is in the future)
        val transport1 = TransportDtoHelper.transport(
            truck1,
            LocalDateTime.of(2025, 5, 20, 10, 0)
        )

        // UNPLANNED transport (no truck or driver assigned)
        val transport2 = TransportDtoHelper.transport(truck = null, driver = null)

        // FINISHED transport (has truck and driver, and delivery time is in the past)
        val transport3 = TransportDtoHelper.transport(truck = truck1)

        val transports = listOf(transport1, transport2, transport3)
        val trucks = listOf(truck1)

        // When
        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(transports)

        whenever(truckService.getAllTrucks()).thenReturn(trucks)

        val result = planningService.getPlanningByDate(pickupDate, status = status)

        // Then
        assertThat(result).isNotNull

        // Check that only transports with the specified statuses are included
        val allTransports = result.transports.flatMap { it.transports.values.flatten() }
        assertThat(allTransports).hasSize(2) // Only PLANNED and FINISHED transports

        // Verify no UNPLANNED transports are included
        val unplannedTransports = allTransports.filter { it.status == TransportDto.Status.UNPLANNED }
        assertThat(unplannedTransports).isEmpty()
    }

    @Test
    fun `createTransportsView should group transports by truck`() {
        val transport1 = TransportDtoHelper.transport(truck1, LocalDateTime.of(2025, 5, 20, 10, 0))
        val transport2 = TransportDtoHelper.transport(truck1, LocalDateTime.of(2025, 5, 20, 10, 0))
        val transport3 = TransportDtoHelper.transport(truck2, LocalDateTime.of(2025, 5, 21, 10, 0))
        val transport4 = TransportDtoHelper.transport(null, LocalDateTime.of(2025, 5, 22, 10, 0))

        val transports = listOf(transport1, transport2, transport3, transport4)

        // When
        val result = planningService.createTransportsView(transports)

        // Then
        assertThat(result).hasSize(3)

        // Check truck1 group
        val truck1Group = result.find { it.truck == truck1.getDisplayName() }
        assertThat(truck1Group).isNotNull
        assertThat(truck1Group!!.transports).hasSize(1) // One date
        assertThat(truck1Group.transports["2025-05-20"]).hasSize(2) // Two transports on this date

        // Check truck2  group
        val truck2Group = result.find { it.truck == truck2.getDisplayName() }
        assertThat(truck2Group).isNotNull
        assertThat(truck2Group!!.transports).hasSize(1) // One date
        assertThat(truck2Group.transports["2025-05-21"]).hasSize(1) // One transport on this date

        // Check unassigned group
        val unassignedGroup = result.find { it.truck == "Niet toegewezen" }
        assertThat(unassignedGroup).isNotNull
        assertThat(unassignedGroup!!.transports).hasSize(1) // One date
        assertThat(unassignedGroup.transports["2025-05-22"]).hasSize(1) // One transport on this date
    }

    @Test
    fun `getDaysInWeek should return all 7 days of the week`() {
        // Given
        val wednesday = LocalDate.of(2025, 5, 21) // A Wednesday
        val expectedMonday = LocalDate.of(2025, 5, 19)
        val expectedSunday = LocalDate.of(2025, 5, 25)

        // When
        // Using reflection to access the private method
        val getDaysInWeekMethod = PlanningService::class.java.getDeclaredMethod("getDaysInWeek", LocalDate::class.java)
        getDaysInWeekMethod.isAccessible = true

        @Suppress("UNCHECKED_CAST")
        val result = getDaysInWeekMethod.invoke(planningService, wednesday) as List<LocalDate>

        // Then
        assertThat(result).hasSize(7)
        assertThat(result[0]).isEqualTo(expectedMonday)
        assertThat(result[6]).isEqualTo(expectedSunday)

        // Check that all days are consecutive
        for (i in 0 until 6) {
            assertThat(result[i + 1]).isEqualTo(result[i].plusDays(1))
        }
    }

    @Test
    fun `reorderTransports should update sequence numbers and allow moving to different truck and date`() {
        // Given
        val newDate = LocalDate.of(2025, 5, 21)

        // Transports with original truck and date
        val transport1 = TransportDtoHelper.transport(truck1)
        val transport2 = TransportDtoHelper.transport(truck1)

        val transport3 = TransportDtoHelper.transport(truck1)
        val transports = listOf(transport1, transport2, transport3)

        // We'll move all transports to a new truck and date with a new order
        val transportIds = listOf(transport1.id!!, transport2.id!!, transport3.id!!)

        whenever(transportRepository.findAllById(transportIds)).thenReturn(transports)
        whenever(entityManager.getReference(Truck::class.java, truck2.licensePlate)).thenReturn(truck2)

        // For the getPlanningByDate call at the end
        val mondayOfWeek = newDate.minusDays(newDate.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(transports)

        whenever(truckService.getAllTrucks()).thenReturn(listOf(truck1, truck2))

        // When
        val result = planningService.reorderTransports(newDate, truck2.licensePlate, transportIds)

        // Then
        // Verify saveAll was called with transports having updated truck, date and sequence numbers
        val transportCaptor = argumentCaptor<List<TransportDto>>()
        verify(transportRepository).saveAll(transportCaptor.capture())

        val updatedTransports = transportCaptor.firstValue
        assertThat(updatedTransports).hasSize(3)
        assertThat(updatedTransports[0].pickupDateTime.toLocalDate()).isEqualTo(newDate)
        assertThat(updatedTransports[0].sequenceNumber).isEqualTo(0)
        assertThat(updatedTransports[1].pickupDateTime.toLocalDate()).isEqualTo(newDate)
        assertThat(updatedTransports[1].sequenceNumber).isEqualTo(1)
        assertThat(updatedTransports[2].pickupDateTime.toLocalDate()).isEqualTo(newDate)
        assertThat(updatedTransports[2].sequenceNumber).isEqualTo(2)
        verify(transportRepository).saveAll(any<List<TransportDto>>())

        // Verify the result contains the updated planning
        assertThat(result).isNotNull
    }

    @Test
    fun `reorderTransports should handle empty transport list gracefully`() {
        // Given
        val date = LocalDate.of(2025, 5, 20)
        val transportIds = emptyList<UUID>()

        whenever(transportRepository.findAllById(transportIds)).thenReturn(emptyList())

        val mondayOfWeek = date.minusDays(date.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(emptyList())

        whenever(truckService.getAllTrucks()).thenReturn(emptyList())

        // When
        val result = planningService.reorderTransports(date, truck1.licensePlate, transportIds)

        // Then
        verify(transportRepository).saveAll(emptyList())
        assertThat(result).isNotNull
    }

    @Test
    fun `reorderTransports should set truck to null when license plate is Niet toegewezen`() {
        // Given
        val date = LocalDate.of(2025, 5, 21)
        val notAssignedLicensePlate = "Niet toegewezen"

        // Transports with original truck
        val transport1 = TransportDtoHelper.transport(truck1)
        val transport2 = TransportDtoHelper.transport(truck1)
        val transportIds = listOf(transport1.id!!, transport2.id!!)
        
        whenever(transportRepository.findAllById(transportIds)).thenReturn(listOf(transport1, transport2))
        
        // For the getPlanningByDate call at the end
        val mondayOfWeek = date.minusDays(date.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(listOf(transport1, transport2))

        whenever(truckService.getAllTrucks()).thenReturn(listOf(truck1, truck2))

        // When
        val result = planningService.reorderTransports(date, notAssignedLicensePlate, transportIds)

        // Then
        // Verify saveAll was called with transports having null truck
        val transportCaptor = argumentCaptor<List<TransportDto>>()
        verify(transportRepository).saveAll(transportCaptor.capture())

        val updatedTransports = transportCaptor.firstValue
        assertThat(updatedTransports).hasSize(2)
        
        // Verify truck is set to null for all transports
        updatedTransports.forEach { transport ->
            assertThat(transport.truck).isNull()
            assertThat(transport.pickupDateTime.toLocalDate()).isEqualTo(date)
        }
        
        // Verify sequence numbers are updated correctly
        assertThat(updatedTransports[0].sequenceNumber).isEqualTo(0)
        assertThat(updatedTransports[1].sequenceNumber).isEqualTo(1)
        
        // Verify the result contains the updated planning
        assertThat(result).isNotNull
    }

    @Test
    fun `reorderTransports should set truck reference when license plate is not Niet toegewezen`() {
        // Given
        val date = LocalDate.of(2025, 5, 21)
        
        // Transports with no truck (unassigned)
        val transport1 = TransportDtoHelper.transport(truck = null)
        val transport2 = TransportDtoHelper.transport(truck = null)
        val transportIds = listOf(transport1.id!!, transport2.id!!)
        
        whenever(transportRepository.findAllById(transportIds)).thenReturn(listOf(transport1, transport2))
        whenever(entityManager.getReference(Truck::class.java, truck2.licensePlate)).thenReturn(truck2)
        
        // For the getPlanningByDate call at the end
        val mondayOfWeek = date.minusDays(date.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(listOf(transport1, transport2))

        whenever(truckService.getAllTrucks()).thenReturn(listOf(truck1, truck2))

        // When
        val result = planningService.reorderTransports(date, truck2.licensePlate, transportIds)

        // Then
        // Verify saveAll was called with transports having the correct truck reference
        val transportCaptor = argumentCaptor<List<TransportDto>>()
        verify(transportRepository).saveAll(transportCaptor.capture())

        val updatedTransports = transportCaptor.firstValue
        assertThat(updatedTransports).hasSize(2)
        
        // Verify truck is set to the referenced truck for all transports
        updatedTransports.forEach { transport ->
            assertThat(transport.truck).isEqualTo(truck2)
            assertThat(transport.pickupDateTime.toLocalDate()).isEqualTo(date)
        }
        
        // Verify sequence numbers are updated correctly
        assertThat(updatedTransports[0].sequenceNumber).isEqualTo(0)
        assertThat(updatedTransports[1].sequenceNumber).isEqualTo(1)
        
        // Verify the result contains the updated planning
        assertThat(result).isNotNull
    }

    @Test
    fun `getPlanningByDate should add specific truck when truckId is provided and not in view`() {
        // Given
        val pickupDate = LocalDate.now()
        val mondayOfWeek = pickupDate.minusDays(pickupDate.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(emptyList())
        whenever(truckService.getTruckByLicensePlate(truck1.licensePlate)).thenReturn(truck1)

        // When
        val result = planningService.getPlanningByDate(pickupDate, truck1.licensePlate)

        // Then
        assertThat(result.transports).hasSize(1)
        assertThat(result.transports[0].truck).isEqualTo(truck1.getDisplayName())
        assertThat(result.transports[0].transports).isEmpty()
    }

    @Test
    fun `getPlanningByDate should not duplicate truck when it already has transports`() {
        // Given
        val pickupDate = LocalDate.now()
        val mondayOfWeek = pickupDate.minusDays(pickupDate.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        val transport = TransportDtoHelper.transport(truck1)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(listOf(transport))

        // When
        val result = planningService.getPlanningByDate(pickupDate, truck1.licensePlate)

        // Then
        assertThat(result.transports).hasSize(1)
        assertThat(result.transports[0].truck).isEqualTo(truck1.getDisplayName())
        assertThat(result.transports[0].transports).isNotEmpty()
    }

    @Test
    fun `getPlanningByDate should add all missing trucks when truckId is null`() {
        // Given
        val pickupDate = LocalDate.now()
        val existingTruck = Truck(
            licensePlate = "DEF-456",
            brand = "DAF",
            model = "XF"
        )

        val mondayOfWeek = pickupDate.minusDays(pickupDate.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        val transport = TransportDtoHelper.transport(existingTruck)

        whenever(
            transportRepository.findByPickupDateTimeIsBetween(
                mondayOfWeek.atStartOfDay(),
                sundayOfWeek.atTime(23, 59, 59)
            )
        ).thenReturn(listOf(transport))

        val allTrucks = listOf(
            existingTruck,
            truck1,
            truck2
        )
        whenever(truckService.getAllTrucks()).thenReturn(allTrucks)

        // When
        val result = planningService.getPlanningByDate(pickupDate)

        // Then
        assertThat(result.transports).hasSize(3)
        assertThat(result.transports.map { it.truck }).containsExactlyInAnyOrder(
            existingTruck.getDisplayName(), truck1.getDisplayName(), truck2.getDisplayName()
        )
    }
}
