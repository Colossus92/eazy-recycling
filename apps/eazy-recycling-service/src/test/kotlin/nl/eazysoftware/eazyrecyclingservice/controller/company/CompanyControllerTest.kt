package nl.eazysoftware.eazyrecyclingservice.controller.company

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.repository.BranchRepository
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
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
    val branchRepository: BranchRepository
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
                    postalCode = "1234AB",
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
            branchRepository.save(branch1),
            branchRepository.save(branch2)
        )
    }

    private fun companyRequest(
        chamberOfCommerceId: String? = "98765432",
        vihbId: String? = "VIHB456", 
        name: String = "Test BV"
    ) = CompanyController.CompanyRequest(
        chamberOfCommerceId = chamberOfCommerceId,
        vihbId = vihbId,
        name = name,
        address = CompanyController.AddressRequest(
            streetName = "Main St",
            buildingName = "HQ",
            buildingNumber = "1",
            postalCode = "1234AB",
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
    @Disabled
    fun `create company - duplicate chamberOfCommerceId returns 409`() {
        val req = companyRequest()
        securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isCreated)

        securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.message").exists())
    }

    @Test
    fun `get companies - returns list`() {
        val req = companyRequest(
            chamberOfCommerceId = "11223344", 
            vihbId = "VIHB789"
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
        
        val secondCompanyBranch = branchRepository.save(
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
            vihbId = "VIHB555"
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
            vihbId = "VIHB999"
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
    @Disabled
    fun `update company - duplicate chamberOfCommerceId returns 409`() {
        val req1 = companyRequest(chamberOfCommerceId = "11111111", vihbId = "VIHB1")
        val req2 = companyRequest(chamberOfCommerceId = "22222222", vihbId = "VIHB2")
        val mvcResult1 = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req1)
        )
            .andReturn()
        val mvcResult2 = securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req2)
        )
            .andReturn()
        val created1 = objectMapper.readValue(mvcResult1.response.contentAsString, CompanyDto::class.java)
        val created2 = objectMapper.readValue(mvcResult2.response.contentAsString, CompanyDto::class.java)

        // Try to update company2 with company1's chamberOfCommerceId
        val update = created2.copy(chamberOfCommerceId = created1.chamberOfCommerceId)
        securedMockMvc.put(
            "/companies/${created2.id}",
            objectMapper.writeValueAsString(update)
        )
            .andExpect(status().isConflict)
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
            vihbId = "VIHB444"
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
}