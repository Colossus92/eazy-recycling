package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.waste.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateWasteStream {
  fun handle(cmd: WasteStreamCommand): CreateWasteStreamResult
}

/**
 * Command for creating a waste stream.
 * This follows the hexagonal architecture pattern where the command contains domain objects.
 */
data class WasteStreamCommand(
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
  private val numberGenerator: WasteStreamNumberGenerator = WasteStreamNumberGenerator(),
) : CreateWasteStream {

  @Transactional
  override fun handle(cmd: WasteStreamCommand): CreateWasteStreamResult {
    // Generate the next sequential waste stream number for this processor
    val processorId = cmd.deliveryLocation.processorPartyId
    val highestExisting = wasteStreamRepo.findHighestNumberForProcessor(processorId)
    val wasteStreamNumber = numberGenerator.generateNext(processorId, highestExisting)

    val wasteStream = WasteStream(
      wasteStreamNumber = wasteStreamNumber,
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

    wasteStreamRepo.save(wasteStream)

    return CreateWasteStreamResult(
      wasteStreamNumber = wasteStream.wasteStreamNumber
    )
  }
}
