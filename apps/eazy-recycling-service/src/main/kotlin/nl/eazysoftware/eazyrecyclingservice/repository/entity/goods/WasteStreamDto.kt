package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Entity
@Table(name = "waste_stream")
data class WasteStreamDto(
    @Id
    @Column(name = "number")
    @field:Size(min = 12, max = 12)
    val number: String,

    @Column(name = "name")
    @field:NotBlank
    val name: String,
)
