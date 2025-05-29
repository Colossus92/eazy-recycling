package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CompanyService(private val companyRepository: CompanyRepository) {
    fun create(company: CompanyController.CompanyRequest): CompanyDto {
        val companyDto = CompanyDto(
            name = company.name,
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
        try {
            return companyRepository.save(companyDto)
        } catch (e: DataIntegrityViolationException) {
            if (isDuplicateKeyException(e)) {
                throw DuplicateKeyException("Kvk nummer of VIHB nummer al in gebruik.")
            }
            throw e
        }
    }

    fun findAll(): List<CompanyDto> {
        return companyRepository.findAll()
    }

    fun findById(id: String): CompanyDto {
        return companyRepository.findById(UUID.fromString(id))
            .orElseThrow { EntityNotFoundException("Bedrijf met id $id niet gevonden") }
    }

    fun delete(id: String) {
        companyRepository.deleteById(UUID.fromString(id))
    }

    fun update(id: String, updatedCompany: CompanyDto): CompanyDto {
        companyRepository.findById(UUID.fromString(id))
            .orElseThrow { EntityNotFoundException("Bedrijf met id $id niet gevonden") }

        try {
            return companyRepository.save(updatedCompany)
        } catch (e: DataIntegrityViolationException) {
            if (isDuplicateKeyException(e)) {
                throw DuplicateKeyException("Kvk nummer of VIHB nummer al in gebruik.")
            }
            throw e
        }
    }

    private fun isDuplicateKeyException(e: DataIntegrityViolationException): Boolean {
        val cause = e.cause
        return cause is ConstraintViolationException
                && (cause.constraintName?.contains("companies_chamber_of_commerce_id_key") == true
                || cause.constraintName?.contains("companies_vihb_id_key") == true)
    }
}