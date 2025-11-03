package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class CompanyService(
  private val companyRepository: CompanyRepository,
  private val companyBranchRepository: ProjectLocations,
) {
  fun create(company: CompanyController.CompanyRequest): CompanyDto {
    val companyDto = CompanyDto(
      name = company.name,
      chamberOfCommerceId = company.chamberOfCommerceId,
      vihbId = company.vihbId,
      address = AddressDto(
        streetName = company.address.streetName,
        buildingName = company.address.buildingNumberAddition,
        buildingNumber = company.address.buildingNumber,
        postalCode = company.address.postalCode,
        city = company.address.city,
        country = company.address.country
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

  fun findAll(includeBranches: Boolean): List<CompanyResponse> {
    val companies = companyRepository.findAll().map { CompanyResponse.from(it) }
    if (includeBranches) {
      val branches = companyBranchRepository.findAll()
        .map { branch -> CompanyBranchResponse.from(branch) }

      return companies.map { company ->
        val companyBranches = branches.filter { it.companyId == company.id }
        company.copy(branches = companyBranches)
      }
    }

    return companies
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
      || cause.constraintName?.contains("companies_vihb_number_key") == true)
  }

  data class CompanyResponse(
    val id: UUID,
    val chamberOfCommerceId: String?,
    val vihbId: String?,
    val name: String,
    val address: AddressDto,
    val processorId: String?,
    val updatedAt: LocalDateTime = LocalDateTime.now(),
    val branches: List<CompanyBranchResponse> = emptyList(),

    ) {

    companion object {
      fun from(company: CompanyDto): CompanyResponse {
        if (company.id == null) {
          throw IllegalArgumentException("Bedrijf met naam ${company.name} en heeft geen id")
        }

        return CompanyResponse(
          id = company.id!!,
          chamberOfCommerceId = company.chamberOfCommerceId,
          vihbId = company.vihbId,
          name = company.name,
          address = company.address,
          processorId = company.processorId,
          updatedAt = company.updatedAt,
        )
      }
    }
  }

  data class CompanyBranchResponse(
    val id: UUID,
    val address: AddressDto,
    val companyId: UUID,
  ) {
    companion object {
      fun from(branch: Location.ProjectLocation): CompanyBranchResponse {
        requireNotNull(branch.id) { "Branch must have an ID" }

        return CompanyBranchResponse(
          id = branch.id,
          address = AddressDto (
            streetName = branch.address.streetName.value,
            buildingNumber = branch.address.buildingNumber,
            postalCode = branch.address.postalCode.value,
            city = branch.address.city.value,
            country = branch.address.country
          ),
          companyId = branch.companyId.uuid,
        )
      }
    }
  }
}
