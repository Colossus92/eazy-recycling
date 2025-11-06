package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteType
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface CreateDraftWasteStream {
  fun handle(cmd: WasteStreamCommand): CreateWasteStreamResult
}

/**
 * Command for creating a waste stream.
 * This follows the hexagonal architecture pattern where the command contains primitive data.
 */
data class WasteStreamCommand(
  val wasteType: WasteType,
  val collectionType: WasteCollectionType,
  val pickupLocation: PickupLocationCommand,
  val deliveryLocation: WasteDeliveryLocation,
  val consignorParty: Consignor,
  val consignorClassification: Int,
  val pickupParty: CompanyId,
  val dealerParty: CompanyId?,
  val collectorParty: CompanyId?,
  val brokerParty: CompanyId?,
)

/**
 * Sealed interface for pickup location commands.
 * Only PickupCompanyCommand contains just the ID - the address will be fetched from the database.
 */
sealed interface PickupLocationCommand {
  data class DutchAddressCommand(
    val streetName: String,
    val buildingNumber: String,
    val buildingNumberAddition: String? = null,
    val postalCode: String,
    val city: String,
    val country: String = "Nederland"
  ) : PickupLocationCommand

  data class ProximityDescriptionCommand(
    val description: String,
    val postalCodeDigits: String,
    val city: String,
    val country: String
  ) : PickupLocationCommand

  data class ProjectLocationCommand(
    val id: UUID,
    val companyId: CompanyId,
    val streetName: String,
    val buildingNumber: String,
    val buildingNumberAddition: String?,
    val postalCode: String,
    val city: String,
    val country: String
  ) : PickupLocationCommand

  data class PickupCompanyCommand(
    val companyId: CompanyId
  ) : PickupLocationCommand

  data object NoPickupLocationCommand : PickupLocationCommand
}

data class CreateWasteStreamResult(
  val wasteStreamNumber: WasteStreamNumber
)

@Service
class CreateDraftWasteStreamService(
  private val wasteStreamFactory: WasteStreamFactory,
  private val wasteStreams: WasteStreams,
) : CreateDraftWasteStream {

  @Transactional
  override fun handle(cmd: WasteStreamCommand): CreateWasteStreamResult {
    val wasteStream = wasteStreamFactory.createDraft(cmd)

    wasteStreams.save(wasteStream)

    return CreateWasteStreamResult(
      wasteStreamNumber = wasteStream.wasteStreamNumber
    )
  }
}
