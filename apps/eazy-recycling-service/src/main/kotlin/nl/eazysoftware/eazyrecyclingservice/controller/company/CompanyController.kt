package nl.eazysoftware.eazyrecyclingservice.controller.company

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/companies")
class CompanyController(
    val companyService: CompanyService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    fun createCompany(@RequestBody company: CompanyRequest): CompanyDto {
        return companyService.create(company)
    }

    @GetMapping
    @PreAuthorize(HAS_ANY_ROLE)
    fun getCompanies(@RequestParam(required = false) includeBranches: Boolean = false): List<CompanyService.CompanyResponse> {
        return companyService.findAll(includeBranches)
    }

    @GetMapping("/{id}")

    @PreAuthorize(HAS_ANY_ROLE)
    fun getById(@PathVariable("id") id: String): CompanyDto {
        return companyService.findById(id)
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCompany(@PathVariable("id") id: String) {
        companyService.delete(id)
    }

    @PutMapping("/{id}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    fun updateCompany(@PathVariable("id") id: String, @RequestBody company: CompanyDto): CompanyDto {
        return companyService.update(id, company)
    }

    @PostMapping("/{id}/branches")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    fun createBranch(@PathVariable("id") id: UUID, @RequestBody branch: AddressRequest): CompanyBranchDto {
        return companyService.createBranch(id, branch)
    }

    @DeleteMapping("/{companyId}/branches/{branchId}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteBranch(@PathVariable("companyId") companyId: UUID, @PathVariable("branchId") branchId: UUID) {
        companyService.deleteBranch(companyId, branchId)
    }

    @PutMapping("/{companyId}/branches/{branchId}")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateBranch(
        @PathVariable("companyId") companyId: UUID,
                     @PathVariable("branchId") branchId: UUID,
        @RequestBody branch: AddressRequest) {
        companyService.updateBranch(companyId, branchId, branch)
    }

    data class CompanyRequest(
        val chamberOfCommerceId: String?,
        val vihbId: String?,
        val name: String,
        val address: AddressRequest?,
    )

    data class AddressRequest(
        val streetName: String,
        val buildingName: String? = null,
        val buildingNumber: String,
        val postalCode: String,
        val city: String,
        val country: String = "Nederland",
    )
}