package nl.eazysoftware.springtemplate.domain.mapper

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.springtemplate.controller.CreateContainerTransportRequest
import nl.eazysoftware.springtemplate.repository.*
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportType
import nl.eazysoftware.springtemplate.repository.entity.waybill.AddressDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class TransportService(
    private val transportRepository: TransportRepository,
    private val truckRepository: TruckRepository,
    private val waybillRepository: WaybillRepository,
    private val driverRepository: DriverRepository,
    private val locationRepository: LocationRepository,
    private val companyRepository: CompanyRepository,
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

    fun assignWaybillTransport(waybillId: UUID, licensePlate: String, driverId: UUID): TransportDto {
        val waybill = waybillRepository.findById(waybillId)
            .orElseThrow { EntityNotFoundException("Waybill with id $waybillId not found") }

        val truck = truckRepository.findByLicensePlate(licensePlate)
            ?: throw EntityNotFoundException("Truck with $licensePlate not found")

        val driver = driverRepository.findById(driverId)
            .orElseThrow { EntityNotFoundException("Driver with id $driverId not found") }

        val transport = TransportDto(
            waybill = waybill,
            truck = truck,
            driver = driver,
            transportType = TransportType.WAYBILL,
        )

        return transportRepository.save(transport)
    }

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    fun assignContainerTransport(request: CreateContainerTransportRequest): TransportDto {
        val truck = truckRepository.findByLicensePlate(request.licensePlate)
            ?: throw EntityNotFoundException("Truck with $request.licensePlate not found")
        val customer = companyRepository.findById(request.customerId)
            .getOrNull()
            ?: throw EntityNotFoundException("Company with id $request.customerId not found")
        val driver = driverRepository.findById(request.driverId)
            .orElseThrow { EntityNotFoundException("Driver with id $request.driverId not found") }

        val origin = locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(request.originAddress.streetName, request.originAddress.buildingNumber)
            ?: LocationDto(
                address = AddressDto(
                    streetName = request.originAddress.streetName,
                    buildingNumber = request.originAddress.buildingNumber,
                    postalCode = request.originAddress.postalCode,
                    city = request.originAddress.city,
                    country = request.originAddress.country
                ),
                id = UUID.randomUUID().toString()
            )

        val destination = locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(request.destinationAddress.streetName, request.destinationAddress.buildingNumber)
            ?: LocationDto(
                address = AddressDto(
                    streetName = request.originAddress.streetName,
                    buildingNumber = request.originAddress.buildingNumber,
                    postalCode = request.originAddress.postalCode,
                    city = request.originAddress.city,
                    country = request.originAddress.country
                ),
                id = UUID.randomUUID().toString()
            )

        val transport = TransportDto(
            customer = customer,
            customOrigin = origin,
            pickupDateTime = request.pickupDateTime,
            customDestination = destination,
            deliveryDateTime = request.deliveryDateTime,
            truck = truck,
            driver = driver,
            transportType = request.transportType,
            containerType = request.containerType,
            waybill = null,
        )

        return transportRepository.save(transport)
    }
}