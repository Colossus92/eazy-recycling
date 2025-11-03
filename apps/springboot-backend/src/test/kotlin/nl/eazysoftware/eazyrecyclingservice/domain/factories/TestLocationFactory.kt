package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

object TestLocationFactory {

  fun createDutchAddress() = Location.DutchAddress(
    address = Address(
      streetName = StreetName("Stadstraat"),
      postalCode = DutchPostalCode("1234 AB"),
      buildingNumber = "2",
      city = City("Test City"),
    ),
  )

  fun createCompanyAddress(): Location.Company {
    val company = TestCompanyFactory.eazyRecycling()

    return Location.Company(
      companyId = CompanyId(company.id!!),
      name = company.name,
      address = Address(
        streetName = StreetName(company.address.streetName!!),
        postalCode = DutchPostalCode(company.address.postalCode),
        buildingNumber = company.address.buildingNumber,
        city = company.address.city?.let { City(it) }!!,
        country = company.address.country!!,
      ),
    )
  }

  fun createProximityDescription() =
    Location.ProximityDescription(
      "1234",
      City("Stad"),
      "Nabijheidsbeschrijving"
    )
}
