package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.ProcessorPartyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.VihbNumber
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import org.springframework.stereotype.Component
import kotlin.time.toJavaInstant
import kotlin.time.toKotlinInstant

@Component
class CompanyMapper {

  fun toDomain(dto: CompanyDto): Company {
    return Company(
      companyId = CompanyId(dto.id),
      name = dto.name,
      chamberOfCommerceId = dto.chamberOfCommerceId,
      vihbNumber = dto.vihbId?.let { VihbNumber(it) },
      processorId = dto.processorId?.let { ProcessorPartyId(it) },
      address = Address(
        streetName = StreetName(dto.address.streetName),
        buildingNumber = dto.address.buildingNumber,
        buildingNumberAddition = dto.address.buildingNumberAddition,
        postalCode = DutchPostalCode(dto.address.postalCode),
        city = City(dto.address.city),
        country = dto.address.country
      ),
      deletedAt = dto.deletedAt?.toKotlinInstant(),
    )
  }

  fun toDto(domain: Company): CompanyDto {
    return CompanyDto(
      id = domain.companyId.uuid,
      name = domain.name,
      chamberOfCommerceId = domain.chamberOfCommerceId,
      vihbId = domain.vihbNumber?.value,
      processorId = domain.processorId?.number,
      address = AddressDto(
        streetName = domain.address.streetName.value,
        buildingNumber = domain.address.buildingNumber,
        buildingNumberAddition = domain.address.buildingNumberAddition,
        postalCode = domain.address.postalCode.value,
        city = domain.address.city.value,
        country = domain.address.country
      ),
      deletedAt = domain.deletedAt?.toJavaInstant()
    )
  }
}
