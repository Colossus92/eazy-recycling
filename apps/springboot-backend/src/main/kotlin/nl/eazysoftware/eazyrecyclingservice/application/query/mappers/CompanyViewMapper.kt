package nl.eazysoftware.eazyrecyclingservice.application.query.mappers

import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.application.query.CompanyView
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import org.springframework.stereotype.Component

@Component
class CompanyViewMapper {

  fun map(company: Company) = CompanyView(
    id = company.companyId.uuid,
    name = company.name,
    chamberOfCommerceId = company.chamberOfCommerceId,
    vihbId = company.vihbNumber?.value,
    processorId = company.processorId?.number,
    address = AddressView(
      street = company.address.streetName.value,
      houseNumber = company.address.buildingNumber,
      houseNumberAddition = company.address.buildingNumberAddition,
      postalCode = company.address.postalCode.value,
      city = company.address.city.value,
      country = company.address.country
    ),
    email = company.email?.value,
  )
}
