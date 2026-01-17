package nl.eazysoftware.eazyrecyclingservice.controller.company

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import nl.eazysoftware.eazyrecyclingservice.application.query.AddressView
import nl.eazysoftware.eazyrecyclingservice.application.query.CompleteCompanyView
import nl.eazysoftware.eazyrecyclingservice.application.query.GetAllCompanies
import nl.eazysoftware.eazyrecyclingservice.application.query.GetCompanyById
import nl.eazysoftware.eazyrecyclingservice.application.query.PagedCompanyResponse
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.*
import nl.eazysoftware.eazyrecyclingservice.application.usecase.company.*
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.*
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/companies")
class CompanyController(
  private val createCompanyUseCase: CreateCompany,
  private val updateCompanyUseCase: UpdateCompany,
  private val deleteCompanyUseCase: DeleteCompany,
  private val getAllCompaniesQuery: GetAllCompanies,
  private val getCompanyByIdQuery: GetCompanyById,
  private val createProjectLocation: CreateProjectLocation,
  private val deleteProjectLocation: DeleteProjectLocation,
  private val updateProjectLocation: UpdateProjectLocation,
) {

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  fun createCompany(
    @Valid @RequestBody request: CompanyRequest,
    @RequestParam(required = false) restoreCompanyId: String?
  ): CompanyResult {
    // If restoreCompanyId is provided, restore the soft-deleted company and update it
    if (restoreCompanyId != null) {
      val companyId = CompanyId(UUID.fromString(restoreCompanyId))
      val updateCommand = UpdateCompanyCommand(
        companyId = companyId,
        name = request.name,
        chamberOfCommerceId = request.chamberOfCommerceId?.takeIf { it.isNotBlank() },
        vihbNumber = request.vihbId?.takeIf { it.isNotBlank() }?.let { VihbNumber(it) },
        processorId = request.processorId?.let { ProcessorPartyId(it) },
        address = Address(
          streetName = StreetName(request.address.streetName),
          buildingNumber = request.address.buildingNumber,
          buildingNumberAddition = request.address.buildingNumberAddition,
          postalCode = DutchPostalCode(request.address.postalCode),
          city = City(request.address.city),
          country = request.address.country
        ),
        phone = request.phone?.let { PhoneNumber(it) },
        email = request.email?.let { Email(it) },
        roles = request.roles,
      )
      return updateCompanyUseCase.handleRestore(companyId, updateCommand)
    }

    // Otherwise, create a new company
    val command = CreateCompanyCommand(
      name = request.name,
      chamberOfCommerceId = request.chamberOfCommerceId?.takeIf { it.isNotBlank() },
      vihbNumber = request.vihbId?.takeIf { it.isNotBlank() }?.let { VihbNumber(it) },
      processorId = request.processorId?.let { ProcessorPartyId(it) },
      address = Address(
        streetName = StreetName(request.address.streetName),
        buildingNumber = request.address.buildingNumber,
        buildingNumberAddition = request.address.buildingNumberAddition,
        postalCode = DutchPostalCode(request.address.postalCode),
        city = City(request.address.city),
        country = request.address.country
      ),
      roles = request.roles,
      phone = request.phone?.let { PhoneNumber(it) },
      email = request.email?.let { Email(it) },
    )
    return createCompanyUseCase.handle(command)
  }

  @GetMapping
  @PreAuthorize(HAS_ANY_ROLE)
  fun getCompanies(
    @RequestParam(required = false) includeBranches: Boolean = false,
    @RequestParam(required = false) role: CompanyRole? = null,
    @RequestParam(required = false) query: String? = null,
    @RequestParam(required = false, defaultValue = "0") page: Int = 0,
    @RequestParam(required = false, defaultValue = "10") size: Int = 10,
    @RequestParam(required = false) sortBy: String? = null,
    @RequestParam(required = false, defaultValue = "asc") sortDirection: String = "asc",
    @RequestParam(required = false, defaultValue = "false") excludeTenant: Boolean = false,
  ): PagedCompanyResponse {
    return getAllCompaniesQuery.searchPaginated(query, role, page, size, includeBranches, sortBy, sortDirection, excludeTenant)
  }

  @GetMapping("/{id}")
  @PreAuthorize(HAS_ANY_ROLE)
  fun getById(@PathVariable("id") id: String): CompleteCompanyView {
    return getCompanyByIdQuery.handle(CompanyId(UUID.fromString(id)))
  }

  @DeleteMapping("/{id}")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteCompany(@PathVariable("id") id: String) {
    deleteCompanyUseCase.handle(CompanyId(UUID.fromString(id)))
  }

  @PutMapping("/{id}")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  fun updateCompany(@PathVariable("id") id: String, @Valid @RequestBody request: CompanyRequest): CompanyResult {
    val command = UpdateCompanyCommand(
      companyId = CompanyId(UUID.fromString(id)),
      name = request.name,
      chamberOfCommerceId = request.chamberOfCommerceId?.takeIf { it.isNotBlank() },
      vihbNumber = request.vihbId?.takeIf { it.isNotBlank() }?.let { VihbNumber(it) },
      processorId = request.processorId?.let { ProcessorPartyId(it) },
      address = Address(
        streetName = StreetName(request.address.streetName),
        buildingNumber = request.address.buildingNumber,
        buildingNumberAddition = request.address.buildingNumberAddition,
        postalCode = DutchPostalCode(request.address.postalCode),
        city = City(request.address.city),
        country = request.address.country
      ),
      phone = request.phone?.let { PhoneNumber(it) },
      email = request.email?.let { Email(it) },
      roles = request.roles,
    )
    return updateCompanyUseCase.handle(command)
  }

  @PostMapping("/{id}/branches")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  fun createBranch(@PathVariable("id") id: UUID, @RequestBody projectLocation: AddressRequest): ProjectLocationResult =
    createProjectLocation.handle(
      CreateProjectLocationCommand(
        CompanyId(id),
        Address(
          streetName = StreetName(projectLocation.streetName),
          buildingNumber = projectLocation.buildingNumber,
          buildingNumberAddition = projectLocation.buildingNumberAddition,
          postalCode = DutchPostalCode(projectLocation.postalCode),
          city = City(projectLocation.city),
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
          streetName = StreetName(branch.streetName),
          buildingNumber = branch.buildingNumber,
          buildingNumberAddition = branch.buildingNumberAddition,
          postalCode = DutchPostalCode(branch.postalCode),
          city = City(branch.city),
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

    @field:Pattern(regexp = "^$|^[\\d]{5}$", message = "Verwerkersnummer moet bestaan uit 5 cijfers, of leeg zijn")
    val processorId: String?,

    @field:NotBlank
    val name: String,
    @field:Valid
    val address: AddressRequest,

    val roles: List<CompanyRole> = emptyList(),
    val phone: String? = null,
    val email: String? = null,
  )

  data class CompanyBranchResponse(
    val id: UUID,
    val address: AddressView,
    val companyId: UUID,
  ) {
    companion object {
      fun from(branch: CompanyProjectLocation): CompanyBranchResponse {
        return CompanyBranchResponse(
          id = branch.id.uuid,
          address = AddressView (
            street = branch.address.streetName.value,
            houseNumber = branch.address.buildingNumber,
            houseNumberAddition = branch.address.buildingNumberAddition,
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
