package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.util.UUID

object TestCompanyFactory {


  fun createTestCompany(
    id: UUID? = null,
    chamberOfCommerceId: String? = "12345678",
    vihbId: String? = "987654VIHB",
    processorId: String? = "12345",
    name: String = "Test Company"
  ): CompanyDto {
    return CompanyDto(
      id = id,
      name = name,
      chamberOfCommerceId = chamberOfCommerceId,
      vihbId = vihbId,
      processorId = processorId,
      address = AddressDto(
        streetName = "Test Street",
        buildingName = "Test Building",
        buildingNumber = "123",
        postalCode = "1234AB",
        city = "Test City",
        country = "Netherlands"
      )
    )
  }
}
