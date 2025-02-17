package nl.eazysoftware.springtemplate.domain.mapper

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.springtemplate.repository.DriverRepository
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import nl.eazysoftware.springtemplate.repository.TransportRepository
import nl.eazysoftware.springtemplate.repository.TruckRepository
import nl.eazysoftware.springtemplate.repository.WaybillRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class TransportService(
    private val transportRepository: TransportRepository,
    private val truckRepository: TruckRepository,
    private val waybillRepository: WaybillRepository,
    private val driverRepository: DriverRepository,
) {

    fun getTransportByDateSortedByTruck(pickupDate: LocalDate): Map<String, List<TransportDto>> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)

        val trucks = truckRepository.findAll().map { it.licensePlate }.toSet()
        val transportsByTruck = transportRepository
            .findByWaybill_PickupDateTimeBetween(start, end)
            .groupBy { it.truck.licensePlate ?: "NOT_ASSIGNED" }
            .toMutableMap()

        val missingLicensePlates = trucks - transportsByTruck.keys
        missingLicensePlates.forEach { licensePlate ->
            transportsByTruck[licensePlate] = emptyList()
        }

        return transportsByTruck
    }

    fun assignTransport(waybillId: UUID, licensePlate: String, driverId: UUID): TransportDto {
        val waybill = waybillRepository.findById(waybillId)
            .orElseThrow { EntityNotFoundException("Waybill with id $waybillId not found") }

        val truck = truckRepository.findByLicensePlate(licensePlate)
            ?: throw EntityNotFoundException("Truck with $licensePlate not found")

        val driver = driverRepository.findById(driverId)
            .orElseThrow { EntityNotFoundException("Driver with id $driverId not found") }

        val transport = TransportDto(
            waybill = waybill,
            truck = truck,
            driver = driver
        )

        return transportRepository.save(transport)
    }

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }
}