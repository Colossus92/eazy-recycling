package nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.ConsignorClassification
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumberGenerator
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import org.springframework.stereotype.Component

@Component
class WasteStreamFactory(
  private val wasteStreams: WasteStreams,
  private val numberGenerator: WasteStreamNumberGenerator = WasteStreamNumberGenerator(),
  private val companies: Companies,
  private val projectLocations: ProjectLocations
) {

  fun createDraft(cmd: WasteStreamCommand): WasteStream {
    val processorId = cmd.deliveryLocation.processorPartyId
    val highestExisting = wasteStreams.findHighestNumberForProcessor(processorId)
    val wasteStreamNumber = numberGenerator.generateNext(processorId, highestExisting)

    // Convert command to domain Location using LocationFactory
    val pickupLocation = cmd.pickupLocation.toDomain(companies, projectLocations)

    return WasteStream(
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
      brokerParty = cmd.brokerParty,
      catalogItemId = cmd.catalogItemId
    )
  }

  fun updateExisting(wasteStreamNumber: WasteStreamNumber, cmd: WasteStreamCommand): WasteStream {
    val wasteStream = wasteStreams.findByNumber(wasteStreamNumber)
      ?: throw EntityNotFoundException("Afvalstroom met nummer ${wasteStreamNumber.number} bestaat niet")

    // Convert command to domain Location using LocationFactory
    val pickupLocation = cmd.pickupLocation.toDomain(companies, projectLocations)

    wasteStream.update(
      wasteType = cmd.wasteType,
      collectionType = cmd.collectionType,
      pickupLocation = pickupLocation,
      deliveryLocation = cmd.deliveryLocation,
      consignorParty = cmd.consignorParty,
      pickupParty = cmd.pickupParty,
      dealerParty = cmd.dealerParty,
      collectorParty = cmd.collectorParty,
      brokerParty = cmd.brokerParty,
      materialId = cmd.catalogItemId
    )

    return wasteStream
  }

}

/**
 * Extension function to convert PickupLocationCommand to domain Location.
 * Uses LocationFactory for PickupCompanyCommand to fetch address from database.
 */
fun PickupLocationCommand.toDomain(companies: Companies, projectLocations: ProjectLocations): Location {
  return when (this) {
    is PickupLocationCommand.DutchAddressCommand -> Location.DutchAddress(
      address = Address(
        streetName = StreetName(streetName),
        postalCode = DutchPostalCode(postalCode),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        city = City(city),
        country = country
      )
    )

    is PickupLocationCommand.ProximityDescriptionCommand -> Location.ProximityDescription(
      description = description,
      postalCodeDigits = postalCodeDigits,
      city = City(city),
      country = country
    )

    is PickupLocationCommand.ProjectLocationCommand -> projectLocations.findById(id)
      ?.toSnapshot()
      ?: throw EntityNotFoundException("Geen projectlocatie gevonden met id $id")

    is PickupLocationCommand.PickupCompanyCommand -> {
      val company = companies.findById(companyId) ?: throw EntityNotFoundException("Geen bedrijf gevonden voor bedrijf met id ${companyId.uuid}")
      Location.Company(
        companyId = companyId,
        name = company.name,
        address = company.address,
      )
    }

    is PickupLocationCommand.NoPickupLocationCommand -> Location.NoLocation
  }
}
