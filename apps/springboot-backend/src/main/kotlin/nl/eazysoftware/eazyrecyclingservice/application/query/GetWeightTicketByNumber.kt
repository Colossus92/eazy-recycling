package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicketId

interface GetWeightTicketByNumber {
    fun execute(weightTicketid: WeightTicketId): WeightTicketDetailView?
}
