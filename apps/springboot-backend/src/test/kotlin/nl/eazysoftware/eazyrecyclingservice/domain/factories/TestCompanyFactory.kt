package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import java.util.*

object TestCompanyFactory {


  fun createTestCompany(
    id: UUID = UUID.randomUUID(),
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
        buildingNumberAddition = "Test Building",
        buildingNumber = "123",
        postalCode = "1234AB",
        city = "Test City",
        country = "Netherlands"
      )
    )
  }

  fun eazyRecycling() = CompanyDto(
      id = UUID.fromString("6a683b2a-96d6-454c-8cae-4a7e2a03f249"),
      name = "Eazy Recycling",
      chamberOfCommerceId = "85217463",
      vihbId = "123456VIXX",
      processorId = "11987",
      address = AddressDto(
        streetName = "Straat",
        buildingNumberAddition = null,
        buildingNumber = "8",
        postalCode = "1234 AB",
        city = "Rotterdam",
        country = "Nederland"
      )
    )
}
