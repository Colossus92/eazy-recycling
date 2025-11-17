package nl.eazysoftware.eazyrecyclingservice.application.query

import java.util.*

interface GetAllWasteStreams {
    fun execute(consignor: UUID? = null): List<WasteStreamListView>
}
