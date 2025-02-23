package nl.eazysoftware.eazyrecyclingservice.repository.entity.truck

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "trucks")
data class Truck(
    @Id
    @Column(name = "license_plate", nullable = false)
    val licensePlate: String,

    @Column
    val brand: String? = null,

    @Column
    val model: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
) {
    @PrePersist
    fun prePersist() {
        updatedAt = LocalDateTime.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}
