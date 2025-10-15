package nl.eazysoftware.eazyrecyclingservice.application.query

interface GetAllWeightTickets {
    fun execute(): List<WeightTicketListView>
}
