package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.*
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketDirection
import java.time.LocalDateTime
import java.util.*

object TestWeightTicketFactory {

  fun createTestWeightTicketRequest(
    carrierParty: UUID? = null,
    consignorCompanyId: UUID = UUID.randomUUID(),
    lines: List<WeightTicketLineRequest> = emptyList(),
    productLines: List<WeightTicketProductLineRequest> = emptyList(),
    truckLicensePlate: String? = "AA-123-BB",
    reclamation: String? = "Test reclamation",
    note: String? = "Test note",
    weightedAt: LocalDateTime? = LocalDateTime.of(2025, 11, 21, 10, 0),
  ): WeightTicketRequest {
    return WeightTicketRequest(
      consignorParty = ConsignorRequest.CompanyConsignor(consignorCompanyId),
      direction = WeightTicketDirection.INBOUND,
      pickupLocation = null,
      deliveryLocation = null,
      secondWeighingValue = null,
      secondWeighingUnit = null,
      tarraWeightValue = null,
      tarraWeightUnit = null,
      lines = lines,
      productLines = productLines,
      carrierParty = carrierParty,
      truckLicensePlate = truckLicensePlate,
      reclamation = reclamation,
      note = note,
      weightedAt = weightedAt,
    )
  }

  fun createTestWeightTicketLine(
    wasteStreamNumber: String = "180101",
    weightValue: String = "100.50",
    weightUnit: WeightUnitRequest = WeightUnitRequest.KG,
    catalogItemId: UUID = UUID.randomUUID()
  ): WeightTicketLineRequest {
    return WeightTicketLineRequest(
      wasteStreamNumber = wasteStreamNumber,
      catalogItemId = catalogItemId,
      weight = WeightRequest(
        value = weightValue,
        unit = weightUnit
      )
    )
  }

  fun createTestWeightTicketProductLine(
    catalogItemId: UUID = UUID.randomUUID(),
    quantity: String = "10.00",
    unit: String = "st"
  ): WeightTicketProductLineRequest {
    return WeightTicketProductLineRequest(
      catalogItemId = catalogItemId,
      quantity = quantity,
      unit = unit
    )
  }
}
