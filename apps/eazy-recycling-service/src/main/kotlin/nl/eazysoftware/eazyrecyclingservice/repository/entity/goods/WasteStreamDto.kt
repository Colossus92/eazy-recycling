package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "waste_stream")
data class WasteStreamDto(
    @Id
    @Column(name = "number")
    val number: String,
    
    @Column(name = "name")
    val name: String,
)
