package nl.eazysoftware.eazyrecyclingservice.repository.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.*
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
      code = dto.code,
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
      roles = dto.roles,
      phone = dto.phone?.let { PhoneNumber(it) },
      email = dto.email?.let { Email(it) },
      isSupplier = dto.isSupplier,
      isCustomer = dto.isCustomer,
      isTenantCompany = dto.isTenantCompany,
      deletedAt = dto.deletedAt?.toKotlinInstant(),
      createdAt = dto.createdAt?.toKotlinInstant(),
      createdBy = dto.createdBy,
      updatedAt = dto.updatedAt?.toKotlinInstant(),
      updatedBy = dto.updatedBy,
    )
  }

  fun toDto(domain: Company): CompanyDto {
    return CompanyDto(
      id = domain.companyId.uuid,
      code = domain.code,
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
      roles = domain.roles,
      phone = domain.phone?.value,
      email = domain.email?.value,
      isSupplier = domain.isSupplier,
      isCustomer = domain.isCustomer,
      isTenantCompany = domain.isTenantCompany,
      deletedAt = domain.deletedAt?.toJavaInstant()
    )
  }
}
