package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.weightticket.WeightTicketId

interface GetWeightTicketByNumber {
    fun execute(weightTicketid: WeightTicketId): WeightTicketDetailView?
}
