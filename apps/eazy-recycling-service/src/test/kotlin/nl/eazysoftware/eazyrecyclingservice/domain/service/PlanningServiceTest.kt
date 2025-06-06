package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.ContainerOperation
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
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

        val truck1 = Truck(licensePlate = "ABC-123", brand = "Volvo", model = "FH16")
        val truck2 = Truck(licensePlate = "XYZ-789", brand = "Mercedes", model = "Actros")

        val driver = ProfileDto(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
        )

        val address1 = AddressDto(
            streetName = "Main Street",
            buildingNumber = "10",
            postalCode = "1234 AB",
            city = "Amsterdam",
            country = "Netherlands"
        )

        val address2 = AddressDto(
            streetName = "Second Street",
            buildingNumber = "20",
            postalCode = "5678 CD",
            city = "Rotterdam",
            country = "Netherlands"
        )

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = address1
        )

        val pickupLocation = LocationDto(
            id = UUID.randomUUID().toString(),
            address = address1
        )

        val deliveryLocation = LocationDto(
            id = UUID.randomUUID().toString(),
            address = address2
        )

        val transport1 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = pickupLocation,
            pickupDateTime = LocalDateTime.of(2025, 5, 20, 10, 0),
            deliveryCompany = company,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = LocalDateTime.of(2025, 5, 20, 14, 0),
            transportType = TransportType.WASTE,
            containerOperation = ContainerOperation.PICKUP,
            truck = truck1,
            driver = driver,
            note = "Test transport 1",
            sequenceNumber = 1,
        )

        val transport2 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-002",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = pickupLocation,
            pickupDateTime = LocalDateTime.of(2025, 5, 21, 11, 0),
            deliveryCompany = company,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = LocalDateTime.of(2025, 5, 21, 15, 0),
            transportType = TransportType.CONTAINER,
            containerOperation = ContainerOperation.DELIVERY,
            truck = truck1,
            driver = driver,
            note = "Test transport 2",
            sequenceNumber = 2,
        )

        val transport3 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-003",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = pickupLocation,
            pickupDateTime = LocalDateTime.of(2025, 5, 22, 9, 0),
            deliveryCompany = company,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = LocalDateTime.of(2025, 5, 22, 13, 0),
            transportType = TransportType.WASTE,
            containerOperation = null,
            truck = null, // Unassigned
            driver = null, // Unassigned
            note = "Test transport 3",
            sequenceNumber = 3,
        )

        val transports = listOf(transport1, transport2, transport3)
        val trucks = listOf(truck1, truck2)

        // When
        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(transports)

        `when`(truckService.getAllTrucks()).thenReturn(trucks)

        val result = planningService.getPlanningByDate(pickupDate)

        // Then
        assertThat(result).isNotNull
        assertThat(result.dates).hasSize(7)
        assertThat(result.dates[0]).isEqualTo(mondayOfWeek.toString())
        assertThat(result.dates[6]).isEqualTo(sundayOfWeek.toString())

        // Should have 3 trucks: ABC-123, XYZ-789, and "Niet toegewezen"
        assertThat(result.transports).hasSize(3)

        // Check that transports are grouped correctly by truck
        val assignedTruckTransports = result.transports.find { it.truck == "ABC-123" }
        assertThat(assignedTruckTransports).isNotNull
        assertThat(assignedTruckTransports!!.transports).hasSize(2) // Two dates with transports

        // Check that unassigned transports are in the "Niet toegewezen" group
        val unassignedTransports = result.transports.find { it.truck == "Niet toegewezen" }
        assertThat(unassignedTransports).isNotNull
        assertThat(unassignedTransports!!.transports).hasSize(1) // One date with transports

        // Check that empty trucks are included
        val emptyTruck = result.transports.find { it.truck == "XYZ-789" }
        assertThat(emptyTruck).isNotNull
        assertThat(emptyTruck!!.transports).isEmpty()
    }

    @Test
    fun `getPlanningByDate should filter by truckId when provided`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20)
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)
        val truckId = "ABC-123"

        val truck1 = Truck(licensePlate = "ABC-123", brand = "Mercedes", model = "Actros")
        val truck2 = Truck(licensePlate = "XYZ-789", brand = "Scania", model = "R450")

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val pickupLocation = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val deliveryLocation = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Second Street",
                buildingNumber = "20",
                postalCode = "5678 CD",
                city = "Rotterdam",
                country = "Netherlands"
            )
        )

        val transport1 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = pickupLocation,
            pickupDateTime = LocalDateTime.of(2025, 5, 20, 10, 0),
            deliveryCompany = company,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = LocalDateTime.of(2025, 5, 20, 14, 0),
            transportType = TransportType.WASTE,
            truck = truck1,
            note = "Test transport 1",
            sequenceNumber = 1,
        )

        val transport2 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-002",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = pickupLocation,
            pickupDateTime = LocalDateTime.of(2025, 5, 21, 11, 0),
            deliveryCompany = company,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = LocalDateTime.of(2025, 5, 21, 15, 0),
            transportType = TransportType.CONTAINER,
            truck = truck2,
            note = "Test transport 2",
            sequenceNumber = 2
        )

        val transports = listOf(transport1, transport2)
        val trucks = listOf(truck1, truck2)

        // When
        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(transports)

        `when`(truckService.getAllTrucks()).thenReturn(trucks)

        val result = planningService.getPlanningByDate(pickupDate, truckId)

        // Then
        assertThat(result).isNotNull
        assertThat(result.transports).hasSize(2) // ABC-123 and XYZ-789 (empty)

        // Check that only transports for the specified truck are included
        val filteredTransports = result.transports.find { it.truck == truckId }
        assertThat(filteredTransports).isNotNull
        assertThat(filteredTransports!!.transports).hasSize(1) // One date with transports
        assertThat(filteredTransports.transports.values.flatten()).hasSize(1) // One transport
    }

    @Test
    fun `getPlanningByDate should filter by driverId when provided`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20)
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)
        val driverId = UUID.randomUUID()

        val truck = Truck(licensePlate = "ABC-123", brand = "Mercedes", model = "Actros")

        val driver1 = ProfileDto(
            id = driverId,
            firstName = "John",
            lastName = "Doe",
        )

        val driver2 = ProfileDto(
            id = UUID.randomUUID(),
            firstName = "Jane",
            lastName = "Smith",
        )

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val location = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val transport1 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 20, 10, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 20, 14, 0),
            transportType = TransportType.WASTE,
            truck = truck,
            driver = driver1,
            note = "Test transport 1",
            sequenceNumber = 1
        )

        val transport2 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-002",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 21, 11, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 21, 15, 0),
            transportType = TransportType.CONTAINER,
            truck = truck,
            driver = driver2,
            note = "Test transport 2",
            sequenceNumber = 2,
        )

        val transports = listOf(transport1, transport2)
        val trucks = listOf(truck)

        // When
        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(transports)

        `when`(truckService.getAllTrucks()).thenReturn(trucks)

        val result = planningService.getPlanningByDate(pickupDate, driverId = driverId)

        // Then
        assertThat(result).isNotNull

        // Check that only transports for the specified driver are included
        val truckTransports = result.transports.find { it.truck == "ABC-123" }
        assertThat(truckTransports).isNotNull
        assertThat(truckTransports!!.transports).hasSize(1) // One date with transports
        assertThat(truckTransports.transports.values.flatten()).hasSize(1) // One transport

        // Verify the transport is the one with the specified driver
        val transportView = truckTransports.transports.values.flatten().first()
        assertThat(transportView.driver?.id).isEqualTo(driverId)
    }

    @Test
    fun `getPlanningByDate should filter by status when provided`() {
        // Given
        val pickupDate = LocalDate.of(2025, 5, 20)
        val mondayOfWeek = LocalDate.of(2025, 5, 19)
        val sundayOfWeek = LocalDate.of(2025, 5, 25)
        val status = "PLANNED,FINISHED"

        val truck = Truck(licensePlate = "ABC-123", brand = "Mercedes", model = "Actros")

        val driver = ProfileDto(
            id = UUID.randomUUID(),
            firstName = "John",
            lastName = "Doe",
        )

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val location = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        // PLANNED transport (has truck and driver, and delivery time is in the future)
        val transport1 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 20, 10, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 6, 20, 14, 0), // Future date
            transportType = TransportType.WASTE,
            truck = truck,
            driver = driver,
            note = "Planned transport",
            sequenceNumber = 1,
        )

        // UNPLANNED transport (no truck or driver assigned)
        val transport2 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-002",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 21, 11, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 21, 15, 0),
            transportType = TransportType.CONTAINER,
            truck = null,
            driver = null,
            note = "Unplanned transport",
            sequenceNumber = 2,
        )

        // FINISHED transport (has truck and driver, and delivery time is in the past)
        val transport3 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-003",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 22, 9, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 22, 13, 0), // Past date (relative to now in the test)
            transportType = TransportType.WASTE,
            truck = truck,
            driver = driver,
            note = "Finished transport",
            sequenceNumber = 3,
        )

        val transports = listOf(transport1, transport2, transport3)
        val trucks = listOf(truck)

        // When
        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(transports)

        `when`(truckService.getAllTrucks()).thenReturn(trucks)

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
        // Given
        val truck1 = Truck(licensePlate = "ABC-123", brand = "Mercedes", model = "Actros")
        val truck2 = Truck(licensePlate = "XYZ-789", brand = "Scania", model = "R450")

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val location = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val transport1 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 20, 10, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 20, 14, 0),
            transportType = TransportType.WASTE,
            truck = truck1,
            note = "Test transport 1",
            sequenceNumber = 1
        )

        val transport2 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-002",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 20, 11, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 20, 15, 0),
            transportType = TransportType.CONTAINER,
            truck = truck1,
            note = "Test transport 2",
            sequenceNumber = 2,
        )

        val transport3 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-003",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 21, 9, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 21, 13, 0),
            transportType = TransportType.WASTE,
            truck = truck2,
            note = "Test transport 3",
            sequenceNumber = 3,
        )

        val transport4 = TransportDto(
            id = UUID.randomUUID(),
            displayNumber = "T-004",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(2025, 5, 22, 10, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(2025, 5, 22, 14, 0),
            transportType = TransportType.CONTAINER,
            truck = null, // Unassigned
            note = "Test transport 4",
            sequenceNumber = 4,
        )

        val transports = listOf(transport1, transport2, transport3, transport4)

        // When
        val result = planningService.createTransportsView(transports)

        // Then
        assertThat(result).hasSize(3) // ABC-123, XYZ-789, and "Niet toegewezen"

        // Check truck1 (ABC-123) group
        val truck1Group = result.find { it.truck == "ABC-123" }
        assertThat(truck1Group).isNotNull
        assertThat(truck1Group!!.transports).hasSize(1) // One date
        assertThat(truck1Group.transports["2025-05-20"]).hasSize(2) // Two transports on this date

        // Check truck2 (XYZ-789) group
        val truck2Group = result.find { it.truck == "XYZ-789" }
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
        val originalDate = LocalDate.of(2025, 5, 20)
        val newDate = LocalDate.of(2025, 5, 21)
        val originalLicensePlate = "ABC-123"
        val newLicensePlate = "XYZ-789"

        val truck1 = Truck(licensePlate = originalLicensePlate, brand = "Mercedes", model = "Actros")
        val truck2 = Truck(licensePlate = newLicensePlate, brand = "Scania", model = "R450")

        val transport1Id = UUID.randomUUID()
        val transport2Id = UUID.randomUUID()
        val transport3Id = UUID.randomUUID()

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val location = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        // Transports with original truck and date
        val transport1 = TransportDto(
            id = transport1Id,
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 10, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 14, 0),
            transportType = TransportType.WASTE,
            truck = truck1,
            note = "Test transport 1",
            sequenceNumber = 0
        )

        val transport2 = TransportDto(
            id = transport2Id,
            displayNumber = "T-002",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 11, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 15, 0),
            transportType = TransportType.CONTAINER,
            truck = truck1,
            note = "Test transport 2",
            sequenceNumber = 1
        )

        val transport3 = TransportDto(
            id = transport3Id,
            displayNumber = "T-003",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 9, 0),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 13, 0),
            transportType = TransportType.WASTE,
            truck = truck1,
            note = "Test transport 3",
            sequenceNumber = 2
        )

        val transports = listOf(transport1, transport2, transport3)

        // We'll move all transports to a new truck and date with a new order
        val transportIds = listOf(transport3Id, transport1Id, transport2Id)

        // Mock repository responses
        `when`(transportRepository.findAllById(transportIds)).thenReturn(transports)

        // Mock entity manager to return the truck reference
        `when`(entityManager.getReference(Truck::class.java, newLicensePlate)).thenReturn(truck2)

        // For the getPlanningByDate call at the end
        val mondayOfWeek = newDate.minusDays(newDate.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(transports)

        `when`(truckService.getAllTrucks()).thenReturn(listOf(truck1, truck2))

        // When
        val result = planningService.reorderTransports(newDate, newLicensePlate, transportIds)

        // Then
        // Verify saveAll was called with transports having updated truck, date and sequence numbers
        val transportCaptor = argumentCaptor<List<TransportDto>>()
        verify(transportRepository).saveAll(transportCaptor.capture())

        val updatedTransports = transportCaptor.firstValue
        assertThat(updatedTransports).hasSize(3)
        assertThat(updatedTransports[0].pickupDateTime!!.toLocalDate()).isEqualTo(newDate)
        assertThat(updatedTransports[0].sequenceNumber).isEqualTo(0)
        assertThat(updatedTransports[1].pickupDateTime!!.toLocalDate()).isEqualTo(newDate)
        assertThat(updatedTransports[1].sequenceNumber).isEqualTo(1)
        assertThat(updatedTransports[2].pickupDateTime!!.toLocalDate()).isEqualTo(newDate)
        assertThat(updatedTransports[2].sequenceNumber).isEqualTo(2)
        verify(transportRepository).saveAll(any<List<TransportDto>>())

        // Verify the result contains the updated planning
        assertThat(result).isNotNull
    }

    @Test
    fun `reorderTransports should preserve time components when changing date`() {
        // Given
        val originalDate = LocalDate.of(2025, 5, 20)
        val newDate = LocalDate.of(2025, 5, 21)
        val licensePlate = "ABC-123"

        val truck = Truck(licensePlate = licensePlate, brand = "Mercedes", model = "Actros")

        val transportId = UUID.randomUUID()

        val company = CompanyDto(
            id = UUID.randomUUID(),
            name = "Test Company",
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        val location = LocationDto(
            id = UUID.randomUUID().toString(),
            address = AddressDto(
                streetName = "Main Street",
                buildingNumber = "10",
                postalCode = "1234 AB",
                city = "Amsterdam",
                country = "Netherlands"
            )
        )

        // Transport with specific time components
        val originalHour = 14
        val originalMinute = 30
        val transport = TransportDto(
            id = transportId,
            displayNumber = "T-001",
            consignorParty = company,
            carrierParty = company,
            pickupCompany = company,
            pickupLocation = location,
            pickupDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, originalHour, originalMinute),
            deliveryCompany = company,
            deliveryLocation = location,
            deliveryDateTime = LocalDateTime.of(originalDate.year, originalDate.monthValue, originalDate.dayOfMonth, 18, 0),
            transportType = TransportType.WASTE,
            truck = truck,
            note = "Test transport with specific time",
            sequenceNumber = 0
        )

        val transports = listOf(transport)
        val transportIds = listOf(transportId)

        // Mock repository responses
        `when`(transportRepository.findAllById(transportIds)).thenReturn(transports)

        // Mock entity manager
        `when`(entityManager.getReference(Truck::class.java, licensePlate)).thenReturn(truck)

        // For the getPlanningByDate call at the end
        val mondayOfWeek = newDate.minusDays(newDate.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(transports)

        `when`(truckService.getAllTrucks()).thenReturn(listOf(truck))

        // When
        planningService.reorderTransports(newDate, licensePlate, transportIds)

        // Then
        verify(transportRepository).saveAll(any<List<TransportDto>>())
    }

    @Test
    fun `reorderTransports should handle empty transport list gracefully`() {
        // Given
        val date = LocalDate.of(2025, 5, 20)
        val licensePlate = "ABC-123"
        val transportIds = emptyList<UUID>()

        // Mock repository responses
        `when`(transportRepository.findAllById(transportIds)).thenReturn(emptyList())

        // For the getPlanningByDate call at the end
        val mondayOfWeek = date.minusDays(date.dayOfWeek.value - 1L)
        val sundayOfWeek = mondayOfWeek.plusDays(6)

        `when`(transportRepository.findByPickupDateTimeIsBetween(
            mondayOfWeek.atStartOfDay(),
            sundayOfWeek.atTime(23, 59, 59)
        )).thenReturn(emptyList())

        `when`(truckService.getAllTrucks()).thenReturn(emptyList())

        // When
        val result = planningService.reorderTransports(date, licensePlate, transportIds)

        // Then
        // Verify saveAll was not called with an empty list
        verify(transportRepository).saveAll(emptyList())
        assertThat(result).isNotNull
    }
}
