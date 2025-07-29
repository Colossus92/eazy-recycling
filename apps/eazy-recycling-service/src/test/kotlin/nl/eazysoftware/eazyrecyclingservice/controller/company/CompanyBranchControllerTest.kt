package nl.eazysoftware.eazyrecyclingservice.controller.company

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.repository.BranchRepository
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DuplicateKeyException
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyBranchControllerTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val companyRepository: CompanyRepository,
    val branchRepository: BranchRepository
) {
    private lateinit var securedMockMvc: SecuredMockMvc
    private lateinit var testCompany: CompanyDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        
        // Create a test company for branch tests
        testCompany = companyRepository.save(
            CompanyDto(
                name = "Test Company BV",
                chamberOfCommerceId = "12345678",
                vihbId = "VIHB123",
                address = AddressDto(
                    streetName = "Main St",
                    buildingName = "HQ",
                    buildingNumber = "1",
                    postalCode = "1234AB",
                    city = "Amsterdam",
                    country = "Nederland"
                )
            )
        )
    }

    private fun branchRequest(
        streetName: String = "Branch Street",
        buildingName: String? = "Branch Building",
        buildingNumber: String = "42",
        postalCode: String = "5678CD",
        city: String = "Rotterdam",
        country: String = "Nederland"
    ) = CompanyController.AddressRequest(
        streetName = streetName,
        buildingName = buildingName,
        buildingNumber = buildingNumber,
        postalCode = postalCode,
        city = city,
        country = country
    )

    @Test
    fun `create branch - success`() {
        val req = branchRequest()
        
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.address.streetName").value(req.streetName))
            .andExpect(jsonPath("$.address.buildingNumber").value(req.buildingNumber))
            .andExpect(jsonPath("$.address.postalCode").value(req.postalCode))
    }

    @Test
    fun `create branch - duplicate postal code and building number returns 409`() {
        val req = branchRequest(
            postalCode = "9876ZY",
            buildingNumber = "99"
        )
        
        // First create a branch
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isOk)
        
        // Try to create another branch with the same postal code and building number
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").value("Er bestaat al een vestiging op dit adres (postcode en huisnummer) voor dit bedrijf."))
    }

    @Test
    fun `create branch - different building number with same postal code succeeds`() {
        // Create first branch
        val firstBranch = branchRequest(
            postalCode = "1111AA",
            buildingNumber = "10"
        )
        
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(firstBranch)
        )
            .andExpect(status().isOk)
        
        // Create second branch with same postal code but different building number
        val secondBranch = branchRequest(
            postalCode = "1111AA",
            buildingNumber = "12"
        )
        
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(secondBranch)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.address.postalCode").value(secondBranch.postalCode))
            .andExpect(jsonPath("$.address.buildingNumber").value(secondBranch.buildingNumber))
    }

    @Test
    fun `create branch - different postal code with same building number succeeds`() {
        // Create first branch
        val firstBranch = branchRequest(
            postalCode = "2222BB",
            buildingNumber = "42"
        )
        
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(firstBranch)
        )
            .andExpect(status().isOk)
        
        // Create second branch with different postal code but same building number
        val secondBranch = branchRequest(
            postalCode = "3333CC",
            buildingNumber = "42"
        )
        
        securedMockMvc.post(
            "/companies/${testCompany.id}/branch",
            objectMapper.writeValueAsString(secondBranch)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.address.postalCode").value(secondBranch.postalCode))
            .andExpect(jsonPath("$.address.buildingNumber").value(secondBranch.buildingNumber))
    }

    @Test
    fun `create branch - company not found returns 404`() {
        val req = branchRequest()
        val nonExistentCompanyId = UUID.randomUUID()
        
        securedMockMvc.post(
            "/companies/$nonExistentCompanyId/branch",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isNotFound)
    }
}
