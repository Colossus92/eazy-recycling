package nl.eazysoftware.eazyrecyclingservice.application.query.mappers

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.application.query.CompanyView
import nl.eazysoftware.eazyrecyclingservice.application.query.PickupLocationView
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import org.springframework.stereotype.Component

@Component
class PickupLocationViewMapper(
  private val companies: Companies,
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
        val company = companies.findById(location.companyId)
          ?: throw EntityNotFoundException("Bedrijf met id $id niet gevonden")

        return PickupLocationView.PickupCompanyView(
          company = CompanyView(
              id = location.companyId.uuid,
              name = location.name,
              chamberOfCommerceId = company.chamberOfCommerceId,
              vihbId = company.vihbNumber?.value,
              processorId = company.processorId?.number,
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
        company = mapCompany(location.companyId),
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

  private fun mapCompany(companyId: CompanyId): CompanyView {
    val company = companies.findById(companyId)
      ?: throw EntityNotFoundException("Bedrijf met id $companyId niet gevonden")

    return companyViewMapper.map(company)
  }
}
