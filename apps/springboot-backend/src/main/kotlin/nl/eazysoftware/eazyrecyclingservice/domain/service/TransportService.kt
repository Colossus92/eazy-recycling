package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.transport.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.transport.CreateWasteTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.LocationFactory
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.GoodsItemDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import nl.eazysoftware.eazyrecyclingservice.repository.entity.user.ProfileDto
import org.slf4j.LoggerFactory
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class TransportService(
  private val transportRepository: TransportRepository,
  private val companyBranchRepository: ProjectLocations,
  private val entityManager: EntityManager,
  private val pdfGenerationClient: PdfGenerationClient,
  private val pickupLocationMapper: PickupLocationMapper,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun getAllTransports(): List<TransportDto> {
        return transportRepository.findAll()
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
      // Create and return the transport
        val transport = TransportDto(
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            pickupLocation = pickupLocationMapper.toDto(getPickupLocation(request)),
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = pickupLocationMapper.toDto(getDeliveryLocation(request)),
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
              ?: throw EntityNotFoundException("Vestiging met id $companyBranchId niet gevonden")

            if (companyBranch.companyId.uuid != companyId) {
                throw IllegalArgumentException("Vestiging met id $companyBranchId is niet van bedrijf met id $companyId")
            }
        }
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

    private fun getUpdatedTransport(id: UUID, request: CreateContainerTransportRequest): TransportDto {
        validateBranchCompanyRelationships(request.pickupCompanyBranchId, request.pickupCompanyId, request.deliveryCompanyBranchId, request.deliveryCompanyId)
        val transport = transportRepository.findById(id)
        .orElseThrow { EntityNotFoundException("Transport met id $id is niet gevonden") }

        if (transport.getStatus() === TransportDto.Status.FINISHED) {
            throw IllegalStateException("Transport is gereed gemeld en kan niet meer worden aangepast.")
        }

      val updatedTransport = transport.copy(
            consignorParty = entityManager.getReference(CompanyDto::class.java, request.consignorPartyId),
            carrierParty = entityManager.getReference(CompanyDto::class.java, request.carrierPartyId),
            pickupLocation = pickupLocationMapper.toDto(getPickupLocation(request)),
            pickupDateTime = request.pickupDateTime,
            deliveryLocation = pickupLocationMapper.toDto(getDeliveryLocation(request)),
            deliveryDateTime = request.deliveryDateTime,
            wasteContainer = request.containerId?.let { entityManager.getReference(WasteContainerDto::class.java, it) },
            transportType = request.transportType,
            truck = if (request.truckId?.isNotEmpty() ?: false) entityManager.getReference(Truck::class.java, request.truckId) else null,
            driver = request.driverId?.let { entityManager.getReference(ProfileDto::class.java, it) },
            containerOperation = request.containerOperation,
            note = request.note
        )

        return updatedTransport
    }

  private fun getPickupLocation(request: CreateContainerTransportRequest): Location = LocationFactory.create(
    companyId = request.pickupCompanyId?.let { CompanyId(it) },
    streetName = request.pickupStreet,
    buildingNumber = request.pickupBuildingNumber,
    buildingNumberAddition = request.pickupBuildingNumberAddition,
    postalCode = request.pickupPostalCode,
    description = request.pickupDescription,
    city = request.pickupCity
  )

  private fun getDeliveryLocation(request: CreateContainerTransportRequest): Location {
    val deliveryLocation = LocationFactory.create(
      companyId = request.deliveryCompanyId?.let { CompanyId(it) },
      streetName = request.deliveryStreet,
      buildingNumber = request.deliveryBuildingNumber,
      buildingNumberAddition = request.deliveryBuildingNumberAddition,
      postalCode = request.deliveryPostalCode,
      description = request.deliveryDescription,
      city = request.deliveryCity
    )
    return deliveryLocation
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
