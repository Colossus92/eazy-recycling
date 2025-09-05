package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "processing_methods")
data class ProcessingMethod(
    @Id
    val code: String,
    val description: String,
)
