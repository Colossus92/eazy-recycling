package nl.eazysoftware.eazyrecyclingservice.controller.weightticket

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestCompanyFactory
import nl.eazysoftware.eazyrecyclingservice.domain.factories.TestWeightTicketFactory
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.weightticket.WeightTicketJpaRepository
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class WeightTicketControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var weightTicketRepository: WeightTicketJpaRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    private lateinit var testConsignorCompany: CompanyDto
    private lateinit var testCarrierCompany: CompanyDto

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        weightTicketRepository.deleteAll()

        // Create test companies for each test (will be rolled back after each test due to @Transactional)
        testConsignorCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
            processorId = "12345",
            name = "Test Consignor Company"
        ))

        testCarrierCompany = companyRepository.save(TestCompanyFactory.createTestCompany(
            processorId = "67890",
            chamberOfCommerceId = "87654321",
            vihbId = "123456VXXX",
            name = "Test Carrier Company"
        ))
    }

    @Test
    fun `should create weight ticket`() {
        // Given
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = testCarrierCompany.id,
            consignorCompanyId = testConsignorCompany.id!!,
            truckLicensePlate = "AA-123-BB",
            reclamation = "Test reclamation",
            note = "Test note"
        )

        // When & Then
        val result = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()

        // Extract the generated weight ticket ID from response
        val response = objectMapper.readTree(result.response.contentAsString)
        val generatedId = response.get("id").asLong()

        // Verify weight ticket was saved in the database
        val savedWeightTicket = weightTicketRepository.findById(generatedId)
        assertThat(savedWeightTicket).isPresent
        assertThat(savedWeightTicket.get().id).isEqualTo(generatedId)
        assertThat(savedWeightTicket.get().truckLicensePlate).isEqualTo("AA-123-BB")
        assertThat(savedWeightTicket.get().reclamation).isEqualTo("Test reclamation")
        assertThat(savedWeightTicket.get().note).isEqualTo("Test note")
    }

    @Test
    fun `should create weight ticket without optional fields`() {
        // Given
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = null,
            consignorCompanyId = testConsignorCompany.id!!,
            truckLicensePlate = null,
            reclamation = null,
            note = null
        )

        // When & Then
        val result = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").exists())
            .andReturn()

        // Extract the generated weight ticket ID from response
        val response = objectMapper.readTree(result.response.contentAsString)
        val generatedId = response.get("id").asLong()

        // Verify weight ticket was saved in the database
        val savedWeightTicket = weightTicketRepository.findById(generatedId)
        assertThat(savedWeightTicket).isPresent
        assertThat(savedWeightTicket.get().id).isEqualTo(generatedId)
        assertThat(savedWeightTicket.get().truckLicensePlate).isNull()
        assertThat(savedWeightTicket.get().reclamation).isNull()
        assertThat(savedWeightTicket.get().note).isNull()
    }

    @Test
    fun `should create multiple weight tickets with sequential IDs`() {
        // Given
        val firstRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id!!,
            note = "First ticket"
        )

        val secondRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id!!,
            note = "Second ticket"
        )

        // When - create first weight ticket
        val result1 = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(firstRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val id1 = objectMapper.readTree(result1.response.contentAsString).get("id").asLong()

        // When - create second weight ticket
        val result2 = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(secondRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val id2 = objectMapper.readTree(result2.response.contentAsString).get("id").asLong()

        // Then - IDs should be different and sequential
        assertThat(id1).isNotEqualTo(id2)
        assertThat(id2).isGreaterThan(id1)
    }

    @Test
    fun `can get weight ticket by id with full details`() {
        // Given - create weight ticket and extract generated ID
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = testCarrierCompany.id,
            consignorCompanyId = testConsignorCompany.id!!,
            truckLicensePlate = "CC-789-DD",
            reclamation = "Detail test reclamation",
            note = "Detail test note"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // When & Then
        securedMockMvc.get("/weight-tickets/${weightTicketId}")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(weightTicketId))
            .andExpect(jsonPath("$.truckLicensePlate").value("CC-789-DD"))
            .andExpect(jsonPath("$.reclamation").value("Detail test reclamation"))
            .andExpect(jsonPath("$.note").value("Detail test note"))
            .andExpect(jsonPath("$.status").exists())
            .andExpect(jsonPath("$.consignorParty").exists())
            .andExpect(jsonPath("$.carrierParty").exists())
    }

    @Test
    fun `can get all weight tickets`() {
        // Given - create multiple weight tickets
        val firstRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id!!,
            note = "First ticket for list"
        )
        val secondRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id!!,
            note = "Second ticket for list"
        )

        securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(firstRequest)
        ).andExpect(status().isCreated)

        securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(secondRequest)
        ).andExpect(status().isCreated)

        // When & Then
        securedMockMvc.get("/weight-tickets")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].id").exists())
            .andExpect(jsonPath("$[1].id").exists())
    }

    @Test
    fun `can update created weight ticket`() {
        // Given - create weight ticket and extract generated ID
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            carrierParty = testCarrierCompany.id,
            consignorCompanyId = testConsignorCompany.id!!,
            truckLicensePlate = "EE-111-FF",
            note = "Original note"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // When - update the weight ticket
        val updatedRequest = weightTicketRequest.copy(
            truckLicensePlate = "GG-222-HH",
            note = "Updated note"
        )

        securedMockMvc.put(
            "/weight-tickets/${weightTicketId}",
            objectMapper.writeValueAsString(updatedRequest)
        )
            .andExpect(status().isNoContent)

        // Then - verify the update was applied
        val updatedWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(updatedWeightTicket).isPresent
        assertThat(updatedWeightTicket.get().truckLicensePlate).isEqualTo("GG-222-HH")
        assertThat(updatedWeightTicket.get().note).isEqualTo("Updated note")
    }

    @Test
    fun `can delete created weight ticket`() {
        // Given - create weight ticket and extract generated ID
        val weightTicketRequest = TestWeightTicketFactory.createTestWeightTicketRequest(
            consignorCompanyId = testConsignorCompany.id!!,
            note = "To be deleted"
        )
        val createResult = securedMockMvc.post(
            "/weight-tickets",
            objectMapper.writeValueAsString(weightTicketRequest)
        )
            .andExpect(status().isCreated)
            .andReturn()

        val weightTicketId = objectMapper.readTree(createResult.response.contentAsString)
            .get("id").asLong()

        // When - delete the weight ticket (soft delete)
        securedMockMvc.delete("/weight-tickets/${weightTicketId}")
            .andExpect(status().isNoContent)

        // Then - verify the weight ticket status is set to CANCELLED
        val deletedWeightTicket = weightTicketRepository.findById(weightTicketId)
        assertThat(deletedWeightTicket).isPresent
        assertThat(deletedWeightTicket.get().status.name).isEqualTo("CANCELLED")
    }
}
