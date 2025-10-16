package nl.eazysoftware.eazyrecyclingservice.domain.model.address

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId

/**
 * Waste can only be delivered to a Company that is registered as a Processor Party.
 */
data class WasteDeliveryLocation(
  val processorPartyId: ProcessorPartyId,
)
