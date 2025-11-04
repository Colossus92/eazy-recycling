package nl.eazysoftware.eazyrecyclingservice.repository.address

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PickupLocationRepository : JpaRepository<PickupLocationDto, String>
