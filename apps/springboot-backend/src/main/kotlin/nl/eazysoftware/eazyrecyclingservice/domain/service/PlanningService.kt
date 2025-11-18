package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.controller.transport.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Trucks
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import nl.eazysoftware.eazyrecyclingservice.repository.truck.TruckMapper
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class PlanningService(
    private val transportRepository: TransportRepository,
    private val trucks: Trucks,
    private val truckMapper: TruckMapper,
    private val entityManager: EntityManager,
) {

    fun getPlanningByDate(
        pickupDate: LocalDate,
        truckId: String? = null,
        driverId: UUID? = null,
        status: String? = null
    ): PlanningView {
        val daysInWeek = getDaysInWeek(pickupDate)
        val statuses = status?.split(',') ?: emptyList()

        // Get all transports for the week
        val transports = transportRepository.findByPickupDateTimeIsBetween(
            daysInWeek.first().atStartOfDay().atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
            daysInWeek.last().atTime(23, 59, 59).atZone(ZoneId.of("Europe/Amsterdam")).toInstant()
        )
            .filter { transport -> truckId == null || transport.truck?.licensePlate == truckId }
            .filter { transport -> driverId == null || transport.driver?.id == driverId }
            .filter { transport -> statuses.isEmpty() || statuses.contains(transport.getStatus().name) }
        val dateInfo = daysInWeek.map { it.toString() }
        val transportsView = createTransportsView(transports)

        getMissingTrucks(transports, truckId)
            .forEach { transportsView.add(PlanningTransportsView(it.getDisplayName(), emptyMap())) }

        transportsView.sortWith(compareBy<PlanningTransportsView> {
            when {
                it.truck == "Niet toegewezen" -> 0
                it.transports.isNotEmpty() -> 1
                else -> 2
            }
        }.thenBy { it.truck }) // Then sort alphabetically by truck license plate

        return PlanningView(dateInfo, transportsView)
    }

    fun createTransportsView(transports: List<TransportDto>) =
        transports.map { transportDto -> PlanningTransportView(transportDto) }
            .groupBy { transportView -> transportView.truck }
            .map { (truck, transportViews) ->
                // Group by pickup date
                val transportsByDate = transportViews.groupBy { it.pickupDate }

                // Sort each group by sequence number (if available)
                val sortedTransportsByDate = transportsByDate.mapValues { (_, dateTransports) ->
                    dateTransports.sortedBy { it.sequenceNumber }
                }
                val displayName = truck?.getDisplayName() ?: "Niet toegewezen"

                PlanningTransportsView(displayName, sortedTransportsByDate)
            }.toMutableList()


    private fun getMissingTrucks(transports: List<TransportDto>, truckId: String?): List<TruckDto> {
        // When filtered by truckId which already has transports, don't add missing trucks because this would erase the filter
        if (truckId != null && transports.any { it.truck?.licensePlate == truckId }) {
            return emptyList()
        }

        val missingTrucks: List<TruckDto>

        if (truckId != null) {
            val truck = trucks.findByLicensePlate(LicensePlate(truckId))
                ?: throw IllegalArgumentException("Vrachtwagen met kenteken $truckId niet gevonden")
            missingTrucks = listOf(truckMapper.toDto(truck))
        } else {
            val existingTrucks = transports.map { it.truck }.toSet()
            missingTrucks = trucks.findAll()
                .map { truckMapper.toDto(it) }
                .filterNot { it in existingTrucks }
        }

        return missingTrucks
    }

    private fun getDaysInWeek(day: LocalDate): List<LocalDate> {
        // Find the first day of the week (Monday) for the given date
        val dayOfWeek = day.dayOfWeek.ordinal.toLong()
        val monday = day.minus(dayOfWeek, ChronoUnit.DAYS)

        // Create a list with all 7 days of the week
        return (0L..6L).map { dayOffset ->
            monday.plus(dayOffset, ChronoUnit.DAYS)
        }
    }

    fun reorderTransports(date: LocalDate, displayName: String, transportIds: List<UUID>): PlanningView {
        val licensePlate = TruckDto.extractLicensePlateFromDisplayName(displayName)
        val transports = transportRepository.findAllById(transportIds)

        val transportsWithSequenceNumber = transportIds.mapIndexed { index, id ->
            val transport = transports.first { it.id == id }
            var truck: TruckDto? = null

            if (licensePlate != "Niet toegewezen") {
                truck = entityManager.getReference(TruckDto::class.java, licensePlate)
            }

            // Update the date while preserving the time of day
            val currentDateTime = transport.pickupDateTime.atZone(ZoneId.of("Europe/Amsterdam"))
            val newPickupDateTime = date.atTime(currentDateTime.toLocalTime())
                .atZone(ZoneId.of("Europe/Amsterdam"))
                .toInstant()

            transport.copy(
                truck = truck,
                pickupDateTime = newPickupDateTime,
                deliveryDateTime = transport.deliveryDateTime,
                sequenceNumber = index,
            )
        }
        transportRepository.saveAll(transportsWithSequenceNumber)

        return getPlanningByDate(date)
    }


    fun getPlanningByDriver(driverId: UUID, startDate: LocalDate, endDate: LocalDate): DriverPlanning {
        return transportRepository.findByDriverIdAndPickupDateTimeIsBetween(
            driverId,
            startDate.atStartOfDay().atZone(ZoneId.of("Europe/Amsterdam")).toInstant(),
            endDate.atTime(23, 59, 59).atZone(ZoneId.of("Europe/Amsterdam")).toInstant()
        )
            .groupBy { it.pickupDateTime.atZone(ZoneId.of("Europe/Amsterdam")).toLocalDate() }
            .mapValues { (_, transportsByDate) ->
                transportsByDate
                    .groupBy { it.truck?.licensePlate ?: "Niet toegewezen" }
                    .mapValues { (_, transportsByTruck) -> transportsByTruck.map { DriverPlanningItem(it) } }
            }
    }
}
