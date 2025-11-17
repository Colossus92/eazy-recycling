package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllWasteStreams
import nl.eazysoftware.eazyrecyclingservice.application.query.GetWasteStreamByNumber
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamDetailView
import nl.eazysoftware.eazyrecyclingservice.application.query.WasteStreamListView
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber
import org.springframework.stereotype.Service
import java.util.*

@Service
class WasteStreamService(
    private val getAllWasteStreams: GetAllWasteStreams,
    private val getWasteStreamByNumber: GetWasteStreamByNumber
) {

    fun getWasteStreams(consignor: UUID?): List<WasteStreamListView> {
        return getAllWasteStreams.execute(consignor)
    }

    fun getWasteStreamByNumber(wasteStreamNumber: WasteStreamNumber): WasteStreamDetailView {
        return getWasteStreamByNumber.execute(wasteStreamNumber)
          ?: throw EntityNotFoundException("Afvalstroom met nummer $wasteStreamNumber niet gevonden")
    }

}
