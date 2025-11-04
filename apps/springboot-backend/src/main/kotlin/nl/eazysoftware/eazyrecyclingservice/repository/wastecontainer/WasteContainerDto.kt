package nl.eazysoftware.eazyrecyclingservice.repository.wastecontainer

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.address.PickupLocationDto

@Entity
@Table(name = "waste_containers")
data class WasteContainerDto(
    @Id
    val id: String,

    @OneToOne(fetch = FetchType.LAZY, cascade = [CascadeType.ALL])
    @JoinColumn(name = "location_id", referencedColumnName = "id")
    var location: PickupLocationDto? = null,

    val notes: String?,
)
