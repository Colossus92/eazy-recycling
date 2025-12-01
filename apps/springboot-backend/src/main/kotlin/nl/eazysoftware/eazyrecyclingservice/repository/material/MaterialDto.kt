package nl.eazysoftware.eazyrecyclingservice.repository.material

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import nl.eazysoftware.eazyrecyclingservice.repository.vat.VatRateDto

@Entity
@Table(name = "materials")
data class MaterialDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "code", nullable = false)
    val code: String,

    @Column(name = "name", nullable = false)
    val name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "material_group_id", nullable = false)
    val materialGroup: MaterialGroupDto,

    @Column(name = "unit_of_measure", nullable = false)
    val unitOfMeasure: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vat_code", nullable = false, referencedColumnName = "vat_code")
    val vatRate: VatRateDto,

    @Column(name = "status", nullable = false)
    val status: String,
) : AuditableEntity()
