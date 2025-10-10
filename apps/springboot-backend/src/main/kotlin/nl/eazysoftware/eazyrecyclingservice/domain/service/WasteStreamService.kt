package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWasteStreams
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamListView
import org.springframework.stereotype.Service

@Service
class WasteStreamService(
    private val getAllWasteStreams: GetAllWasteStreams
) {

    fun getWasteStreams(): List<WasteStreamListView> {
        return getAllWasteStreams.execute()
    }

}
