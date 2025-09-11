package nl.eazysoftware.eazyrecyclingservice.controller.company

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
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

    @GetMapping("/hello")
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    fun hello(): String {
      return "Hello"
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    fun createCompany(@Valid @RequestBody company: CompanyRequest): CompanyDto {
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
        @field:Pattern(regexp = "^$|^[\\d]{8}$", message = "KVK nummer moet bestaan uit 8 cijfers, of leeg zijn")
        val chamberOfCommerceId: String?,

        @field:Pattern(regexp = "^$|^[0-9]{6}[VIHBX]{4}\$", message = "VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn")
        val vihbId: String?,
        @field:NotBlank
        val name: String,
        @field:Valid
        val address: AddressRequest?,
    )


}
