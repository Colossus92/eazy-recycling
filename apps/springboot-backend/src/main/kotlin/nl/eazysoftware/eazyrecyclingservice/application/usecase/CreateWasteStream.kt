package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.*
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamMapper
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateWasteStream {
  fun handle(cmd: CreateWasteStreamCommand): CreateWasteStreamResult
}

/**
 * Command for creating a waste stream.
 * This follows the hexagonal architecture pattern where the command contains domain objects.
 */
data class CreateWasteStreamCommand(
  val wasteStreamNumber: WasteStreamNumber,
  val wasteType: WasteType,
  val collectionType: WasteCollectionType,
  val pickupLocation: PickupLocation,
  val deliveryLocation: DeliveryLocation,
  val consignorParty: Consignor,
  val pickupParty: CompanyId,
  val dealerParty: CompanyId?,
  val collectorParty: CompanyId?,
  val brokerParty: CompanyId?
)

data class CreateWasteStreamResult(
  val wasteStreamNumber: WasteStreamNumber
)

@Service
class CreateWasteStreamService(
  private val wasteStreamRepo: WasteStreams,
  private val wasteStreamMapper: WasteStreamMapper
) : CreateWasteStream {

  @Transactional
  override fun handle(cmd: CreateWasteStreamCommand): CreateWasteStreamResult {
    check(!wasteStreamRepo.existsById(cmd.wasteStreamNumber)) {
      "Afvalstroom met nummer ${cmd.wasteStreamNumber.number} bestaat al"
    }

    val wasteStream = WasteStream(
      wasteStreamNumber = cmd.wasteStreamNumber,
      wasteType = cmd.wasteType,
      collectionType = cmd.collectionType,
      pickupLocation = cmd.pickupLocation,
      deliveryLocation = cmd.deliveryLocation,
      consignorParty = cmd.consignorParty,
      pickupParty = cmd.pickupParty,
      dealerParty = cmd.dealerParty,
      collectorParty = cmd.collectorParty,
      brokerParty = cmd.brokerParty
    )

    val dto = wasteStreamMapper.toDto(wasteStream)
    wasteStreamRepo.save(dto)

    return CreateWasteStreamResult(
      wasteStreamNumber = wasteStream.wasteStreamNumber
    )
  }
}
