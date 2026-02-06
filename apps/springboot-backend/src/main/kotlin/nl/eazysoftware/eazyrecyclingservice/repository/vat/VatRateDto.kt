package nl.eazysoftware.eazyrecyclingservice.repository.vat

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import nl.eazysoftware.eazyrecyclingservice.repository.AuditableEntity
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "vat_rates")
data class VatRateDto(
    @Id
    @Column(name = "vat_code")
    val vatCode: String,

    @Column(name = "percentage", nullable = false)
    val percentage: BigDecimal,

    @Column(name = "valid_from", nullable = false)
    val validFrom: Instant,

    @Column(name = "valid_to")
    val validTo: Instant?,

    @Column(name = "country_code", nullable = false)
    val countryCode: String,

    @Column(name = "description", nullable = false)
    val description: String,

    @Column(name = "tax_scenario", nullable = false)
    val taxScenario: String,
) : AuditableEntity()
