package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.controller.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.PlanningView
import nl.eazysoftware.eazyrecyclingservice.controller.transport.TransportView
import nl.eazysoftware.eazyrecyclingservice.controller.transport.TransportsView
import nl.eazysoftware.eazyrecyclingservice.repository.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*

@Service
class TransportService(
    private val transportRepository: TransportRepository,
    private val locationRepository: LocationRepository,
    private val companyRepository: CompanyRepository,
    private val entityManager: EntityManager,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun assignWaybillTransport(waybillId: UUID, licensePlate: String, driverId: UUID): TransportDto {
        val transport = transportRepository.findByGoods_Uuid(waybillId)
            ?: throw EntityNotFoundException("Waybill with id $waybillId not found")

        return transportRepository.save(
            transport.copy(
                truck = entityManager.getReference(Truck::class.java, licensePlate),
                driver = entityManager.getReference(ProfileDto::class.java, driverId),
                transportType = TransportType.WAYBILL,
            )
        )
    }

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    fun createContainerTransport(request: CreateContainerTransportRequest): TransportDto {
        val pickupLocation = findOrCreateLocation(
            AddressRequest(
                streetName = request.pickupStreet,
                buildingNumber = request.pickupHouseNumber,
                postalCode = request.pickupPostalCode,
                city = request.pickupCity,
            )
        )

        // For delivery location, check if it's the same as pickup location (by postal code and building number)
        val deliveryLocation = if (request.pickupPostalCode == request.deliveryPostalCode &&
            request.pickupHouseNumber == request.deliveryHouseNumber
        ) {
            // If they're the same, reuse the pickup location to avoid unique constraint violation
            pickupLocation
        } else {
            // Otherwise, find or create a new location
            findOrCreateLocation(
                AddressRequest(
                    streetName = request.deliveryStreet,
                    buildingNumber = request.deliveryHouseNumber,
                    postalCode = request.deliveryPostalCode,
                    city = request.deliveryCity,
                )
            )
        }

        val transport = TransportDto(
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            pickupCompany = request.pickupCompanyId?.let { entityManager.getReference(CompanyDto::class.java, request.pickupCompanyId) },
            pickupLocation = pickupLocation,
            pickupDateTime = request.pickupDateTime,
            deliveryCompany = request.deliveryCompanyId?.let { entityManager.getReference(CompanyDto::class.java, request.deliveryCompanyId) },
            deliveryLocation = deliveryLocation,
            deliveryDateTime = request.deliveryDateTime,
            truck = request.truckId?.let { entityManager.getReference(Truck::class.java, it) },
            driver = request.driverId?.let { entityManager.getReference(ProfileDto::class.java, it) },
            transportType = request.transportType,
            wasteContainer = request.containerId?.let { entityManager.getReference(WasteContainerDto::class.java, it) },
            carrierParty = entityManager.getReference(CompanyDto::class.java, request.carrierPartyId),
        )

        log.info("Creating container transport: $transport")

        return transportRepository.save(transport)
    }

    @Transactional
    fun save(transportDto: TransportDto): TransportDto {
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
            truck = entityManager.getReference(Truck::class.java, transportDto.truck?.licensePlate),
        )

        return entityManager.merge(updatedTransport)
    }

    fun findCompany(company: CompanyDto): CompanyDto {
        return companyRepository.findByChamberOfCommerceIdAndVihbId(company.chamberOfCommerceId, company.vihbId)
            ?: company

    }

    fun updateTransport(id: UUID, request: CreateContainerTransportRequest): TransportDto {
        val transport = transportRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Transport with id $id not found") }

        val pickupLocation = findOrCreateLocation(
            AddressRequest(
                streetName = request.pickupStreet,
                buildingNumber = request.pickupHouseNumber,
                postalCode = request.pickupPostalCode,
                city = request.pickupCity
            )
        )

        val deliveryLocation = if (request.pickupPostalCode == request.deliveryPostalCode &&
            request.pickupHouseNumber == request.deliveryHouseNumber
        ) {
            pickupLocation
        } else {
            findOrCreateLocation(
                AddressRequest(
                    streetName = request.deliveryStreet,
                    buildingNumber = request.deliveryHouseNumber,
                    postalCode = request.deliveryPostalCode,
                    city = request.deliveryCity
                )
            )
        }

        val updatedTransport = transport.copy(
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            pickupLocation = pickupLocation,
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = request.deliveryDateTime,
            wasteContainer = request.containerId?.let { entityManager.getReference(WasteContainerDto::class.java, it) },
            transportType = request.transportType,
            truck = if (request.truckId?.isNotEmpty() ?: false) entityManager.getReference(Truck::class.java, request.truckId) else null,
            driver = request.driverId?.let { entityManager.getReference(ProfileDto::class.java, it) },
        )

        return transportRepository.save(updatedTransport)
    }

    fun findOrCreateLocation(address: AddressRequest) =
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
            daysInWeek.last().atTime(23, 59, 59))
            .filter { transport -> truckId == null || transport.truck?.licensePlate == truckId }
            .filter { transport -> driverId == null || transport.driver?.id == driverId }
            .filter { transport -> statuses.isEmpty() || statuses.contains(transport.getStatus().name) }

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

    fun deleteTransport(id: UUID) {
        transportRepository.deleteById(id)
    }

    fun getTransportById(id: UUID): TransportDto {
        return transportRepository.findByIdOrNull(id)
            ?: throw EntityNotFoundException("Transport with id $id not found")
    }
}