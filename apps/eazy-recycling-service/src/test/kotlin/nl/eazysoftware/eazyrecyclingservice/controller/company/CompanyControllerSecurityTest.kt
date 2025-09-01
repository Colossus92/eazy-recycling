package nl.eazysoftware.eazyrecyclingservice.controller.company

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.controller.company.CompanyController.CompanyRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*
import java.util.stream.Stream

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CompanyControllerSecurityTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    private lateinit var testCompanyId: UUID
    private lateinit var testCompany: CompanyDto

    @BeforeEach
    fun setup() {
        testCompany = CompanyDto(
            name = "Test Company",
            chamberOfCommerceId = "12345678",
            vihbId = "123456VIHB",
            address = AddressDto(
                streetName = "Test Street",
                buildingName = "Test Building",
                buildingNumber = "123",
                postalCode = "1234 AB",
                city = "Test City",
                country = "Test Country"
            )
        )
        val savedCompany = companyRepository.save(testCompany)
        testCompanyId = savedCompany.id!!
    }

    @AfterEach
    fun cleanup() {
        companyRepository.deleteAll()
    }

    companion object {
        @JvmStatic
        fun roleAccessScenarios(): Stream<Arguments> {
            return Stream.of(
                // GET all companies - any role can access
                Arguments.of("/companies", "GET", Roles.ADMIN, 200),
                Arguments.of("/companies", "GET", Roles.PLANNER, 200),
                Arguments.of("/companies", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/companies", "GET", "unauthorized_role", 403),

                // GET company by id - any role can access
                Arguments.of("/companies/{id}", "GET", Roles.ADMIN, 200),
                Arguments.of("/companies/{id}", "GET", Roles.PLANNER, 200),
                Arguments.of("/companies/{id}", "GET", Roles.CHAUFFEUR, 200),
                Arguments.of("/companies/{id}", "GET", "unauthorized_role", 403),

                // POST (create) company - only admin and planner can access
                Arguments.of("/companies", "POST", Roles.ADMIN, 201),
                Arguments.of("/companies", "POST", Roles.PLANNER, 201),
                Arguments.of("/companies", "POST", Roles.CHAUFFEUR, 403),
                Arguments.of("/companies", "POST", "unauthorized_role", 403),

                // PUT (update) company - only admin and planner can access
                Arguments.of("/companies/{id}", "PUT", Roles.ADMIN, 200),
                Arguments.of("/companies/{id}", "PUT", Roles.PLANNER, 200),
                Arguments.of("/companies/{id}", "PUT", Roles.CHAUFFEUR, 403),
                Arguments.of("/companies/{id}", "PUT", "unauthorized_role", 403),

                // DELETE company - only admin and planner can access
                Arguments.of("/companies/{id}", "DELETE", Roles.ADMIN, 204),
                Arguments.of("/companies/{id}", "DELETE", Roles.PLANNER, 204),
                Arguments.of("/companies/{id}", "DELETE", Roles.CHAUFFEUR, 403),
                Arguments.of("/companies/{id}", "DELETE", "unauthorized_role", 403)
            )
        }
    }

    @ParameterizedTest(name = "{1} {0} with role {2} should return {3}")
    @MethodSource("roleAccessScenarios")
    fun `should verify role-based access control for company endpoints`(
        endpoint: String,
        method: String,
        role: String,
        expectedStatus: Int
    ) {
        // Replace {id} placeholder with actual ID
        val resolvedEndpoint = endpoint.replace("{id}", testCompanyId.toString())

        val request = when (method) {
            "GET" -> get(resolvedEndpoint)
            "POST" -> post(resolvedEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(CompanyRequest(
                    chamberOfCommerceId = "87654321",
                    vihbId = "654321VIHB",
                    name = "New Test Company",
                    address = CompanyController.AddressRequest(
                        streetName = "Test Street",
                        buildingNumber = "123",
                        postalCode = "1234 AB",
                        city = "Test City"
                    )
                )))
            "PUT" -> put(resolvedEndpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testCompany.copy(
                    name = "Updated Test Company"
                )))
            "DELETE" -> delete(resolvedEndpoint)
            else -> throw IllegalArgumentException("Unsupported method: $method")
        }

        mockMvc.perform(
            request.with(
                jwt().authorities(SimpleGrantedAuthority(role))
            )
        ).andExpect(status().`is`(expectedStatus))
    }
}
