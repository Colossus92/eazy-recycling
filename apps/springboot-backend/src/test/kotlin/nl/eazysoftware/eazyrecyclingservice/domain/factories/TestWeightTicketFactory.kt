package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.*
import java.util.*

object TestWeightTicketFactory {

    fun createTestWeightTicketRequest(
        carrierParty: UUID? = null,
        consignorCompanyId: UUID = UUID.randomUUID(),
        lines: List<WeightTicketLineRequest> = emptyList(),
        truckLicensePlate: String? = "AA-123-BB",
        reclamation: String? = "Test reclamation",
        note: String? = "Test note"
    ): WeightTicketRequest {
        return WeightTicketRequest(
            consignorParty = ConsignorRequest.CompanyConsignor(consignorCompanyId),
            tarraWeight = null,
            lines = lines,
            carrierParty = carrierParty,
            truckLicensePlate = truckLicensePlate,
            reclamation = reclamation,
            note = note
        )
    }

    fun createTestWeightTicketLine(
        wasteStreamNumber: String = "180101",
        weightValue: String = "100.50",
        weightUnit: WeightUnitRequest = WeightUnitRequest.KG
    ): WeightTicketLineRequest {
        return WeightTicketLineRequest(
            wasteStreamNumber = wasteStreamNumber,
            weight = WeightRequest(
                value = weightValue,
                unit = weightUnit
            )
        )
    }
}
