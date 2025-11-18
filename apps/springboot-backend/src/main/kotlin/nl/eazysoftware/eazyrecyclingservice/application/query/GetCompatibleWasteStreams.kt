package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber

interface GetCompatibleWasteStreams {
    fun execute(wasteStreamNumber: WasteStreamNumber): List<WasteStreamListView>
}
