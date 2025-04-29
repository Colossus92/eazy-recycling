package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.CompanyController
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CompanyService(private val companyRepository: CompanyRepository) {
    fun create(company: CompanyController.CompanyRequest): CompanyDto {
        val companyDto = CompanyDto(
            name = company.name ?: "",
            chamberOfCommerceId = company.chamberOfCommerceId,
            vihbId = company.vihbId,
            address = AddressDto(
                streetName = company.address?.streetName,
                buildingName = company.address?.buildingName,
                buildingNumber = company.address?.buildingNumber,
                postalCode = company.address?.postalCode,
                city = company.address?.city,
                country = company.address?.country
            ),
        )
        return companyRepository.save(companyDto)
    }

    fun findAll(): List<CompanyDto> {
        return companyRepository.findAll()
    }

    fun findById(id: String): CompanyDto {
        return companyRepository.findById(UUID.fromString(id))
            .orElseThrow { EntityNotFoundException("Company with id $id not found") }
    }

    fun delete(id: String) {
        companyRepository.deleteById(UUID.fromString(id))
    }

    fun update(id: String, updatedCompany: CompanyDto): CompanyDto {
        val existingCompany = companyRepository.findById(UUID.fromString(id))
            .orElseThrow { EntityNotFoundException("Company with id $id not found") }
        return companyRepository.save(existingCompany.copy(
            id = existingCompany.id,
            name = updatedCompany.name,
            chamberOfCommerceId = updatedCompany.chamberOfCommerceId,
            vihbId = updatedCompany.vihbId,
            address = AddressDto(
                streetName = updatedCompany.address.streetName,
                buildingName = updatedCompany.address.buildingName,
                buildingNumber = updatedCompany.address.buildingNumber,
                postalCode = updatedCompany.address.postalCode,
                city = updatedCompany.address.city,
                country = updatedCompany.address.country
            ),
        ))
    }
}