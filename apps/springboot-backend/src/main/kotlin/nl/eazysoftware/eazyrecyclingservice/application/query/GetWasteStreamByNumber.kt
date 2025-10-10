package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber

interface GetWasteStreamByNumber {
    fun execute(wasteStreamNumber: WasteStreamNumber): WasteStreamDetailView?
}
