package nl.eazysoftware.eazyrecyclingservice.repository.address

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProjectLocationId
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyViewMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.hibernate.Hibernate
import org.springframework.stereotype.Component
import java.util.*

@Component
class PickupLocationMapper(
  private var entityManager: EntityManager,
) {

  fun toDomain(dto: PickupLocationDto): Location {
    return when (val unproxied = Hibernate.unproxy(dto)) {
      is PickupLocationDto.DutchAddressDto -> DutchAddress(
        address = Address(
          streetName = StreetName(unproxied.streetName),
          postalCode = DutchPostalCode(unproxied.postalCode),
          buildingNumber = unproxied.buildingNumber,
          buildingNumberAddition = unproxied.buildingNumberAddition,
          city = City(unproxied.city),
          country = unproxied.country,
        )
      )

      is PickupLocationDto.ProximityDescriptionDto -> ProximityDescription(
        description = unproxied.description,
        postalCodeDigits = unproxied.postalCode,
        city = City(unproxied.city),
        country = unproxied.country
      )

      is PickupLocationDto.PickupCompanyDto -> Company(
        companyId = CompanyId(unproxied.company.id!!),
        name = unproxied.company.name,
        address = Address(
          streetName = StreetName(unproxied.streetName),
          postalCode = DutchPostalCode(unproxied.postalCode),
          buildingNumber = unproxied.buildingNumber,
          buildingNumberAddition = unproxied.buildingNumberAddition,
          city = City(unproxied.city),
          country = unproxied.country,
        )
      )

      is PickupLocationDto.PickupProjectLocationDto -> ProjectLocationSnapshot(
        projectLocationId = ProjectLocationId(UUID.fromString(unproxied.id)),
        address = Address(
          streetName = StreetName(unproxied.streetName),
          postalCode = DutchPostalCode(unproxied.postalCode),
          buildingNumber = unproxied.buildingNumber,
          buildingNumberAddition = unproxied.buildingNumberAddition,
          city = City(unproxied.city),
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
      is DutchAddress -> mapAndSave(location)
      is ProximityDescription -> mapAndSave(location)
      is Company -> mapAndSave(location)
      is ProjectLocationSnapshot -> mapAndSave(location)
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
          id = dto.id,
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

  private fun mapAndSave(location: ProjectLocationSnapshot) = PickupLocationDto.PickupProjectLocationDto(
      id = location.projectLocationId.uuid.toString(),
      company = entityManager.getReference(CompanyDto::class.java, location.companyId.uuid),
      streetName = location.streetName(),
      buildingNumber = location.buildingNumber(),
      buildingNumberAddition = location.buildingNumberAddition(),
      postalCode = location.postalCode().value,
      city = location.city().value,
      country = location.country()
    )

  private fun mapAndSave(domain: ProximityDescription) = PickupLocationDto.ProximityDescriptionDto(
    description = domain.description,
    postalCode = domain.postalCodeDigits,
    city = domain.city.value,
    country = domain.country
  )

  private fun mapAndSave(address: DutchAddress) = PickupLocationDto.DutchAddressDto(
    streetName = address.streetName(),
    buildingNumber = address.buildingNumber(),
    buildingNumberAddition = address.buildingNumberAddition(),
    postalCode = address.postalCode().value,
    city = address.city(),
    country = address.country()
  )

  private fun mapAndSave(domain: Company) = PickupLocationDto.PickupCompanyDto(
    company = entityManager.getReference(CompanyDto::class.java, domain.companyId.uuid),
    name = domain.name,
    streetName = domain.address.streetName.value,
    buildingNumber = domain.address.buildingNumber,
    buildingNumberAddition = domain.address.buildingNumberAddition,
    postalCode = domain.address.postalCode.value,
    city = domain.address.city.value,
    country = domain.address.country,
  )

}
