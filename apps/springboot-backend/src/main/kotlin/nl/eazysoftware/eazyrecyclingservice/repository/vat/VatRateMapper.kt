package nl.eazysoftware.eazyrecyclingservice.repository.vat

import nl.eazysoftware.eazyrecyclingservice.domain.model.vat.VatRate
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class VatRateMapper {

    fun toDto(domain: VatRate): VatRateDto {
        return VatRateDto(
            vatCode = domain.vatCode,
            percentage = domain.percentage.toBigDecimal(),
            validFrom = domain.validFrom.toJavaInstant(),
            validTo = domain.validTo?.toJavaInstant(),
            countryCode = domain.countryCode,
            description = domain.description
        )
    }

    fun toDomain(dto: VatRateDto): VatRate {
        return VatRate(
            vatCode = dto.vatCode,
            percentage = dto.percentage.toString(),
            validFrom = dto.validFrom.toKotlinInstant(),
            validTo = dto.validTo?.toKotlinInstant(),
            countryCode = dto.countryCode,
            description = dto.description
        )
    }
}
