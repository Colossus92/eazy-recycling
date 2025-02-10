package nl.eazysoftware.springtemplate.repository

import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime

@Entity
data class Truck(
    @Id
    val licensePlate: String,
    val brand: String,
    val model: String,
)
