package nl.eazysoftware.springtemplate.repository.entity.truck

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "trucks")
data class Truck(
    @Id
    @Column(name = "license_plate", nullable = false)
    val licensePlate: String,

    @Column(nullable = false)
    val brand: String,

    @Column(nullable = false)
    val model: String,

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)