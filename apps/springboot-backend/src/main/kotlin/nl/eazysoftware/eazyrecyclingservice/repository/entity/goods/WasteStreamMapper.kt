package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import nl.eazysoftware.eazyrecyclingservice.domain.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.waste.*
import nl.eazysoftware.eazyrecyclingservice.domain.waste.PickupLocation.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationRepository
import org.springframework.stereotype.Component

@Component
class WasteStreamMapper(
  @PersistenceContext private var entityManager: EntityManager,
  private var pickupLocationRepository: PickupLocationRepository,
) {

  fun toDomain(dto: WasteStreamDto): WasteStream {
    return WasteStream(
      wasteStreamNumber = WasteStreamNumber(dto.number),
      wasteType = WasteType(
        name = dto.name,
        euralCode = EuralCode(dto.euralCode.code),
        processingMethod = ProcessingMethod("A01")
      ),
      collectionType = WasteCollectionType.valueOf(dto.wasteCollectionType),
      pickupLocation = when (val location = dto.pickupLocation) {
        is PickupLocationDto.DutchAddressDto -> DutchAddress(
          postalCode = DutchPostalCode(location.postalCode),
          buildingNumber = location.buildingNumber,
          buildingNumberAddition = location.buildingNumberAddition,
          country = location.country,
        )
        is PickupLocationDto.ProximityDescriptionDto -> ProximityDescription(
          description = location.description,
          postalCodeDigits = location.postalCode,
          city = location.city,
          country = location.country
        )
        is PickupLocationDto.NoPickupLocationDto -> NoPickupLocation
        else -> throw IllegalArgumentException("Ongeldige herkomstlocatie")
      },
      deliveryLocation = DeliveryLocation(
        processorPartyId = ProcessorPartyId(dto.processorPartyId),
      ),
      consignorParty = Consignor.Company(
        CompanyId(dto.consignorParty.id!!)
      ),
      pickupParty = CompanyId(dto.pickupParty.id!!),
      dealerParty = dto.dealerParty?.let { CompanyId(it.id!!) },
      collectorParty = dto.collectorParty?.let { CompanyId(it.id!!) },
      brokerParty = dto.brokerParty?.let { CompanyId(it.id!!) },
    )
  }

  fun toDto(domain: WasteStream): WasteStreamDto {
    return WasteStreamDto(
      number = domain.wasteStreamNumber.number,
      name = domain.wasteType.name,
      euralCode = entityManager.getReference(Eural::class.java, domain.wasteType.euralCode.code),
      processingMethodCode = entityManager.getReference(
        ProcessingMethodDto::class.java,
        domain.wasteType.processingMethod.code
      ),
      wasteCollectionType = domain.collectionType.name,
      pickupLocation = when (val location = domain.pickupLocation) {
        is DutchAddress -> findOrCreateLocation(location)
        is ProximityDescription -> createProximity(location)
        is NoPickupLocation -> PickupLocationDto.NoPickupLocationDto()
      },
      processorPartyId = domain.deliveryLocation.processorPartyId.number,
      consignorParty = entityManager.getReference(CompanyDto::class.java, domain.consignorParty),
      pickupParty = entityManager.getReference(CompanyDto::class.java, domain.pickupParty.uuid),
      dealerParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      collectorParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      brokerParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
    )
  }

  private fun createProximity(
    domain: ProximityDescription
  ): PickupLocationDto.ProximityDescriptionDto {
    return PickupLocationDto.ProximityDescriptionDto(
      description = domain.description,
      postalCode = domain.postalCodeDigits,
      city = domain.city,
      country = domain.country
    )
  }

  private fun findOrCreateLocation(address: DutchAddress) =
    pickupLocationRepository.findDutchAddressByPostalCodeAndBuildingNumber(
      address.postalCode.value,
      address.buildingNumber
    )
      ?: PickupLocationDto.DutchAddressDto(
        buildingNumber = address.buildingNumber,
        postalCode = address.postalCode.value,
        country = address.country
      )
}
