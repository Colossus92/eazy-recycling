package nl.eazysoftware.eazyrecyclingservice.repository.wastestream

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import kotlinx.datetime.toKotlinInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.*
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationRepository
import nl.eazysoftware.eazyrecyclingservice.config.clock.toJavaInstant
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.WasteDeliveryLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.EuralCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.ProcessingMethod
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteType
import org.hibernate.Hibernate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component

@Component
class WasteStreamMapper(
  @PersistenceContext private var entityManager: EntityManager,
  private var pickupLocationRepository: PickupLocationRepository,
  private var companyRepository: CompanyRepository,
) {

  fun toDomain(dto: WasteStreamDto): WasteStream {
    val processorPartyId = dto.processorParty.processorId
      ?.let { ProcessorPartyId(it) }
      ?: throw IllegalArgumentException("Verwerkersnummer is verplicht voor een geldig afvalstroomnummer, maar ${dto.processorParty.name} heeft geen vewerkersnummer")
    val actualPickupLocation = Hibernate.unproxy(dto.pickupLocation, PickupLocationDto::class.java)

    return WasteStream(
        wasteStreamNumber = WasteStreamNumber(dto.number),
        wasteType = WasteType(
            name = dto.name,
            euralCode = EuralCode(dto.euralCode.code),
            processingMethod = ProcessingMethod(dto.processingMethodCode.code)
        ),
        collectionType = WasteCollectionType.valueOf(dto.wasteCollectionType),
        pickupLocation = when (actualPickupLocation) {
            is PickupLocationDto.DutchAddressDto -> DutchAddress(
                streetName = actualPickupLocation.streetName,
                postalCode = DutchPostalCode(actualPickupLocation.postalCode),
                buildingNumber = actualPickupLocation.buildingNumber,
                buildingNumberAddition = actualPickupLocation.buildingNumberAddition,
                city = actualPickupLocation.city,
                country = actualPickupLocation.country,
            )

            is PickupLocationDto.ProximityDescriptionDto -> ProximityDescription(
                description = actualPickupLocation.description,
                postalCodeDigits = actualPickupLocation.postalCode,
                city = actualPickupLocation.city,
                country = actualPickupLocation.country
            )

            is PickupLocationDto.PickupCompanyDto -> Company(
                companyId = CompanyId(actualPickupLocation.companyId)
            )

            is PickupLocationDto.NoPickupLocationDto -> NoLocation
            else -> throw IllegalArgumentException("Ongeldige herkomstlocatie: ${actualPickupLocation::class.simpleName}")
        },
        deliveryLocation = WasteDeliveryLocation(
            processorPartyId = processorPartyId
        ),
        consignorParty = Consignor.Company(
            CompanyId(dto.consignorParty.id!!)
        ),
        pickupParty = CompanyId(dto.pickupParty.id!!),
        dealerParty = dto.dealerParty?.let { CompanyId(it.id!!) },
        collectorParty = dto.collectorParty?.let { CompanyId(it.id!!) },
        brokerParty = dto.brokerParty?.let { CompanyId(it.id!!) },
        lastActivityAt = dto.lastActivityAt.toKotlinInstant(),
        status = WasteStreamStatus.valueOf(dto.status)
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
        is Company -> createPickupCompany(location)
        is NoLocation -> PickupLocationDto.NoPickupLocationDto()
      },
      processorParty = companyRepository.findByProcessorId(domain.deliveryLocation.processorPartyId.number)
        ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: ${domain.deliveryLocation.processorPartyId.number}"),
      consignorParty = when (val consignor = domain.consignorParty) {
        is Consignor.Company -> entityManager.getReference(CompanyDto::class.java, consignor.id.uuid)
        is Consignor.Person -> throw IllegalArgumentException("Person consignor is not yet supported in persistence layer")
      },
      pickupParty = entityManager.getReference(CompanyDto::class.java, domain.pickupParty.uuid),
      dealerParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      collectorParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      brokerParty = domain.brokerParty?.let { entityManager.getReference(CompanyDto::class.java, it.uuid) },
      lastActivityAt = domain.lastActivityAt.toJavaInstant(),
      status = domain.status.name
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

  private fun findOrCreateLocation(address: DutchAddress): PickupLocationDto.DutchAddressDto {
    return pickupLocationRepository.findDutchAddressByPostalCodeAndBuildingNumber(
      address.postalCode.value,
      address.buildingNumber
    ) ?: run {
      val newLocation = PickupLocationDto.DutchAddressDto(
        streetName = address.streetName,
        buildingNumber = address.buildingNumber,
        buildingNumberAddition = address.buildingNumberAddition,
        postalCode = address.postalCode.value,
        city = address.city,
        country = address.country
      )
      pickupLocationRepository.save(newLocation)
    }
  }

  private fun createPickupCompany(
    domain: Company
  ): PickupLocationDto.PickupCompanyDto {
    val company = companyRepository.findByIdOrNull(domain.companyId.uuid)
      ?: throw IllegalArgumentException("Geen bedrijf gevonden met verwerkersnummer: ${domain.companyId}")

    return pickupLocationRepository.findCompanyByCompanyId(company.id) ?: run {
      pickupLocationRepository.save(PickupLocationDto.PickupCompanyDto(companyId = domain.companyId.uuid))
    }
  }
}
