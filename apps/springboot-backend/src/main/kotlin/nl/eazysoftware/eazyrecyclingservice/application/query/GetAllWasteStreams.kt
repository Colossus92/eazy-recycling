package nl.eazysoftware.eazyrecyclingservice.application.query

interface GetAllWasteStreams {
    fun execute(): List<WasteStreamListView>
}
