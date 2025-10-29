package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface CreateWasteStream {
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
class CreateWasteStreamService(
  private val wasteStreamRepo: WasteStreams,
  private val companyService: CompanyService,
  private val numberGenerator: WasteStreamNumberGenerator = WasteStreamNumberGenerator(),
) : CreateWasteStream {

  @Transactional
  override fun handle(cmd: WasteStreamCommand): CreateWasteStreamResult {
    // Generate the next sequential waste stream number for this processor
    val processorId = cmd.deliveryLocation.processorPartyId
    val highestExisting = wasteStreamRepo.findHighestNumberForProcessor(processorId)
    val wasteStreamNumber = numberGenerator.generateNext(processorId, highestExisting)

    // Convert command to domain Location using LocationFactory
    val pickupLocation = cmd.pickupLocation.toDomain(companyService)

    val wasteStream = WasteStream(
      wasteStreamNumber = wasteStreamNumber,
      wasteType = cmd.wasteType,
      collectionType = cmd.collectionType,
      pickupLocation = pickupLocation,
      deliveryLocation = cmd.deliveryLocation,
      consignorParty = cmd.consignorParty,
      consignorClassification = ConsignorClassification.fromCode(cmd.consignorClassification),
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

/**
 * Extension function to convert PickupLocationCommand to domain Location.
 * Uses LocationFactory for PickupCompanyCommand to fetch address from database.
 */
fun PickupLocationCommand.toDomain(companyService: CompanyService): Location {
  return when (this) {
    is PickupLocationCommand.DutchAddressCommand -> Location.DutchAddress(
      address = Address(
        streetName = streetName,
        postalCode = DutchPostalCode(postalCode),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        city = city,
        country = country
      )
    )

    is PickupLocationCommand.ProximityDescriptionCommand -> Location.ProximityDescription(
      description = description,
      postalCodeDigits = postalCodeDigits,
      city = city,
      country = country
    )

    is PickupLocationCommand.ProjectLocationCommand -> Location.ProjectLocation(
      id = id,
      companyId = companyId,
      address = Address(
        streetName = streetName,
        postalCode = DutchPostalCode(postalCode),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        city = city,
        country = country
      )
    )

    is PickupLocationCommand.PickupCompanyCommand -> {
      val company = companyService.findById(companyId.uuid.toString())
      Location.Company(
        companyId = companyId,
        name = company.name,
        address = Address(
          streetName = company.address.streetName ?: throw IllegalStateException("Het bedrijf heet geen straatnaam, dit is verplicth"),
          postalCode = DutchPostalCode(company.address.postalCode),
          buildingNumber = company.address.buildingNumber,
          buildingNumberAddition = company.address.buildingName,
          city = company.address.city ?: throw IllegalStateException("Het bedrijf heet geen stad, dit is verplicth"),
          country = company.address.country ?: "Nederland"
        ),
      )
    }

    is PickupLocationCommand.NoPickupLocationCommand -> Location.NoLocation
  }
}
