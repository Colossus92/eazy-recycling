package nl.eazysoftware.eazyrecyclingservice.repository.address

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.DutchAddress
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.NoLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.ProjectLocation
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.ProximityDescription
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PickupLocationMapper(
  private var pickupLocationRepository: PickupLocationRepository,
  private var companyRepository: CompanyRepository,
) {

  fun toDomain(dto: PickupLocationDto): Location {
    return when (dto) {
      is PickupLocationDto.DutchAddressDto -> DutchAddress(
        address = Address(
          streetName = dto.streetName,
          postalCode = DutchPostalCode(dto.postalCode),
          buildingNumber = dto.buildingNumber,
          buildingNumberAddition = dto.buildingNumberAddition,
          city = dto.city,
          country = dto.country,
        )
      )

      is PickupLocationDto.ProximityDescriptionDto -> ProximityDescription(
        description = dto.description,
        postalCodeDigits = dto.postalCode,
        city = dto.city,
        country = dto.country
      )

      is PickupLocationDto.PickupCompanyDto -> Company(
        companyId = CompanyId(dto.company.id!!)
      )

      is PickupLocationDto.PickupProjectLocationDto -> ProjectLocation(
        id = UUID.fromString(dto.id),
        address = Address(
          streetName = dto.streetName,
          postalCode = DutchPostalCode(dto.postalCode),
          buildingNumber = dto.buildingNumber,
          buildingNumberAddition = dto.buildingNumberAddition,
          city = dto.city,
          country = dto.country,
        ),
        companyId = CompanyId(dto.company.id!!)
      )

      is PickupLocationDto.NoPickupLocationDto -> NoLocation

      else -> throw IllegalArgumentException("Ongeldige herkomstlocatie: ${dto::class.simpleName}")
    }
  }

  fun toDto(location: Location): PickupLocationDto {
    return when (location) {
      is DutchAddress -> findOrCreateDutchAddress(location)
      is ProximityDescription -> createProximity(location)
      is Company -> createPickupCompany(location)
      is ProjectLocation -> findOrCreateProjectLocation(location)
      is NoLocation -> PickupLocationDto.NoPickupLocationDto()
    }
  }

  private fun findOrCreateProjectLocation(location: ProjectLocation): PickupLocationDto.PickupProjectLocationDto {
    // Try to find existing project location by ID
    val existingLocation = pickupLocationRepository.findByIdOrNull(location.id.toString())
    if (existingLocation is PickupLocationDto.PickupProjectLocationDto) {
      return existingLocation
    }

    // If not found, create and save new project location
    val company = companyRepository.findByIdOrNull(location.companyId.uuid)
      ?: throw IllegalArgumentException("Geen bedrijf gevonden met id: ${location.companyId}")

    val newLocation = PickupLocationDto.PickupProjectLocationDto(
      id = location.id.toString(),
      company = company,
      streetName = location.streetName(),
      buildingNumber = location.buildingNumber(),
      buildingNumberAddition = location.buildingNumberAddition(),
      postalCode = location.postalCode().value,
      city = location.city(),
      country = location.country()
    )
    
    return pickupLocationRepository.save(newLocation)
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

  private fun findOrCreateDutchAddress(address: DutchAddress): PickupLocationDto.DutchAddressDto {
    return pickupLocationRepository.findDutchAddressByPostalCodeAndBuildingNumber(
      address.postalCode().value,
      address.buildingNumber()
    ) ?: run {
      val newLocation = PickupLocationDto.DutchAddressDto(
        streetName = address.streetName(),
        buildingNumber = address.buildingNumber(),
        buildingNumberAddition = address.buildingNumberAddition(),
        postalCode = address.postalCode().value,
        city = address.city(),
        country = address.country()
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
      pickupLocationRepository.save(PickupLocationDto.PickupCompanyDto(company = company))
    }
  }

}
