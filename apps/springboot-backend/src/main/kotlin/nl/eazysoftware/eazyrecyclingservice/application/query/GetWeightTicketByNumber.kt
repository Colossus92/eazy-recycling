package nl.eazysoftware.eazyrecyclingservice.application.query

interface GetWeightTicketByNumber {
    fun execute(weightTicketNumber: Long): WeightTicketDetailView?
}
