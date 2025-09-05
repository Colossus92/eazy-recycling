package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface WasteContainerRepository: JpaRepository<WasteContainerDto, UUID> {

}