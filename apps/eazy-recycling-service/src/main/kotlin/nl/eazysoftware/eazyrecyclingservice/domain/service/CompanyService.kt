package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyBranchRepository
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
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
    private val companyBranchRepository: CompanyBranchRepository,
) {
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
                || cause.constraintName?.contains("companies_vihb_id_key") == true)
    }

    fun createBranch(companyId: UUID, branch: CompanyController.AddressRequest): CompanyBranchDto {
        // Check if a branch with the same postal code and building number already exists for this company
        if (companyBranchRepository.existsByCompanyIdAndPostalCodeAndBuildingNumber(
                companyId,
                branch.postalCode,
                branch.buildingNumber
            )
        ) {
            throw DuplicateKeyException("Er bestaat al een vestiging op dit adres (postcode en huisnummer) voor dit bedrijf.")
        }

        // Use findById instead of getReference to avoid proxy serialization issues
        val company = companyRepository.findById(companyId)
            .orElseThrow { EntityNotFoundException("Bedrijf met id $companyId niet gevonden") }

        val branchDto = CompanyBranchDto(
            company = company,
            address = AddressDto(
                streetName = branch.streetName,
                buildingName = branch.buildingName,
                buildingNumber = branch.buildingNumber,
                postalCode = branch.postalCode,
                city = branch.city,
                country = branch.country
            )
        )

        return companyBranchRepository.save(branchDto)
    }

    fun deleteBranch(companyId: UUID, branchId: UUID) {
        val branch = companyBranchRepository.findById(branchId)
            .orElseThrow { EntityNotFoundException("Vestiging met id $branchId niet gevonden") }

        if (branch.company.id != companyId) {
            throw IllegalArgumentException("Vestiging met id $branchId is niet van bedrijf met id $companyId")
        }

        companyBranchRepository.deleteById(branchId)
    }

    fun updateBranch(companyId: UUID, branchId: UUID, branchAddress: CompanyController.AddressRequest) {
        val branch = companyBranchRepository.findById(branchId)
            .orElseThrow { EntityNotFoundException("Vestiging met id $branchId niet gevonden") }

        if (branch.company.id != companyId) {
            throw IllegalArgumentException("Vestiging met id $branchId is niet van bedrijf met id $companyId")
        }

        val updatedBranch = CompanyBranchDto(
            id = branchId,
            company = branch.company,
            address = AddressDto(
                streetName = branchAddress.streetName,
                buildingName = branchAddress.buildingName,
                buildingNumber = branchAddress.buildingNumber,
                postalCode = branchAddress.postalCode,
                city = branchAddress.city,
                country = branchAddress.country
            )
        )

        companyBranchRepository.save(updatedBranch)
    }

    data class CompanyResponse(
        val id: UUID,
        val chamberOfCommerceId: String?,
        val vihbId: String?,
        val name: String,
        val address: AddressDto,
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
            fun from(branch: CompanyBranchDto): CompanyBranchResponse {
                if (branch.id == null) {
                    throw IllegalArgumentException("Vestiging heeft geen id")
                }

                if (branch.company.id == null) {
                    throw IllegalArgumentException("Vestiging is niet gekoppeld aan een bedrijf met een id")
                }

                return CompanyBranchResponse(
                    id = branch.id!!,
                    address = branch.address,
                    companyId = branch.company.id!!,
                )
            }
        }
    }
}
