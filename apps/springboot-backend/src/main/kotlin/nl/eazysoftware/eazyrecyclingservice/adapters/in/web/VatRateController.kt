package nl.eazysoftware.eazyrecyclingservice.adapters.`in`.web

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.config.clock.toCetInstant
import nl.eazysoftware.eazyrecyclingservice.config.clock.toDisplayLocalDateTime
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.VatRates
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDateTime
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@RestController
@RequestMapping("/vat-rates")
@PreAuthorize(HAS_ANY_ROLE)
class VatRateController(
    private val vatRates: VatRates
) {

    @GetMapping
    fun getAllVatRates(): List<VatRateResponse> {
        return vatRates.getAllVatRates().map { it.toResponse() }
    }

    @GetMapping("/{vatCode}")
    fun getVatRateByCode(@PathVariable vatCode: String): VatRateResponse {
        val vatRate = vatRates.getVatRateByCode(vatCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BTW tarief met code $vatCode niet gevonden")
        return vatRate.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createVatRate(@Valid @RequestBody request: VatRateRequest): VatRateResponse {
        val vatRate = request.toDomain()
        val created = vatRates.createVatRate(vatRate)
        return created.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping("/{vatCode}")
    fun updateVatRate(
        @PathVariable vatCode: String,
        @Valid @RequestBody request: VatRateRequest
    ): VatRateResponse {
        // Check if VAT rate exists
        vatRates.getVatRateByCode(vatCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BTW tarief met code $vatCode niet gevonden")

        val vatRate = request.toDomain(vatCode)
        val updated = vatRates.updateVatRate(vatCode, vatRate)
        return updated.toResponse()
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping("/{vatCode}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteVatRate(@PathVariable vatCode: String) {
        // Check if VAT rate exists
        vatRates.getVatRateByCode(vatCode)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "BTW tarief met code $vatCode niet gevonden")

        vatRates.deleteVatRate(vatCode)
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
    val description: String
) {
    fun toDomain(overrideVatCode: String? = null): VatRate {
        return VatRate(
            vatCode = overrideVatCode ?: vatCode,
            percentage = percentage,
            validFrom = validFrom.toCetInstant().toKotlinInstant(),
            validTo = validTo?.toCetInstant()?.toKotlinInstant(),
            countryCode = countryCode,
            description = description
        )
    }
}

data class VatRateResponse(
    val vatCode: String,
    val percentage: String,
    val validFrom: LocalDateTime,
    val validTo: LocalDateTime?,
    val countryCode: String,
    val description: String,
    val createdAt: Instant? = null,
    val createdByName: String? = null,
    val updatedAt: Instant? = null,
    val updatedByName: String? = null,
)

fun VatRate.toResponse(): VatRateResponse {
    return VatRateResponse(
        vatCode = vatCode,
        percentage = percentage,
        validFrom = validFrom.toDisplayLocalDateTime(),
        validTo = validTo?.toDisplayLocalDateTime(),
        countryCode = countryCode,
        description = description,
        createdAt = createdAt?.toJavaInstant(),
        createdByName = createdBy,
        updatedAt = updatedAt?.toJavaInstant(),
        updatedByName = updatedBy,
    )
}
