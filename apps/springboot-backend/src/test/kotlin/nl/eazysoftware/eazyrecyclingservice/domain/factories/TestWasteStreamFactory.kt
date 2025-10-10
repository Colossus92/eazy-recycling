package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto
import java.util.*

object TestWasteStreamFactory {
  fun createTestWasteStreamDto(
    number: String = "WS" + UUID.randomUUID().toString().substring(0, 8),
    name: String = "Test Waste Stream",
    euralCode: Eural = Eural(code = "20 01 01", description = "Paper and cardboard"),
    processingMethod: ProcessingMethodDto = ProcessingMethodDto(code = "R1", description = "Recycling"),
    wasteCollectionType: String = "CONTAINER",
    pickupLocation: PickupLocationDto = createTestPickupLocation(),
    processorPartyId: CompanyDto = createTestCompany(),
    consignorParty: CompanyDto = createTestCompany(),
    pickupParty: CompanyDto = createTestCompany(),
    dealerParty: CompanyDto? = null,
    collectorParty: CompanyDto? = null,
    brokerParty: CompanyDto? = null
  ): WasteStreamDto {
    return WasteStreamDto(
      number = number,
      name = name,
      euralCode = euralCode,
      processingMethodCode = processingMethod,
      wasteCollectionType = wasteCollectionType,
      pickupLocation = pickupLocation,
      processorParty = processorPartyId,
      consignorParty = consignorParty,
      pickupParty = pickupParty,
      dealerParty = dealerParty,
      collectorParty = collectorParty,
      brokerParty = brokerParty
    )
  }

  private fun createTestPickupLocation(): PickupLocationDto {
    return PickupLocationDto.DutchAddressDto(
      buildingNumber = "42",
      buildingNumberAddition = "A",
      postalCode = "1234AB",
      country = "Netherlands"
    )
  }

  private fun createTestCompany(): CompanyDto {
    return CompanyDto(
      id = UUID.randomUUID(),
      name = "Test Company",
      chamberOfCommerceId = "12345678",
      vihbId = "VIHB123",
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
