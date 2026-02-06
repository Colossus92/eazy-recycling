package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetInstant
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayLocalDateTime
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate
import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRateId
import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatTaxScenario
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.VatRates
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDateTime
import java.util.*
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@RestController
@RequestMapping("/vat-rates")
class VatRateController(
    private val vatRates: VatRates
) {


    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping
    fun getAllVatRates(): List<VatRateResponse> {
        return vatRates.getAllVatRates().map { it.toResponse() }
    }


    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping("/{id}")
    fun getVatRateById(@PathVariable id: UUID): VatRateResponse {
        val vatRate = vatRates.getVatRateById(VatRateId(id))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BTW tarief met id $id niet gevonden")
        return vatRate.toResponse()
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createVatRate(@Valid @RequestBody request: VatRateRequest): VatRateResponse {
        val vatRate = request.toDomain()
        val created = vatRates.createVatRate(vatRate)
        return created.toResponse()
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @PutMapping("/{id}")
    fun updateVatRate(
        @PathVariable id: UUID,
        @Valid @RequestBody request: VatRateRequest
    ): VatRateResponse {
        // Check if VAT rate exists
        vatRates.getVatRateById(VatRateId(id))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BTW tarief met id $id niet gevonden")

        val vatRate = request.toDomain(id)
        val updated = vatRates.updateVatRate(VatRateId(id), vatRate)
        return updated.toResponse()
    }

    @PreAuthorize(HAS_ROLE_ADMIN)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteVatRate(@PathVariable id: UUID) {
        // Check if VAT rate exists
        vatRates.getVatRateById(VatRateId(id))
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BTW tarief met id $id niet gevonden")

        vatRates.deleteVatRate(VatRateId(id))
    }
}

data class VatRateRequest(
    @field:NotBlank(message = "VAT code is verplicht")
    val vatCode: String,

    @field:NotBlank(message = "Percentage is verplicht")
    @field:Pattern(regexp = "^\\d+(\\.\\d{1,2})?$", message = "Percentage moet een geldig getal zijn")
    val percentage: String,

    val validFrom: LocalDateTime,

    val validTo: LocalDateTime?,

    @field:NotBlank(message = "Landcode is verplicht")
    val countryCode: String,

    @field:NotBlank(message = "Omschrijving is verplicht")
    val description: String,

    val taxScenario: VatTaxScenario,
) {
    fun toDomain(id: UUID = UUID.randomUUID()): VatRate {
        return VatRate(
            id = VatRateId(id),
            vatCode = vatCode,
            percentage = percentage,
            validFrom = validFrom.toCetInstant().toKotlinInstant(),
            validTo = validTo?.toCetInstant()?.toKotlinInstant(),
            countryCode = countryCode,
            description = description,
            taxScenario = taxScenario,
        )
    }
}

data class VatRateResponse(
    val id: UUID,
    val vatCode: String,
    val percentage: String,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime?,
    val countryCode: String,
    val description: String,
    val taxScenario: VatTaxScenario,
    val isReverseCharge: Boolean,
    val createdAt: Instant? = null,
    val createdByName: String? = null,
    val updatedAt: Instant? = null,
    val updatedByName: String? = null,
)

fun VatRate.toResponse(): VatRateResponse {
    return VatRateResponse(
        id = id.value,
        vatCode = vatCode,
        percentage = percentage,
        validFrom = validFrom.toDisplayLocalDateTime(),
        validTo = validTo?.toDisplayLocalDateTime(),
        countryCode = countryCode,
        description = description,
        taxScenario = taxScenario,
        isReverseCharge = isReverseCharge(),
        createdAt = createdAt?.toJavaInstant(),
        createdByName = createdBy,
        updatedAt = updatedAt?.toJavaInstant(),
        updatedByName = updatedBy,
    )
}
