package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWasteStreams
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWasteStreamByNumber
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamListView
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import org.springframework.stereotype.Service

@Service
class WasteStreamService(
    private val getAllWasteStreams: GetAllWasteStreams,
    private val getWasteStreamByNumber: GetWasteStreamByNumber
) {

    fun getWasteStreams(): List<WasteStreamListView> {
        return getAllWasteStreams.execute()
    }

    fun getWasteStreamByNumber(wasteStreamNumber: WasteStreamNumber): WasteStreamDetailView? {
        return getWasteStreamByNumber.execute(wasteStreamNumber)
    }

}
