package nl.eazysoftware.eazyrecyclingservice.controller.company

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyBranchRepository
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerIntegrationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper,
    val companyRepository: CompanyRepository,
    val companyBranchRepository: CompanyBranchRepository
) {
    private lateinit var securedMockMvc: SecuredMockMvc
    private lateinit var testCompany: CompanyDto
    private lateinit var testBranches: List<CompanyBranchDto>

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        
        // Create a test company with branches for includeBranches tests
        testCompany = companyRepository.save(
            CompanyDto(
                name = "Test Company BV",
                chamberOfCommerceId = "12345678",
                vihbId = "VIHB123",
                address = AddressDto(
                    streetName = "Main St",
                    buildingName = "HQ",
                    buildingNumber = "1",
                    postalCode = "1234 AB",
                    city = "Amsterdam",
                    country = "Nederland"
                )
            )
        )
        
        // Create test branches
        val branch1 = CompanyBranchDto(
            company = testCompany,
            address = AddressDto(
                streetName = "Branch Street",
                buildingName = "Branch Building 1",
                buildingNumber = "42",
                postalCode = "5678CD",
                city = "Rotterdam",
                country = "Nederland"
            )
        )
        
        val branch2 = CompanyBranchDto(
            company = testCompany,
            address = AddressDto(
                streetName = "Another Street",
                buildingName = "Branch Building 2",
                buildingNumber = "99",
                postalCode = "9876ZY",
                city = "Utrecht",
                country = "Nederland"
            )
        )
        
        testBranches = listOf(
            companyBranchRepository.save(branch1),
            companyBranchRepository.save(branch2)
        )
    }

    private fun companyRequest(
        chamberOfCommerceId: String? = "98765432",
            vihbId: String? = "123456VIHB",
        name: String = "Test BV"
    ) = CompanyController.CompanyRequest(
        chamberOfCommerceId = chamberOfCommerceId,
        vihbId = vihbId,
        name = name,
        address = AddressRequest(
            streetName = "Main St",
            buildingName = "HQ",
            buildingNumber = "1",
            postalCode = "1234 AB",
            city = "Amsterdam",
            country = "Nederland"
        )
    )

    @Test
    fun `create company - success`() {
        val req = companyRequest()
        securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.chamberOfCommerceId").value(req.chamberOfCommerceId))
    }

    @Test
    fun `get companies - returns list`() {
        val req = companyRequest(
            chamberOfCommerceId = "11223344", 
            vihbId = "987654XIXX"
        )
        securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isCreated)

        securedMockMvc.get("/companies")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2)) 
    }
    
    @Test
    fun `get companies - without includeBranches parameter should not include branches`() {
        // The test company and branches are already created in setup()
        
        securedMockMvc.get("/companies")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testCompany.id.toString()))
            .andExpect(jsonPath("$[0].name").value(testCompany.name))
            .andExpect(jsonPath("$[0].branches").isEmpty())
    }
    
    @Test
    fun `get companies - with includeBranches=false should not include branches`() {
        // The test company and branches are already created in setup()
        
        securedMockMvc.get("/companies?includeBranches=false")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testCompany.id.toString()))
            .andExpect(jsonPath("$[0].name").value(testCompany.name))
            .andExpect(jsonPath("$[0].branches").isEmpty())
    }
    
    @Test
    fun `get companies - with includeBranches=true should include branches`() {
        // The test company and branches are already created in setup()
        
        securedMockMvc.get("/companies?includeBranches=true")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(testCompany.id.toString()))
            .andExpect(jsonPath("$[0].name").value(testCompany.name))
            .andExpect(jsonPath("$[0].branches.length()").value(2))
            .andExpect(jsonPath("$[0].branches[0].id").exists())
            .andExpect(jsonPath("$[0].branches[0].address.postalCode").value(testBranches[0].address.postalCode))
            .andExpect(jsonPath("$[0].branches[1].id").exists())
            .andExpect(jsonPath("$[0].branches[1].address.postalCode").value(testBranches[1].address.postalCode))
    }
    
    @Test
    fun `get companies - with multiple companies should include correct branches for each company`() {
        // The first test company and its branches are already created in setup()
        
        // Create a second company with its own branch
        val secondCompany = companyRepository.save(
            CompanyDto(
                name = "Second Company BV",
                chamberOfCommerceId = "87654321", 
                vihbId = "VIHB321", 
                address = AddressDto(
                    streetName = "Second St",
                    buildingName = "HQ2",
                    buildingNumber = "2",
                    postalCode = "4321BA",
                    city = "Eindhoven",
                    country = "Nederland"
                )
            )
        )
        
        val secondCompanyBranch = companyBranchRepository.save(
            CompanyBranchDto(
                company = secondCompany,
                address = AddressDto(
                    streetName = "Second Branch St",
                    buildingName = "Branch Building",
                    buildingNumber = "22",
                    postalCode = "8765DC",
                    city = "Groningen",
                    country = "Nederland"
                )
            )
        )
        
        securedMockMvc.get("/companies?includeBranches=true")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
            
            // First company should have 2 branches
            .andExpect(jsonPath("$[?(@.id == '${testCompany.id}')].branches.length()").value(2))
            
            // Second company should have 1 branch
            .andExpect(jsonPath("$[?(@.id == '${secondCompany.id}')].branches.length()").value(1))
            .andExpect(jsonPath("$[?(@.id == '${secondCompany.id}')].branches[0].address.postalCode").value(secondCompanyBranch.address.postalCode))
    }

    @Test
    fun `get company by id - success`() {
        val req = companyRequest(
            chamberOfCommerceId = "55667788", 
            vihbId = "654321XXXX"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        securedMockMvc.get("/companies/${created.id}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(created.id.toString()))
    }

    @Test
    fun `get company by id - not found returns 404`() {
        securedMockMvc.get("/companies/00000000-0000-0000-0000-000000000000")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update company - success`() {
        val req = companyRequest(
            chamberOfCommerceId = "99887766", 
            vihbId = "999999VXHB"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)
        val updated = created.copy(name = "Updated BV")

        securedMockMvc.put(
            "/companies/${created.id}",
            objectMapper.writeValueAsString(updated)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated BV"))
    }

    @Test
    fun `update company - not found returns 404`() {
        val req = companyRequest()
        val company = CompanyDto(
            id = UUID.randomUUID(),
            chamberOfCommerceId = req.chamberOfCommerceId!!,
            vihbId = req.vihbId,
            name = req.name,
            address = AddressDto(
                streetName = req.address!!.streetName,
                buildingName = req.address.buildingName,
                buildingNumber = req.address.buildingNumber,
                postalCode = req.address.postalCode,
                city = req.address.city,
                country = req.address.country
            ),
        )
        securedMockMvc.put(
            "/companies/${company.id}",
            objectMapper.writeValueAsString(company)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete company - success`() {
        val req = companyRequest(
            chamberOfCommerceId = "44332211", 
            vihbId = "444444XXBX"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        securedMockMvc.delete("/companies/${created.id}")
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete company - not found returns 204`() {
        securedMockMvc.delete("/companies/00000000-0000-0000-0000-000000000000")
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete branch - success`() {
        // Create a company first
        val req = companyRequest(
            chamberOfCommerceId = "55443322", 
            vihbId = "555555VIHB"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        // Create a branch for the company
        val branchRequest = AddressRequest(
            streetName = "Branch Street",
            buildingName = "Branch Building",
            buildingNumber = "123",
            postalCode = "1111 AA",
            city = "Test City",
            country = "Nederland"
        )
        
        val branchResult = securedMockMvc.post(
            "/companies/${company.id}/branches",
            objectMapper.writeValueAsString(branchRequest)
        )
            .andReturn()
        val branch = objectMapper.readValue(branchResult.response.contentAsString, CompanyBranchDto::class.java)

        // Delete the branch
        securedMockMvc.delete("/companies/${company.id}/branches/${branch.id}")
            .andExpect(status().isNoContent)
    }

    @Test
    fun `delete branch - company not found returns 404`() {
        val nonExistentCompanyId = UUID.randomUUID()
        val nonExistentBranchId = UUID.randomUUID()
        
        securedMockMvc.delete("/companies/$nonExistentCompanyId/branches/$nonExistentBranchId")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete branch - branch not found returns 404`() {
        // Create a company first
        val req = companyRequest(
            chamberOfCommerceId = "66554433", 
            vihbId = "666666VIHB"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        val nonExistentBranchId = UUID.randomUUID()
        
        securedMockMvc.delete("/companies/${company.id}/branches/$nonExistentBranchId")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `delete branch - branch belongs to different company returns 400`() {
        // Create first company
        val req1 = companyRequest(
            chamberOfCommerceId = "77665544", 
            vihbId = "777777XIHX"
        )
        val mvcResult1 = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req1)
        )
            .andReturn()
        val company1 = objectMapper.readValue(mvcResult1.response.contentAsString, CompanyDto::class.java)

        // Create second company
        val req2 = companyRequest(
            chamberOfCommerceId = "88776655", 
            vihbId = "888888XIXX"
        )
        val mvcResult2 = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req2)
        )
            .andReturn()
        val company2 = objectMapper.readValue(mvcResult2.response.contentAsString, CompanyDto::class.java)

        // Create a branch for company2
        val branchRequest = AddressRequest(
            streetName = "Branch Street",
            buildingName = "Branch Building",
            buildingNumber = "456",
            postalCode = "2222 BB",
            city = "Test City",
            country = "Nederland"
        )
        
        val branchResult = securedMockMvc.post(
            "/companies/${company2.id}/branches",
            objectMapper.writeValueAsString(branchRequest)
        )
            .andReturn()
        val branch = objectMapper.readValue(branchResult.response.contentAsString, CompanyBranchDto::class.java)

        // Try to delete the branch using company1's ID (should fail)
        securedMockMvc.delete("/companies/${company1.id}/branches/${branch.id}")
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete company with branches - success`() {
        // Create a company
        val req = companyRequest(
            chamberOfCommerceId = "99887766", 
            vihbId = "999999XIBH"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        // Create a branch for the company
        val branchRequest = AddressRequest(
            streetName = "Branch Street",
            buildingName = "Branch Building",
            buildingNumber = "789",
            postalCode = "3333 CC",
            city = "Test City",
            country = "Nederland"
        )
        
        securedMockMvc.post(
            "/companies/${company.id}/branches",
            objectMapper.writeValueAsString(branchRequest)
        )
            .andExpect(status().isOk)

        // Delete the company (should also delete its branches)
        securedMockMvc.delete("/companies/${company.id}")
            .andExpect(status().isNoContent)
    }

    @Test
    fun `update branch - success`() {
        // Create a company first
        val req = companyRequest(
            chamberOfCommerceId = "11998877", 
            vihbId = "111111VIXH"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        // Create a branch for the company
        val branchRequest = AddressRequest(
            streetName = "Original Street",
            buildingName = "Original Building",
            buildingNumber = "100",
            postalCode = "1000 AA",
            city = "Original City",
            country = "Nederland"
        )
        
        val branchResult = securedMockMvc.post(
            "/companies/${company.id}/branches",
            objectMapper.writeValueAsString(branchRequest)
        )
            .andReturn()
        val branch = objectMapper.readValue(branchResult.response.contentAsString, CompanyBranchDto::class.java)

        // Update the branch
        val updateRequest = AddressRequest(
            streetName = "Updated Street",
            buildingName = "Updated Building",
            buildingNumber = "200",
            postalCode = "2000 BB",
            city = "Updated City",
            country = "Nederland"
        )

        securedMockMvc.put("/companies/${company.id}/branches/${branch.id}", objectMapper.writeValueAsString(updateRequest))
            .andExpect(status().isNoContent)

        // Verify the branch was updated by fetching companies with branches
        securedMockMvc.get("/companies?includeBranches=true")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.streetName").value("Updated Street"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.buildingName").value("Updated Building"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.buildingNumber").value("200"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.postalCode").value("2000 BB"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.city").value("Updated City"))
    }

    @Test
    fun `update branch - company not found returns 404`() {
        val nonExistentCompanyId = UUID.randomUUID()
        val nonExistentBranchId = UUID.randomUUID()
        
        val updateRequest = AddressRequest(
            streetName = "Test Street",
            buildingName = "Test Building",
            buildingNumber = "123",
            postalCode = "1234 AB",
            city = "Test City",
            country = "Nederland"
        )
        
        securedMockMvc.put("/companies/$nonExistentCompanyId/branches/$nonExistentBranchId", objectMapper.writeValueAsString(updateRequest))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update branch - branch not found returns 404`() {
        // Create a company first
        val req = companyRequest(
            chamberOfCommerceId = "22998877", 
            vihbId = "222222XIBH"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        val nonExistentBranchId = UUID.randomUUID()
        
        val updateRequest = AddressRequest(
            streetName = "Test Street",
            buildingName = "Test Building",
            buildingNumber = "123",
            postalCode = "1234 AB",
            city = "Test City",
            country = "Nederland"
        )
        
        securedMockMvc.put("/companies/${company.id}/branches/$nonExistentBranchId", objectMapper.writeValueAsString(updateRequest))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update branch - branch belongs to different company returns 400`() {
        // Create first company
        val req1 = companyRequest(
            chamberOfCommerceId = "33998877", 
            vihbId = "333333XXHB"
        )
        val mvcResult1 = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req1)
        )
            .andReturn()
        val company1 = objectMapper.readValue(mvcResult1.response.contentAsString, CompanyDto::class.java)

        // Create second company
        val req2 = companyRequest(
            chamberOfCommerceId = "44998877", 
            vihbId = "444444XXHB"
        )
        val mvcResult2 = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req2)
        )
            .andReturn()
        val company2 = objectMapper.readValue(mvcResult2.response.contentAsString, CompanyDto::class.java)

        // Create a branch for company2
        val branchRequest = AddressRequest(
            streetName = "Branch Street",
            buildingName = "Branch Building",
            buildingNumber = "300",
            postalCode = "3000 CC",
            city = "Test City",
            country = "Nederland"
        )
        
        val branchResult = securedMockMvc.post(
            "/companies/${company2.id}/branches",
            objectMapper.writeValueAsString(branchRequest)
        )
            .andReturn()
        val branch = objectMapper.readValue(branchResult.response.contentAsString, CompanyBranchDto::class.java)

        // Try to update the branch using company1's ID (should fail)
        val updateRequest = AddressRequest(
            streetName = "Updated Street",
            buildingName = "Updated Building",
            buildingNumber = "400",
            postalCode = "4000 DD",
            city = "Updated City",
            country = "Nederland"
        )

        securedMockMvc.put("/companies/${company1.id}/branches/${branch.id}", objectMapper.writeValueAsString(updateRequest))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `update branch - partial update with null building name`() {
        // Create a company first
        val req = companyRequest(
            chamberOfCommerceId = "55998877", 
            vihbId = "555555XXHB"
        )
        val mvcResult = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andReturn()
        val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyDto::class.java)

        // Create a branch for the company
        val branchRequest = AddressRequest(
            streetName = "Original Street",
            buildingName = "Original Building",
            buildingNumber = "500",
            postalCode = "5000 EE",
            city = "Original City",
            country = "Nederland"
        )
        
        val branchResult = securedMockMvc.post(
            "/companies/${company.id}/branches",
            objectMapper.writeValueAsString(branchRequest)
        )
            .andReturn()
        val branch = objectMapper.readValue(branchResult.response.contentAsString, CompanyBranchDto::class.java)

        // Update the branch with null building name
        val updateRequest = AddressRequest(
            streetName = "Updated Street",
            buildingName = null,
            buildingNumber = "600",
            postalCode = "6000 FF",
            city = "Updated City",
            country = "Nederland"
        )

        securedMockMvc.put("/companies/${company.id}/branches/${branch.id}", objectMapper.writeValueAsString(updateRequest))
            .andExpect(status().isNoContent)

        // Verify the branch was updated correctly
        securedMockMvc.get("/companies?includeBranches=true")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.streetName").value("Updated Street"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.buildingName").value(null))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.buildingNumber").value("600"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.postalCode").value("6000 FF"))
            .andExpect(jsonPath("$[?(@.id == '${company.id}')].branches[0].address.city").value("Updated City"))
    }
}