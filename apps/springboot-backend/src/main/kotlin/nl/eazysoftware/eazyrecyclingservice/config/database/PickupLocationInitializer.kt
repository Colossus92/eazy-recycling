package nl.eazysoftware.eazyrecyclingservice.config.database

import jakarta.persistence.EntityManager
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationType.NO_PICKUP
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Ensures the singleton NO_PICKUP location exists in the database.
 * This is required because multiple WeightTickets may reference the same NO_PICKUP location,
 * and Hibernate requires a single persistent entity instance to avoid merge conflicts.
 */
@Component
class PickupLocationInitializer(
  private val entityManager: EntityManager,
) {

  @EventListener(ApplicationReadyEvent::class)
  @Transactional
  fun initializePickupLocations() {
    // Check if NO_PICKUP location already exists
    val existingNoPickup = entityManager.find(PickupLocationDto.NoPickupLocationDto::class.java, NO_PICKUP)

    if (existingNoPickup == null) {
      // Create and persist the singleton NO_PICKUP location
      val noPickupLocation = PickupLocationDto.NoPickupLocationDto()
      entityManager.persist(noPickupLocation)
      entityManager.flush()
    }
  }
}
