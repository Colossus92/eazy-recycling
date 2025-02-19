package nl.eazysoftware.springtemplate.domain.mapper

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.springtemplate.controller.CreateContainerTransportRequest
import nl.eazysoftware.springtemplate.repository.*
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportType
import nl.eazysoftware.springtemplate.repository.entity.waybill.AddressDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.CompanyDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*
import kotlin.jvm.optionals.getOrNull

@Service
class TransportService(
    private val transportRepository: TransportRepository,
    private val truckRepository: TruckRepository,
    private val driverRepository: DriverRepository,
    private val locationRepository: LocationRepository,
    private val companyRepository: CompanyRepository,
    private val entityManager: EntityManager,
) {

    fun getTransportByDateSortedByTruck(pickupDate: LocalDate): Map<String, List<TransportDto>> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)

        val trucks = truckRepository.findAll().map { it.licensePlate }.toSet()
        val transportsByTruck = transportRepository
            .findByPickupDateTimeBetween(start, end)
            .groupBy { it.truck?.licensePlate ?: "NOT_ASSIGNED" }
            .toMutableMap()

        val missingLicensePlates = trucks - transportsByTruck.keys
        missingLicensePlates.forEach { licensePlate ->
            transportsByTruck[licensePlate] = emptyList()
        }

        return transportsByTruck
    }

    fun assignWaybillTransport(waybillId: UUID, licensePlate: String, driverId: UUID): TransportDto {
        val transport = transportRepository.findByGoods_Uuid(waybillId)
            ?: throw EntityNotFoundException("Waybill with id $waybillId not found")

        val truck = truckRepository.findByLicensePlate(licensePlate)
            ?: throw EntityNotFoundException("Truck with $licensePlate not found")

        val driver = driverRepository.findById(driverId)
            .orElseThrow { EntityNotFoundException("Driver with id $driverId not found") }

        return transportRepository.save(transport.copy(
            truck = truck,
            driver = driver,
            transportType = TransportType.WAYBILL,
        ))
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

        val origin = locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(
            request.originAddress.streetName,
            request.originAddress.buildingNumber
        )
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

        val destination = locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(
            request.destinationAddress.streetName,
            request.destinationAddress.buildingNumber
        )
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
            consignorParty = customer,
            pickupLocation = origin,
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = destination,
            deliveryDateTime = request.deliveryDateTime,
            truck = truck,
            driver = driver,
            transportType = request.transportType,
            containerType = request.containerType,
            carrierParty = companyRepository.findById(UUID.fromString("a5f516e7-d504-436f-b9bb-79a95da73902"))
                .orElseThrow { EntityNotFoundException("Default carrier not found") }
        )

        return transportRepository.save(transport)
    }

    @Transactional
    fun save(transportDto: TransportDto): TransportDto {
        val truck = transportDto.truck?.licensePlate
            ?.let { truckRepository.findByLicensePlate(it) }
        val consignor = findCompany(transportDto.consignorParty)
        val carrier = findCompany(transportDto.carrierParty)

        val updatedGoods = transportDto.goods?.let { goods ->
            val consignee = findCompany(goods.consigneeParty)
            val pickup = findCompany(goods.pickupParty)

            goods.copy(
                consigneeParty = consignee,
                pickupParty = pickup
            )
        }

        val updatedTransport = transportDto.copy(
            goods = updatedGoods,
            consignorParty = consignor,
            carrierParty = carrier,
            truck = truck,
        )

        return entityManager.merge(updatedTransport)
    }

    fun findCompany(company: CompanyDto): CompanyDto {
        return companyRepository.findByChamberOfCommerceIdAndVihbId(company.chamberOfCommerceId, company.vihbId)
            ?: company

    }
}