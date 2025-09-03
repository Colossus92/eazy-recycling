package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.CreateWasteTransportRequest
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyBranchRepository
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.LocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.LocationDto
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TransportService(
    private val transportRepository: TransportRepository,
    private val locationRepository: LocationRepository,
    private val companyRepository: CompanyRepository,
    private val companyBranchRepository: CompanyBranchRepository,
    private val entityManager: EntityManager,
    private val pdfGenerationClient: PdfGenerationClient,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun assignWaybillTransport(waybillId: UUID, licensePlate: String, driverId: UUID): TransportDto {
        val transport = transportRepository.findByGoods_Uuid(waybillId)
            ?: throw EntityNotFoundException("Waybill with id $waybillId not found")

        return transportRepository.save(
            transport.copy(
                truck = entityManager.getReference(Truck::class.java, licensePlate),
                driver = entityManager.getReference(ProfileDto::class.java, driverId),
            )
        )
    }

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
    }

    fun createContainerTransport(request: CreateContainerTransportRequest): TransportDto {
        return createContainerTransport(request = request, goods = null)
    }

    fun createWasteTransport(request: CreateWasteTransportRequest): TransportDto {
        val goodsItem = GoodsItemDto(
            wasteStreamNumber = request.wasteStreamNumber,
            netNetWeight = request.weight,
            unit = request.unit,
            quantity = request.quantity,
            euralCode = request.euralCode,
            name = request.goodsName,
            processingMethodCode = request.processingMethodCode,
        )

        val goods = GoodsDto(
            id = UUID.randomUUID().toString(),
            goodsItem = goodsItem,
            consignorClassification = request.consignorClassification,
            consigneeParty = entityManager.getReference(CompanyDto::class.java, UUID.fromString(request.consigneePartyId)),
            pickupParty = entityManager.getReference(CompanyDto::class.java, UUID.fromString(request.pickupPartyId)),
        )

        val wasteTransport = createContainerTransport(request = request, goods = goods)

        wasteTransport.id?.let { transportId ->
            pdfGenerationClient.triggerPdfGeneration(transportId, "empty")
        }

        return wasteTransport
    }

    private fun createContainerTransport(request: CreateContainerTransportRequest, goods: GoodsDto? = null): TransportDto {
        validateBranchCompanyRelationships(
            pickupBranchId = request.pickupCompanyBranchId,
            pickupCompanyId = request.pickupCompanyId,
            deliveryBranchId = request.deliveryCompanyBranchId,
            deliveryCompanyId = request.deliveryCompanyId
        )
        val pickupLocation = findOrCreateLocation(AddressRequest(
            streetName = request.pickupStreet,
            buildingNumber = request.pickupBuildingNumber,
            postalCode = request.pickupPostalCode,
            city = request.pickupCity
        ))

        // Check if delivery location is the same as pickup
        val deliveryLocation = if (isDeliveryAndPickupSameLocation(request)) {
            pickupLocation
        } else {
            findOrCreateLocation(AddressRequest(
                streetName = request.deliveryStreet,
                buildingNumber = request.deliveryBuildingNumber,
                postalCode = request.deliveryPostalCode,
                city = request.deliveryCity
            ))
        }

        // Create and return the transport
        val transport = TransportDto(
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            pickupCompany = request.pickupCompanyId?.let { entityManager.getReference(CompanyDto::class.java, it) },
            pickupCompanyBranch = request.pickupCompanyBranchId?.let { entityManager.getReference(CompanyBranchDto::class.java, it) },
            pickupLocation = pickupLocation,
            pickupDateTime = request.pickupDateTime,
            deliveryCompany = request.deliveryCompanyId?.let { entityManager.getReference(CompanyDto::class.java, it) },
            deliveryCompanyBranch = request.deliveryCompanyBranchId?.let { entityManager.getReference(CompanyBranchDto::class.java, it) },
            deliveryLocation = deliveryLocation,
            deliveryDateTime = request.deliveryDateTime,
            truck = request.truckId?.let { entityManager.getReference(Truck::class.java, it) },
            driver = request.driverId?.let { entityManager.getReference(ProfileDto::class.java, it) },
            containerOperation = request.containerOperation,
            transportType = request.transportType,
            wasteContainer = request.containerId?.let { entityManager.getReference(WasteContainerDto::class.java, it) },
            carrierParty = entityManager.getReference(CompanyDto::class.java, request.carrierPartyId),
            goods = goods,
            note = request.note,
            sequenceNumber = 9999,
        )

        log.info("Creating transport: $transport")
        return transportRepository.save(transport)
    }

    /**
     * Validates that branch IDs belong to their respective company IDs
     * 
     * @param pickupBranchId The ID of the pickup branch
     * @param pickupCompanyId The ID of the pickup company
     * @param deliveryBranchId The ID of the delivery branch
     * @param deliveryCompanyId The ID of the delivery company
     * @throws EntityNotFoundException If a branch is not found
     * @throws IllegalArgumentException If a branch doesn't belong to the specified company
     */
    private fun validateBranchCompanyRelationships(
        pickupBranchId: UUID?,
        pickupCompanyId: UUID?,
        deliveryBranchId: UUID?,
        deliveryCompanyId: UUID?
    ) {
        validateBranchCompanyRelationShip(pickupBranchId, pickupCompanyId)
        validateBranchCompanyRelationShip(deliveryBranchId, deliveryCompanyId)
    }

    private fun validateBranchCompanyRelationShip(companyBranchId: UUID?, companyId: UUID?) {
        if (companyBranchId != null && companyId != null) {
            val companyBranch = companyBranchRepository.findById(companyBranchId)
            if (companyBranch.isEmpty) {
                throw EntityNotFoundException("Vestiging met id $companyBranchId niet gevonden")
            }

            if (companyBranch.get().company.id != companyId) {
                throw IllegalArgumentException("Vestiging met id $companyBranchId is niet van bedrijf met id $companyId")
            }
        }
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

    fun updateContainerTransport(id: UUID, request: CreateContainerTransportRequest): TransportDto {
        return transportRepository.save(getUpdatedTransport(id, request))
    }

    @Transactional
    fun updateWasteTransport(id: UUID, request: CreateWasteTransportRequest): TransportDto {
        var transport = getUpdatedTransport(id, request)

        // Update goods information if it exists
        val updatedGoods = transport.goods?.let { existingGoods ->
            val goodsItem = existingGoods.goodsItem.copy(
                wasteStreamNumber = request.wasteStreamNumber,
                netNetWeight = request.weight,
                unit = request.unit,
                quantity = request.quantity,
                euralCode = request.euralCode,
                name = request.goodsName,
                processingMethodCode = request.processingMethodCode
            )

            existingGoods.copy(
                consignorClassification = request.consignorClassification,
                goodsItem = goodsItem,
                consigneeParty = entityManager.getReference(CompanyDto::class.java, UUID.fromString(request.consigneePartyId)),
                pickupParty = entityManager.getReference(CompanyDto::class.java, UUID.fromString(request.pickupPartyId))
            )
        } ?: GoodsDto(
            id = UUID.randomUUID().toString(),
            uuid = UUID.randomUUID(),
            goodsItem = GoodsItemDto(
                wasteStreamNumber = request.wasteStreamNumber,
                netNetWeight = request.weight,
                unit = request.unit,
                quantity = request.quantity,
                euralCode = request.euralCode,
                name = request.goodsName,
                processingMethodCode = request.processingMethodCode,
            ),
            consignorClassification = request.consignorClassification,
            consigneeParty = entityManager.getReference(CompanyDto::class.java, request.consigneePartyId),
            pickupParty = entityManager.getReference(CompanyDto::class.java, request.pickupPartyId)
        )

        transport = transport.copy(goods = updatedGoods)

        return transportRepository.save(transport)
    }

    fun deleteTransport(id: UUID) {
        transportRepository.deleteById(id)
    }

    fun getTransportById(id: UUID): TransportDto {
        return transportRepository.findByIdOrNull(id)
            ?: throw EntityNotFoundException("Transport with id $id not found")
    }

    private fun findOrCreateLocation(address: AddressRequest): LocationDto {
        return locationRepository.findByAddress_PostalCodeAndAddress_BuildingNumber(address.postalCode, address.buildingNumber)
            ?: LocationDto(
                address = AddressDto(
                    streetName = address.streetName,
                    buildingNumber = address.buildingNumber,
                    postalCode = address.postalCode,
                    city = address.city,
                    country = address.country
                ),
                id = UUID.randomUUID().toString()
            ).let { locationRepository.save(it) }
    }


    private fun findCompany(company: CompanyDto): CompanyDto {
        return companyRepository.findByChamberOfCommerceIdAndVihbId(company.chamberOfCommerceId, company.vihbId)
            ?: company
    }

    private fun getUpdatedTransport(id: UUID, request: CreateContainerTransportRequest): TransportDto {
        validateBranchCompanyRelationships(request.pickupCompanyBranchId, request.pickupCompanyId, request.deliveryCompanyBranchId, request.deliveryCompanyId)
        val transport = transportRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Transport met id $id is niet gevonden") }

        if (transport.getStatus() === TransportDto.Status.FINISHED) {
            throw IllegalStateException("Transport is gereed gemeld en kan niet meer worden aangepast.")
        }

        val pickupLocation = findOrCreateLocation(
            AddressRequest(
                streetName = request.pickupStreet,
                buildingNumber = request.pickupBuildingNumber,
                postalCode = request.pickupPostalCode,
                city = request.pickupCity
            )
        )

        val deliveryLocation = if (isDeliveryAndPickupSameLocation(request)
        ) {
            pickupLocation
        } else {
            findOrCreateLocation(
                AddressRequest(
                    streetName = request.deliveryStreet,
                    buildingNumber = request.deliveryBuildingNumber,
                    postalCode = request.deliveryPostalCode,
                    city = request.deliveryCity
                )
            )
        }

        val updatedTransport = transport.copy(
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            carrierParty = entityManager.getReference(CompanyDto::class.java, request.carrierPartyId),
            pickupCompany = request.pickupCompanyId?.let { entityManager.getReference(CompanyDto::class.java, request.pickupCompanyId)},
            pickupCompanyBranch = request.pickupCompanyBranchId?.let { entityManager.getReference(CompanyBranchDto::class.java, request.pickupCompanyBranchId)},
            pickupLocation = pickupLocation,
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = deliveryLocation,
            deliveryDateTime = request.deliveryDateTime,
            deliveryCompany = request.deliveryCompanyId?.let { entityManager.getReference(CompanyDto::class.java, request.deliveryCompanyId) },
            deliveryCompanyBranch = request.deliveryCompanyBranchId?.let { entityManager.getReference(CompanyBranchDto::class.java, request.deliveryCompanyBranchId) },
            wasteContainer = request.containerId?.let { entityManager.getReference(WasteContainerDto::class.java, it) },
            transportType = request.transportType,
            truck = if (request.truckId?.isNotEmpty() ?: false) entityManager.getReference(Truck::class.java, request.truckId) else null,
            driver = request.driverId?.let { entityManager.getReference(ProfileDto::class.java, it) },
            containerOperation = request.containerOperation,
            note = request.note
        )

        return updatedTransport
    }

    private fun isDeliveryAndPickupSameLocation(request: CreateContainerTransportRequest): Boolean =
        request.pickupPostalCode == request.deliveryPostalCode && request.pickupBuildingNumber == request.deliveryBuildingNumber

    fun markTransportAsFinished(id: UUID, transportHours: Double): TransportDto {
        return transportRepository.findById(id)
            .orElseThrow { EntityNotFoundException("Transport met id $id niet gevonden") }
            .let {
                transportRepository.save(it.copy(transportHours = transportHours))
            }
    }
}