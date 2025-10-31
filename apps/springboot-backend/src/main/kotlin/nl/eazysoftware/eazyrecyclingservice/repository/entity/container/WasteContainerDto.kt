package nl.eazysoftware.eazyrecyclingservice.repository.entity.container

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto

@Entity
@Table(name = "waste_containers")
data class WasteContainerDto(
    @Id
    val id: String,

    @OneToOne
    @JoinColumn(name = "location_company_id")
    var company: CompanyDto? = null,

    @Embedded
    var address: AddressDto? = null,

    val notes: String?,
)
