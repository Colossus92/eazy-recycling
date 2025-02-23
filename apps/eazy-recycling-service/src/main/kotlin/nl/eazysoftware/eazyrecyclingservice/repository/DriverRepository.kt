package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.driver.Driver
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DriverRepository: JpaRepository<Driver, UUID> {
}