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
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            pickupLocation = pickupLocation,
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = request.deliveryDateTime,
            truck = entityManager.getReference(Truck::class.java, request.truckId),
            driver = entityManager.getReference(ProfileDto::class.java, request.driverId),
            transportType = request.typeOfTransport,
            wasteContainer = entityManager.getReference(WasteContainerDto::class.java, request.containerId),
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
            wasteContainer = entityManager.getReference(WasteContainerDto::class.java, request.containerId),
            transportType = request.typeOfTransport,
            truck = entityManager.getReference(Truck::class.java, request.truckId),
            driver = entityManager.getReference(ProfileDto::class.java, request.driverId),
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
}