package nl.eazysoftware.eazyrecyclingservice.domain.factories

import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.ConsignorRequest
import nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web.WeightTicketRequest
import java.util.*

object TestWeightTicketFactory {

    fun createTestWeightTicketRequest(
        carrierParty: UUID? = null,
        consignorCompanyId: UUID = UUID.randomUUID(),
        truckLicensePlate: String? = "AA-123-BB",
        reclamation: String? = "Test reclamation",
        note: String? = "Test note"
    ): WeightTicketRequest {
        return WeightTicketRequest(
            carrierParty = carrierParty,
            consignorParty = ConsignorRequest.CompanyConsignor(consignorCompanyId),
            truckLicensePlate = truckLicensePlate,
            reclamation = reclamation,
            note = note
        )
    }
}
