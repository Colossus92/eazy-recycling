package nl.eazysoftware.eazyrecyclingservice.repository.entity.container

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import java.util.*

@Entity
@Table(name = "waste_containers")
data class WasteContainerDto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val uuid: UUID? = null,
    val id: String,

    @OneToOne
    @JoinColumn(name = "location_company_id")
    var company: CompanyDto? = null,
    
    @Embedded
    var address: AddressDto? = null,
    
    val notes: String?,
)