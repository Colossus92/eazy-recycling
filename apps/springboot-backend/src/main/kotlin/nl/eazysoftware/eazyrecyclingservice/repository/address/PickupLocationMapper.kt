package nl.eazysoftware.eazyrecyclingservice.repository.address

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.hibernate.Hibernate
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.*

@Component
class PickupLocationMapper(
  private var pickupLocationRepository: PickupLocationRepository,
  private var companyRepository: CompanyRepository,
  private var entityManager: EntityManager,
) {

  fun toDomain(dto: PickupLocationDto): Location {
    return when (val unproxied = Hibernate.unproxy(dto)) {
      is PickupLocationDto.DutchAddressDto -> DutchAddress(
        address = Address(
          streetName = unproxied.streetName,
          postalCode = DutchPostalCode(unproxied.postalCode),
          buildingNumber = unproxied.buildingNumber,
          buildingNumberAddition = unproxied.buildingNumberAddition,
          city = unproxied.city,
          country = unproxied.country,
        )
      )

      is PickupLocationDto.ProximityDescriptionDto -> ProximityDescription(
        description = unproxied.description,
        postalCodeDigits = unproxied.postalCode,
        city = unproxied.city,
        country = unproxied.country
      )

      is PickupLocationDto.PickupCompanyDto -> Company(
        companyId = CompanyId(unproxied.company.id!!),
        name = unproxied.company.name,
        address = Address(
          streetName = unproxied.streetName,
          postalCode = DutchPostalCode(unproxied.postalCode),
          buildingNumber = unproxied.buildingNumber,
          buildingNumberAddition = unproxied.buildingNumberAddition,
          city = unproxied.city,
          country = unproxied.country,
        )
      )

      is PickupLocationDto.PickupProjectLocationDto -> ProjectLocation(
        id = UUID.fromString(unproxied.id),
        address = Address(
          streetName = unproxied.streetName,
          postalCode = DutchPostalCode(unproxied.postalCode),
          buildingNumber = unproxied.buildingNumber,
          buildingNumberAddition = unproxied.buildingNumberAddition,
          city = unproxied.city,
          country = unproxied.country,
        ),
        companyId = CompanyId(unproxied.company.id!!)
      )

      is PickupLocationDto.NoPickupLocationDto -> NoLocation

      else -> throw IllegalArgumentException("Ongeldige herkomstlocatie: ${dto::class.simpleName}")
    }
  }

  fun toDto(location: Location): PickupLocationDto {
    return when (location) {
      is DutchAddress -> findOrCreateDutchAddress(location)
      is ProximityDescription -> createProximity(location)
      is Company -> createCompany(location)
      is ProjectLocation -> findOrCreateProjectLocation(location)
      is NoLocation -> PickupLocationDto.NoPickupLocationDto()
    }
  }

  fun toView(location: PickupLocationDto): PickupLocationView? {
    return when (val dto = Hibernate.unproxy(location)) {
      is PickupLocationDto.DutchAddressDto ->
        PickupLocationView.DutchAddressView(
          streetName = dto.streetName,
          postalCode = dto.postalCode,
          buildingNumber = dto.buildingNumber,
          buildingNumberAddition = dto.buildingNumberAddition,
          city = dto.city,
          country = dto.country
        )

      is PickupLocationDto.ProximityDescriptionDto ->
        PickupLocationView.ProximityDescriptionView(
          postalCodeDigits = dto.postalCode,
          city = dto.city,
          description = dto.description,
          country = dto.country
        )

      is PickupLocationDto.PickupCompanyDto ->
        PickupLocationView.PickupCompanyView(
          company = CompanyViewMapper.map(dto.company)
        )


      is PickupLocationDto.PickupProjectLocationDto -> {
        PickupLocationView.ProjectLocationView(
          company = CompanyViewMapper.map(dto.company),
          streetName = dto.streetName,
          postalCode = dto.postalCode,
          buildingNumber = dto.buildingNumber,
          buildingNumberAddition = dto.buildingNumberAddition,
          city = dto.city,
          country = dto.country
        )
      }

      else -> null
    }
  }


  private fun findOrCreateProjectLocation(location: ProjectLocation): PickupLocationDto.PickupProjectLocationDto {
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
    val newLocation = PickupLocationDto.ProximityDescriptionDto(
      description = domain.description,
      postalCode = domain.postalCodeDigits,
      city = domain.city,
      country = domain.country
    )
    return pickupLocationRepository.save(newLocation)
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

  private fun createCompany(
    domain: Company
  ): PickupLocationDto.PickupCompanyDto {
    val saved = pickupLocationRepository.save(
      PickupLocationDto.PickupCompanyDto(
        company = entityManager.getReference(CompanyDto::class.java, domain.companyId.uuid),
        name = domain.name,
        streetName = domain.address.streetName,
        buildingNumber = domain.address.buildingNumber,
        buildingNumberAddition = domain.address.buildingNumberAddition,
        postalCode = domain.address.postalCode.value,
        city = domain.address.city,
        country = domain.address.country,
      )
    )
    pickupLocationRepository.flush()

    return saved
  }

}
