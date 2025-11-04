package nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer

interface WasteContainers {

  fun findAll(): List<WasteContainer>

  fun findById(id: String): WasteContainer?

  fun deleteById(id: String)

  fun save(container: WasteContainer)
}
