package nl.eazysoftware.eazyrecyclingservice.controller

import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/companies")
class CompanyController(
    val companyService: CompanyService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCompany(@RequestBody company: CompanyRequest): CompanyDto {
        return companyService.create(company)
    }

    @GetMapping
    fun getCompanies(): List<CompanyDto> {
        return companyService.findAll()
    }

    @GetMapping("/{id}")
    fun getById(@PathVariable("id") id: String): CompanyDto {
        return companyService.findById(id)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCompany(@PathVariable("id") id: String) {
        companyService.delete(id)
    }

    @PutMapping("/{id}")
    fun updateCompany(@PathVariable("id") id: String, @RequestBody company: CompanyDto): CompanyDto {
        return companyService.update(id, company)
    }

    data class CompanyRequest(
            val chamberOfCommerceId: String?,
            val vihbId: String?,
            val name: String?,
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