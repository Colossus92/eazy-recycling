package nl.eazysoftware.eazyrecyclingservice.application.query.mappers

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.application.query.CompanyView
import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.util.*

@Component
class PickupLocationViewMapper(
  private val companyRepository: CompanyRepository,
  private val companyViewMapper: CompanyViewMapper,
) {

  fun mapLocation(location: Location): PickupLocationView {
    return when (location) {
      is Location.DutchAddress -> PickupLocationView.DutchAddressView(
        streetName = location.streetName(),
        postalCode = location.postalCode().value,
        buildingNumber = location.buildingNumber(),
        buildingNumberAddition = location.buildingNumberAddition(),
        city = location.city(),
        country = location.country()
      )

      is Location.ProximityDescription -> PickupLocationView.ProximityDescriptionView(
        postalCodeDigits = location.postalCodeDigits,
        city = location.city.value,
        description = location.description,
        country = location.country
      )

      is Location.Company -> {
        val id = location.companyId.uuid
        val company = companyRepository.findByIdOrNull(id)
          ?: throw EntityNotFoundException("Bedrijf met id $id niet gevonden")

        return PickupLocationView.PickupCompanyView(
          company = CompanyView(
              id = id,
              name = location.name,
              chamberOfCommerceId = company.chamberOfCommerceId,
              vihbId = company.vihbId,
              processorId = company.processorId,
              address = AddressView(
                  street = location.address.streetName.value,
                  houseNumber = location.address.buildingNumber,
                  houseNumberAddition = location.address.buildingNumberAddition,
                  postalCode = location.address.postalCode.value,
                  city = location.address.city.value,
                  country = location.address.country
              )
          )
        )
      }

      is Location.ProjectLocationSnapshot -> PickupLocationView.ProjectLocationView(
        id = location.projectLocationId.uuid.toString(),
        company = mapCompany(location.companyId.uuid),
        streetName = location.streetName(),
        postalCode = location.postalCode().value,
        buildingNumber = location.buildingNumber(),
        buildingNumberAddition = location.buildingNumberAddition(),
        city = location.city().value,
        country = location.country()
      )

      is Location.NoLocation -> PickupLocationView.NoPickupView()
    }
  }

  private fun mapCompany(companyId: UUID): CompanyView {
    val company = companyRepository.findByIdOrNull(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id $companyId niet gevonden")

    return companyViewMapper.map(company)
  }
}
