package nl.eazysoftware.eazyrecyclingservice.controller.company

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.application.usecase.address.ProjectLocationResult
import nl.eazysoftware.eazyrecyclingservice.application.usecase.company.CompanyResult
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyRole
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.company.CompanyProjectLocationDto
import nl.eazysoftware.eazyrecyclingservice.repository.company.ProjectLocationJpaRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerIntegrationTest @Autowired constructor(
  val mockMvc: MockMvc,
  val objectMapper: ObjectMapper,
  val companyRepository: CompanyJpaRepository,
  val projectLocationRepository: ProjectLocationJpaRepository,
) : BaseIntegrationTest() {
  private lateinit var securedMockMvc: SecuredMockMvc
  private lateinit var testCompany: CompanyDto
  private lateinit var testBranches: List<CompanyProjectLocationDto>

  @BeforeEach
  fun setup() {
    securedMockMvc = SecuredMockMvc(mockMvc)
    companyRepository.deleteAll()

    // Create a test company with branches for includeBranches tests
    testCompany = companyRepository.save(
      CompanyDto(
        id = UUID.randomUUID(),
        name = "Test Company BV",
        chamberOfCommerceId = "12345678",
        vihbId = "123456VIHB",
        address = AddressDto(
          streetName = "Main St",
          buildingNumberAddition = "HQ",
          buildingNumber = "1",
          postalCode = "1234 AB",
          city = "Amsterdam",
          country = "Nederland"
        )
      )
    )

    // Create test branches
    val branch1 = CompanyProjectLocationDto(
      id = UUID.randomUUID(),
      company = testCompany,
      streetName = "Branch Street",
      buildingNumber = "42",
      buildingNumberAddition = null,
      postalCode = "5678CD",
      city = "Rotterdam",
      country = "Nederland",
      createdAt = Instant.now(),
      updatedAt = null,
    )

    val branch2 = CompanyProjectLocationDto(
      id = UUID.randomUUID(),
      company = testCompany,
      streetName = "Another Street",
      buildingNumber = "99",
      buildingNumberAddition = null,
      postalCode = "9876ZY",
      city = "Utrecht",
      country = "Nederland",
      createdAt = Instant.now(),
      updatedAt = null,
    )

    testBranches = listOf(
      projectLocationRepository.save(branch1),
      projectLocationRepository.save(branch2)
    )
  }

  private fun companyRequest(
    chamberOfCommerceId: String? = "98765432",
    vihbId: String? = "123456VIHB",
    name: String = "Test BV"
  ) = CompanyController.CompanyRequest(
      chamberOfCommerceId = chamberOfCommerceId,
      vihbId = vihbId,
      processorId = "12345",
      name = name,
      address = AddressRequest(
        streetName = "Main St",
        buildingNumberAddition = "HQ",
        buildingNumber = "1",
        postalCode = "1234 AB",
        city = "Amsterdam",
        country = "Nederland"
      ),
      roles = listOf(CompanyRole.PROCESSOR),
  )

  @Test
  fun `create company - success`() {
    val req = companyRequest(
      vihbId = "101125VIHB"
    )
    val mvcResult = securedMockMvc.post(
      "/companies",
      objectMapper.writeValueAsString(req)
    )
      .andExpect(status().isCreated)
      .andReturn()

    val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)
    val company = companyRepository.findByIdOrNull(created.companyId)

    assertThat(company).isNotNull
    assertThat(company?.chamberOfCommerceId).isEqualTo(req.chamberOfCommerceId)
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
      .andExpect(jsonPath("$.content.length()").value(2))
      .andExpect(jsonPath("$.totalElements").value(2))
  }

  @Test
  fun `get companies - without includeBranches parameter should not include branches`() {
    // The test company and branches are already created in setup()

    securedMockMvc.get("/companies")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.content[0].id").value(testCompany.id.toString()))
      .andExpect(jsonPath("$.content[0].name").value(testCompany.name))
      .andExpect(jsonPath("$.content[0].branches").isEmpty())
  }

  @Test
  fun `get companies - with includeBranches=false should not include branches`() {
    // The test company and branches are already created in setup()

    securedMockMvc.get("/companies?includeBranches=false")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.content[0].id").value(testCompany.id.toString()))
      .andExpect(jsonPath("$.content[0].name").value(testCompany.name))
      .andExpect(jsonPath("$.content[0].branches").isEmpty())
  }

  @Test
  fun `get companies - with includeBranches=true should include branches`() {
    // The test company and branches are already created in setup()

    securedMockMvc.get("/companies?includeBranches=true")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.content[0].id").value(testCompany.id.toString()))
      .andExpect(jsonPath("$.content[0].name").value(testCompany.name))
      .andExpect(jsonPath("$.content[0].branches.length()").value(2))
      .andExpect(jsonPath("$.content[0].branches[0].id").exists())
      .andExpect(jsonPath("$.content[0].branches[0].address.postalCode").value(testBranches[0].postalCode))
      .andExpect(jsonPath("$.content[0].branches[1].id").exists())
      .andExpect(jsonPath("$.content[0].branches[1].address.postalCode").value(testBranches[1].postalCode))
  }

  @Test
  fun `get companies - with multiple companies should include correct branches for each company`() {
    // The first test company and its branches are already created in setup()

    // Create a second company with its own branch
    val secondCompany = companyRepository.save(
      CompanyDto(
        id = UUID.randomUUID(),
        name = "Second Company BV",
        chamberOfCommerceId = "87654321",
        vihbId = "654321VIHB",
        address = AddressDto(
          streetName = "Second St",
          buildingNumberAddition = "HQ2",
          buildingNumber = "2",
          postalCode = "4321BA",
          city = "Eindhoven",
          country = "Nederland"
        )
      )
    )

    val secondCompanyBranch = projectLocationRepository.save(
      CompanyProjectLocationDto(
        id = UUID.randomUUID(),
        company = secondCompany,
        streetName = "Second Branch St",
        buildingNumber = "22",
        buildingNumberAddition = null,
        postalCode = "8765DC",
        city = "Groningen",
        country = "Nederland",
        createdAt = Instant.now(),
        updatedAt = null,
      )
    )

    securedMockMvc.get("/companies?includeBranches=true")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.content.length()").value(2))

      // First company should have 2 branches
      .andExpect(jsonPath("$.content[?(@.id == '${testCompany.id}')].branches.length()").value(2))

      // Second company should have 1 branch
      .andExpect(jsonPath("$.content[?(@.id == '${secondCompany.id}')].branches.length()").value(1))
      .andExpect(
        jsonPath("$.content[?(@.id == '${secondCompany.id}')].branches[0].address.postalCode").value(
          secondCompanyBranch.postalCode
        )
      )
  }

  @Test
  fun `get company by id - success`() {
    val req = companyRequest(
      chamberOfCommerceId = "55667788",
      vihbId = "654321XXXB"
    )
    val mvcResult = securedMockMvc.post(
      "/companies",
      objectMapper.writeValueAsString(req)
    )
      .andReturn()
    val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    securedMockMvc.get("/companies/${created.companyId}")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.id").value(created.companyId.toString()))
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
    val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)
    val updated = req.copy(name = "Updated BV")

    securedMockMvc.put(
      "/companies/${created.companyId}",
      objectMapper.writeValueAsString(updated)
    )
      .andExpect(status().isOk)

    val updatedCompany = companyRepository.findByIdOrNull(created.companyId)

    assertThat(updatedCompany).isNotNull
    assertThat(updatedCompany?.name).isEqualTo("Updated BV")
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
        streetName = req.address.streetName,
        buildingNumberAddition = req.address.buildingNumberAddition,
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
    val created = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    securedMockMvc.delete("/companies/${created.companyId}")
      .andExpect(status().isNoContent)
  }

  @Test
  fun `delete company - not found returns 404`() {
    securedMockMvc.delete("/companies/00000000-0000-0000-0000-000000000000")
      .andExpect(status().isNotFound)
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
    val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    // Create a branch for the company
    val branchRequest = AddressRequest(
      streetName = "Branch Street",
      buildingNumberAddition = "Branch Building",
      buildingNumber = "123",
      postalCode = "1111 AA",
      city = "Test City",
      country = "Nederland"
    )

    val branchResult = securedMockMvc.post(
      "/companies/${company.companyId}/branches",
      objectMapper.writeValueAsString(branchRequest)
    )
      .andReturn()
    val response = objectMapper.readValue(branchResult.response.contentAsString, ProjectLocationResult::class.java)

    // Delete the branch
    securedMockMvc.delete("/companies/${company.companyId}/branches/${response.projectLocationId}")
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
    val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    val nonExistentBranchId = UUID.randomUUID()

    securedMockMvc.delete("/companies/${company.companyId}/branches/$nonExistentBranchId")
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
    val company1 = objectMapper.readValue(mvcResult1.response.contentAsString, CompanyResult::class.java)

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
    val company2 = objectMapper.readValue(mvcResult2.response.contentAsString, CompanyResult::class.java)

    // Create a branch for company2
    val branchRequest = AddressRequest(
      streetName = "Branch Street",
      buildingNumberAddition = "Branch Building",
      buildingNumber = "456",
      postalCode = "2222 BB",
      city = "Test City",
      country = "Nederland"
    )

    val branchResult = securedMockMvc.post(
      "/companies/${company2.companyId}/branches",
      objectMapper.writeValueAsString(branchRequest)
    )
      .andReturn()
    val response = objectMapper.readValue(branchResult.response.contentAsString, ProjectLocationResult::class.java)

    // Try to delete the branch using company1's ID (should fail)
    securedMockMvc.delete("/companies/${company1.companyId}/branches/${response.companyId}")
      .andExpect(status().isNotFound)
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
    val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    // Create a branch for the company
    val branchRequest = AddressRequest(
      streetName = "Branch Street",
      buildingNumberAddition = "Branch Building",
      buildingNumber = "789",
      postalCode = "3333 CC",
      city = "Test City",
      country = "Nederland"
    )

    securedMockMvc.post(
      "/companies/${company.companyId}/branches",
      objectMapper.writeValueAsString(branchRequest)
    )
      .andExpect(status().isOk)

    // Delete the company (should also delete its branches)
    securedMockMvc.delete("/companies/${company.companyId}")
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
    val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    // Create a branch for the company
    val branchRequest = AddressRequest(
      streetName = "Original Street",
      buildingNumberAddition = "Original Building",
      buildingNumber = "100",
      postalCode = "1000 AA",
      city = "Original City",
      country = "Nederland"
    )

    val branchResult = securedMockMvc.post(
      "/companies/${company.companyId}/branches",
      objectMapper.writeValueAsString(branchRequest)
    )
      .andReturn()
    val projectlocationResult =
      objectMapper.readValue(branchResult.response.contentAsString, ProjectLocationResult::class.java)

    // Update the branch
    val updateRequest = AddressRequest(
      streetName = "Updated Street",
      buildingNumberAddition = "Updated Building",
      buildingNumber = "200",
      postalCode = "2000 BB",
      city = "Updated City",
      country = "Nederland"
    )

    securedMockMvc.put(
      "/companies/${company.companyId}/branches/${projectlocationResult.projectLocationId}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isNoContent)

    // Verify the branch was updated by fetching companies with branches
    securedMockMvc.get("/companies?includeBranches=true")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.street").value("Updated Street"))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.houseNumber").value("200"))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.postalCode").value("2000BB"))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.city").value("Updated City"))
  }

  @Test
  fun `update branch - company not found returns 404`() {
    val nonExistentCompanyId = UUID.randomUUID()
    val nonExistentBranchId = UUID.randomUUID()

    val updateRequest = AddressRequest(
      streetName = "Test Street",
      buildingNumberAddition = "Test Building",
      buildingNumber = "123",
      postalCode = "1234 AB",
      city = "Test City",
      country = "Nederland"
    )

    securedMockMvc.put(
      "/companies/$nonExistentCompanyId/branches/$nonExistentBranchId",
      objectMapper.writeValueAsString(updateRequest)
    )
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
    val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    val nonExistentBranchId = UUID.randomUUID()

    val updateRequest = AddressRequest(
      streetName = "Test Street",
      buildingNumberAddition = "Test Building",
      buildingNumber = "123",
      postalCode = "1234 AB",
      city = "Test City",
      country = "Nederland"
    )

    securedMockMvc.put(
      "/companies/${company.companyId}/branches/$nonExistentBranchId",
      objectMapper.writeValueAsString(updateRequest)
    )
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
    val company1 = objectMapper.readValue(mvcResult1.response.contentAsString, CompanyResult::class.java)

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
    val company2 = objectMapper.readValue(mvcResult2.response.contentAsString, CompanyResult::class.java)

    // Create a branch for company2
    val branchRequest = AddressRequest(
      streetName = "Branch Street",
      buildingNumberAddition = "Branch Building",
      buildingNumber = "300",
      postalCode = "3000 CC",
      city = "Test City",
      country = "Nederland"
    )

    val response = securedMockMvc.post(
      "/companies/${company2.companyId}/branches",
      objectMapper.writeValueAsString(branchRequest)
    )
      .andReturn()
    val result = objectMapper.readValue(response.response.contentAsString, ProjectLocationResult::class.java)

    // Try to update the branch using company1's ID (should fail)
    val updateRequest = AddressRequest(
      streetName = "Updated Street",
      buildingNumberAddition = "Updated Building",
      buildingNumber = "400",
      postalCode = "4000 DD",
      city = "Updated City",
      country = "Nederland"
    )

    securedMockMvc.put(
      "/companies/${company1.companyId}/branches/${result.projectLocationId}",
      objectMapper.writeValueAsString(updateRequest)
    )
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
    val company = objectMapper.readValue(mvcResult.response.contentAsString, CompanyResult::class.java)

    // Create a branch for the company
    val branchRequest = AddressRequest(
      streetName = "Original Street",
      buildingNumberAddition = "Original Building",
      buildingNumber = "500",
      postalCode = "5000 EE",
      city = "Original City",
      country = "Nederland"
    )

    val response = securedMockMvc.post(
      "/companies/${company.companyId}/branches",
      objectMapper.writeValueAsString(branchRequest)
    )
      .andReturn()
    val result = objectMapper.readValue(response.response.contentAsString, ProjectLocationResult::class.java)

    // Update the branch with null building number addition
    val updateRequest = AddressRequest(
      streetName = "Updated Street",
      buildingNumberAddition = null,
      buildingNumber = "600",
      postalCode = "6000 FF",
      city = "Updated City",
      country = "Nederland"
    )

    securedMockMvc.put(
      "/companies/${company.companyId}/branches/${result.projectLocationId}",
      objectMapper.writeValueAsString(updateRequest)
    )
      .andExpect(status().isNoContent)

    // Verify the branch was updated correctly
    securedMockMvc.get("/companies?includeBranches=true")
      .andExpect(status().isOk)
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.street").value("Updated Street"))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.houseNumberAddition").value(null))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.houseNumber").value("600"))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.postalCode").value("6000FF"))
      .andExpect(jsonPath("$.content[?(@.id == '${company.companyId}')].branches[0].address.city").value("Updated City"))
  }
}
