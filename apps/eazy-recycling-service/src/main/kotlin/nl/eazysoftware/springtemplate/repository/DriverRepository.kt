package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.driver.Driver
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface DriverRepository: JpaRepository<Driver, UUID> {
}