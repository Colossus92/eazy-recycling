package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.controller.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.PlanningView
import nl.eazysoftware.eazyrecyclingservice.controller.transport.TransportView
import nl.eazysoftware.eazyrecyclingservice.controller.transport.TransportsView
import nl.eazysoftware.eazyrecyclingservice.repository.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.driver.Driver
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
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

    private val log = LoggerFactory.getLogger(this::class.java)

    fun getTransportByDateSortedByTruck(pickupDate: LocalDate): Map<String, List<TransportDto>> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)

        val trucks = truckRepository.findAll().map { it.licensePlate }.toSet()
        val transportsByTruck = transportRepository
            .findByTruckIsNotNullAndPickupDateTimeBetween(start, end)
            .groupBy { it.truck?.licensePlate ?: "Niet toegewezen" }
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

        return transportRepository.save(
            transport.copy(
                truck = truck,
                driver = driver,
                transportType = TransportType.WAYBILL,
            )
        )
    }

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    fun createContainerTransport(request: CreateContainerTransportRequest): TransportDto {
        val truck = request.licensePlate?.let { findTruck(it) }
        val consignorParty = companyRepository.findById(request.consignorPartyId)
            .getOrNull()
            ?: throw EntityNotFoundException("Company with id $request.customerId not found")
        val driver = request.driverId?.let { findDriver(it) }
        val pickupLocation = findOrCreateLocation(AddressRequest(
            streetName = request.pickupStreet,
            buildingNumber = request.pickupHouseNumber,
            postalCode = request.pickupPostalCode,
            city = request.pickupCity,
        ))
        val deliveryLocation = findOrCreateLocation(
            AddressRequest(
                streetName = request.deliveryStreet,
                buildingNumber = request.deliveryHouseNumber,
                postalCode = request.deliveryPostalCode,
                city = request.deliveryCity,
            )
        )

        val transport = TransportDto(
            consignorParty = consignorParty,
            pickupLocation = pickupLocation,
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = request.deliveryDateTime,
            truck = truck,
            driver = driver,
            transportType = request.typeOfTransport,
            containerType = request.containerType,
            carrierParty = request.carrierPartyId
                .let { companyRepository.findById(it) }
                .getOrNull()
                ?: throw  EntityNotFoundException("Default carrier not found")
        )

        log.info("Creating container transport: $transport")

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

    fun updateTransport(request: CreateContainerTransportRequest): TransportDto {
        val transport = transportRepository.findById(UUID.fromString(request.id))
            .orElseThrow { EntityNotFoundException("Transport with id ${request.id} not found") }

        val updatedTransport = transport.copy(
            consignorParty = companyRepository.findById(request.consignorPartyId)
                .orElseThrow { EntityNotFoundException("Company with id ${request.consignorPartyId} not found") },
            pickupLocation = findOrCreateLocation(
                AddressRequest(
                    streetName = request.pickupStreet,
                    buildingNumber = request.pickupHouseNumber,
                    postalCode = request.pickupPostalCode,
                    city = request.pickupCity,
                )
            ),
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = findOrCreateLocation(
                AddressRequest(
                    streetName = request.deliveryStreet,
                    buildingNumber = request.deliveryHouseNumber,
                    postalCode = request.deliveryPostalCode,
                    city = request.deliveryCity,
                )
            ),
            deliveryDateTime = request.deliveryDateTime,
            containerType = request.containerType,
            transportType = request.typeOfTransport,
            truck = request.licensePlate?.let { findTruck(it) },
            driver = request.driverId?.let { findDriver(it) },
        )

        return transportRepository.save(updatedTransport)
    }

    private fun findOrCreateLocation(address: AddressRequest) =
        (locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(
            address.postalCode,
            address.buildingNumber
        )
            ?: LocationDto(
                address = AddressDto(
                    streetName = address.streetName,
                    buildingNumber = address.buildingNumber,
                    postalCode = address.postalCode,
                    city = address.city,
                    country = address.country
                ),
                id = UUID.randomUUID().toString()
            ))

    fun getPlanningByDate(pickupDate: LocalDate): PlanningView {
        val daysInWeek = getDaysInWeek(pickupDate)
        val transports = transportRepository.findByPickupDateTimeIsBetween(
            daysInWeek.first().atTime(0, 0, 0),
            daysInWeek.last().atTime(23, 59, 59)
        )

        val dateInfo = daysInWeek.map { it.toString() }


        val transportsView = transports.map { transportDto -> TransportView(transportDto) }
            .groupBy { transportView -> transportView.truck?.licensePlate ?: "Niet toegewezen" }
            .map { (truckLicensePlate, transportViews) ->
                TransportsView(truckLicensePlate, transportViews.groupBy { it.pickupDate })
            }

        return PlanningView(dateInfo, transportsView)
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

    private fun findTruck(licensePlate: String): Truck? {
        return truckRepository.findByLicensePlate(licensePlate)
            ?: throw EntityNotFoundException("Truck with $licensePlate not found")
    }

    private fun findDriver(driverId: UUID): Driver? {
        return driverRepository.findById(driverId)
            .getOrNull()
            ?: throw EntityNotFoundException("Driver with id $driverId not found")
    }
}