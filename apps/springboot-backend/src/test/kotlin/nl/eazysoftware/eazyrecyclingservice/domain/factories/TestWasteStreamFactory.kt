package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.ConsignorRequest
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.CreateWasteStreamRequest
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory.createTestCompany
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.PickupLocationDto
import java.util.*

object TestWasteStreamFactory {

  fun createTestWasteStreamRequest(
    companyId: UUID,
    number: String = "123456789012",
    name: String = "Test Waste Stream",
    euralCode: String = "16 01 17",
    processingMethod: String = "A.01",
    wasteCollectionType: String = WasteCollectionType.DEFAULT.name,
    pickupLocation: PickupLocationRequest = createTestPickupLocation(),
    processorPartyId: String = "12345",
    consignorParty: ConsignorRequest.CompanyConsignor = ConsignorRequest.CompanyConsignor(companyId),
    pickupParty: UUID = companyId,
    dealerParty: UUID? = null,
    collectorParty: UUID? = null,
    brokerParty: UUID? = null
  ): CreateWasteStreamRequest {
    return CreateWasteStreamRequest(
      wasteStreamNumber = number,
      name = name,
      euralCode = euralCode,
      processingMethodCode = processingMethod,
      collectionType = wasteCollectionType,
      pickupLocation = pickupLocation,
      processorPartyId = processorPartyId,
      consignorParty = consignorParty,
      pickupParty = pickupParty,
      dealerParty = dealerParty,
      collectorParty = collectorParty,
      brokerParty = brokerParty
    )
  }

  fun createTestWasteStreamDto(
    number: String = "WS" + UUID.randomUUID().toString().substring(0, 8),
    name: String = "Test Waste Stream",
    euralCode: Eural = Eural(code = "20 01 01", description = "Paper and cardboard"),
    processingMethod: ProcessingMethodDto = ProcessingMethodDto(code = "R1", description = "Recycling"),
    wasteCollectionType: String = "CONTAINER",
    pickupLocation: PickupLocationDto = createTestPickupLocationDto(),
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

  private fun createTestPickupLocationDto(): PickupLocationDto {
    return PickupLocationDto.DutchAddressDto(
      buildingNumber = "42",
      buildingNumberAddition = "A",
      postalCode = "1234AB",
      country = "Netherlands"
    )
  }

  private fun createTestPickupLocation(): PickupLocationRequest {
    return PickupLocationRequest.DutchAddressRequest(
      buildingNumber = "42",
      buildingNumberAddition = "A",
      postalCode = "1234AB",
      country = "Nederland"
    )
  }
}
