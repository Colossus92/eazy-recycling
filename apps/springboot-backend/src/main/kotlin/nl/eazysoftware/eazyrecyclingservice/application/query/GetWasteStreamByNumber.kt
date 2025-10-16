package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber

interface GetWasteStreamByNumber {
    fun execute(wasteStreamNumber: WasteStreamNumber): WasteStreamDetailView?
}
