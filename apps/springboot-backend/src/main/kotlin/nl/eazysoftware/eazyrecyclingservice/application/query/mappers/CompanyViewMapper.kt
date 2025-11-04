package nl.eazysoftware.eazyrecyclingservice.application.query.mappers

import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.application.query.CompanyView
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.stereotype.Component

@Component
class CompanyViewMapper {

  fun map(company: CompanyDto) = CompanyView(
      id = company.id!!,
      name = company.name,
      chamberOfCommerceId = company.chamberOfCommerceId,
      vihbId = company.vihbId,
      processorId = company.processorId,
      address = AddressView(
        street = company.address.streetName ?: "Niet bekend",
        houseNumber = company.address.buildingNumber,
        houseNumberAddition = company.address.buildingName,
        postalCode = company.address.postalCode,
        city = company.address.city ?: "",
        country = company.address.country ?: "Nederland"
      )
    )
}
