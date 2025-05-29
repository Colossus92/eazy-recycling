package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.controller.transport.PlanningView
import nl.eazysoftware.eazyrecyclingservice.controller.transport.TransportView
import nl.eazysoftware.eazyrecyclingservice.controller.transport.TransportsView
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.collections.component1
import kotlin.collections.component2

@Service
class PlanningService(
    private val transportRepository: TransportRepository,
    private val truckService: TruckService
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
            daysInWeek.first().atStartOfDay(),
            daysInWeek.last().atTime(23, 59, 59)
        )
            .filter { transport -> truckId == null || transport.truck?.licensePlate == truckId }
            .filter { transport -> driverId == null || transport.driver?.id == driverId }
            .filter { transport -> statuses.isEmpty() || statuses.contains(transport.getStatus().name) }

        val dateInfo = daysInWeek.map { it.toString() }

        val transportsView = createTransportsView(transports)

        addMissingTrucks(transportsView)

        transportsView.sortWith(compareBy<TransportsView> {
            when {
                it.truck == "Niet toegewezen" -> 0
                it.transports.isNotEmpty() -> 1
                else -> 2
            }
        }.thenBy { it.truck }) // Then sort alphabetically by truck license plate

        return PlanningView(dateInfo, transportsView)
    }

    fun createTransportsView(transports: List<TransportDto>) =
        transports.map { transportDto -> TransportView(transportDto) }
            .groupBy { transportView -> transportView.truck?.licensePlate ?: "Niet toegewezen" }
            .map { (truckLicensePlate, transportViews) ->
                TransportsView(truckLicensePlate, transportViews.groupBy { it.pickupDate })
            }.toMutableList()

    private fun addMissingTrucks(transportsView: MutableList<TransportsView>) {
        truckService.getAllTrucks()
            .map { it.licensePlate }
            .filter { it !in transportsView.map { it.truck } }
            .map { TransportsView(it, emptyMap()) }
            .apply { transportsView.addAll(this) }
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
}