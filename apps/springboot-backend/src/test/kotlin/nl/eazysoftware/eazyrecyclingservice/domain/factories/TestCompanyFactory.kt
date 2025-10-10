package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.util.UUID

object TestCompanyFactory {


  fun createTestCompany(
    id: UUID? = null,  // null = let JPA generate it
    processorId: String = "12345"
  ): CompanyDto {
    return CompanyDto(
      id = id,
      name = "Test Company",
      chamberOfCommerceId = "12345678",
      vihbId = "VIHB123",
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
