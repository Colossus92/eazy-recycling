package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.WasteStreamNumber

/**
 * Query interface for retrieving weight tickets that have a line for a specific waste stream.
 */
interface GetWeightTicketsByWasteStream {
    fun execute(wasteStreamNumber: WasteStreamNumber): List<WeightTicketsByWasteStreamView>
}
