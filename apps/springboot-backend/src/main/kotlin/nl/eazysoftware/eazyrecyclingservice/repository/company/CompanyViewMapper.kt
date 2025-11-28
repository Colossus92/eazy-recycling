package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.application.query.CompanyView
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto

object CompanyViewMapper {

  fun map(dto: CompanyDto): CompanyView {
    return CompanyView(
      id = dto.id,
      name = dto.name,
      chamberOfCommerceId = dto.chamberOfCommerceId,
      vihbId = dto.vihbId,
      processorId = dto.processorId,
      address = AddressView(
        street = dto.address.streetName,
        houseNumber = dto.address.buildingNumber,
        houseNumberAddition = null,
        postalCode = dto.address.postalCode,
        city = dto.address.city,
        country = dto.address.country
      ),
      email = dto.email
    )
  }
}
