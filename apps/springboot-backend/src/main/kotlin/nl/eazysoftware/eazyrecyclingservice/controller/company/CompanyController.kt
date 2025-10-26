package nl.eazysoftware.eazyrecyclingservice.controller.company

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.CreateProjectLocation
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.CreateProjectLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.DeleteProjectLocation
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.DeleteProjectLocationCommand
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.ProjectLocationResult
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.UpdateProjectLocation
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.UpdateProjectLocationCommand
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/companies")
class CompanyController(
  val companyService: CompanyService,
  val createProjectLocation: CreateProjectLocation,
  val deleteProjectLocation: DeleteProjectLocation,
  val updateProjectLocation: UpdateProjectLocation,
) {

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
  fun createBranch(@PathVariable("id") id: UUID, @RequestBody projectLocation: AddressRequest): ProjectLocationResult =
    createProjectLocation.handle(
      CreateProjectLocationCommand(
        CompanyId(id),
        Address(
          streetName = projectLocation.streetName,
          buildingNumber = projectLocation.buildingNumber,
          buildingNumberAddition = projectLocation.buildingNumberAddition,
          postalCode = DutchPostalCode(projectLocation.postalCode),
          city = projectLocation.city,
          country = projectLocation.country
        )))

  @DeleteMapping("/{companyId}/branches/{branchId}")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteBranch(@PathVariable("companyId") companyId: UUID, @PathVariable("branchId") branchId: UUID) {
    deleteProjectLocation.handle(
      DeleteProjectLocationCommand(
        companyId = CompanyId(companyId),
        projectLocationId = branchId
      )
    )
  }

  @PutMapping("/{companyId}/branches/{branchId}")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun updateBranch(
    @PathVariable("companyId") companyId: UUID,
    @PathVariable("branchId") branchId: UUID,
    @RequestBody branch: AddressRequest
  ) {
    updateProjectLocation.handle(
      UpdateProjectLocationCommand(
        companyId = CompanyId(companyId),
        projectLocationId = branchId,
        address = Address(
          streetName = branch.streetName,
          buildingNumber = branch.buildingNumber,
          buildingNumberAddition = branch.buildingNumberAddition,
          postalCode = DutchPostalCode(branch.postalCode),
          city = branch.city,
          country = branch.country
        )
      )
    )
  }

  data class CompanyRequest(
    @field:Pattern(regexp = "^$|^[\\d]{8}$", message = "KVK nummer moet bestaan uit 8 cijfers, of leeg zijn")
    val chamberOfCommerceId: String?,

    @field:Pattern(
      regexp = "^$|^[0-9]{6}[VIHBX]{4}\$",
      message = "VIHB nummer moet bestaan uit 6 cijfers en 4 letters (VIHBX), of leeg zijn"
    )
    val vihbId: String?,
    @field:NotBlank
    val name: String,
    @field:Valid
    val address: AddressRequest,
  )
}
