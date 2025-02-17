package nl.eazysoftware.springtemplate.domain.mapper

import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportRepository
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class TransportService(
    private val transportRepository: TransportRepository
) {

    fun getTranspsortByDateSortedByTruck(pickupDate: LocalDate): Map<String, List<TransportDto>> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)


        return transportRepository
            .findByWaybill_PickupDateTimeBetween(start, end)
            .groupBy { it.waybill?.licensePlate ?: "NOT_ASSIGNED" }
    }

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }
}