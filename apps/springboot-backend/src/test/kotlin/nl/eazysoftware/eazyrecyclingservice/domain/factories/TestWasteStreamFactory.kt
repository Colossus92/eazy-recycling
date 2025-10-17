package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.ConsignorRequest
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.WasteStreamRequest
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.PickupLocationRequest
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory.createTestCompany
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteCollectionType
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamStatus
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import nl.eazysoftware.eazyrecyclingservice.repository.wastestream.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import java.time.Instant
import java.util.*

object TestWasteStreamFactory {

  fun randomWasteStreamNumber(): String {
    val chars = ('0'..'9')
    val sb = StringBuilder()
    repeat(7) {
        val randomChar = chars.random()
        sb.append(randomChar)
    }
    return "12345$sb"
  }

  fun createTestWasteStreamRequest(
    companyId: UUID,
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
  ): WasteStreamRequest {
    return WasteStreamRequest(
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
    euralCode: Eural = Eural(code = "16 01 17", description = "Paper and cardboard"),
    processingMethod: ProcessingMethodDto = ProcessingMethodDto(code = "A.01", description = "Recycling"),
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
      brokerParty = brokerParty,
      lastActivityAt = Instant.now(),
      status = WasteStreamStatus.DRAFT.name
    )
  }

  private fun createTestPickupLocationDto(): PickupLocationDto {
    return PickupLocationDto.DutchAddressDto(
      streetName = "Test Street",
      buildingNumber = "42",
      buildingNumberAddition = "A",
      postalCode = "1234AB",
      city = "Test City",
      country = "Netherlands"
    )
  }

  private fun createTestPickupLocation(): PickupLocationRequest {
    return PickupLocationRequest.DutchAddressRequest(
      streetName = "Test Street",
      buildingNumber = "42",
      buildingNumberAddition = "A",
      postalCode = "1234AB",
      city = "Test City",
      country = "Nederland"
    )
  }
}
