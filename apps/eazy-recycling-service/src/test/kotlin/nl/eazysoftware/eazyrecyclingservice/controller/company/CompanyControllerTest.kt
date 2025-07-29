package nl.eazysoftware.eazyrecyclingservice.controller.company

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CompanyControllerIntegrationTest @Autowired constructor(
    val mockMvc: MockMvc,
    val objectMapper: ObjectMapper
) {
    private lateinit var securedMockMvc: SecuredMockMvc

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
    }

    private fun companyRequest(
        chamberOfCommerceId: String?= "12345678",
        vihbId: String? = "VIHB123",
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
        val req = companyRequest()
        securedMockMvc.post(
            "/companies",
            objectMapper.writeValueAsString(req)
        )
            .andExpect(status().isCreated)

        securedMockMvc.get("/companies")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `get company by id - success`() {
        val req = companyRequest()
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
        val req = companyRequest()
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
        val req = companyRequest()
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