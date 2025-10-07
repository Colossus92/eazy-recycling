package nl.eazysoftware.eazyrecyclingservice.controller.transport

import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.repository.EuralRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import com.fasterxml.jackson.databind.ObjectMapper

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EuralControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var euralRepository: EuralRepository

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
    }

    @AfterEach
    fun cleanup() {
        euralRepository.deleteAll()
    }

    @Test
    fun `should get all eurals when repository is empty`() {
        // When & Then
        securedMockMvc.get("/eural")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should get all eurals when repository contains data`() {
        // Given
        val eural1 = Eural(
            code = "200301",
            description = "Municipal waste"
        )
        val eural2 = Eural(
            code = "200101",
            description = "Paper and cardboard"
        )
        val eural3 = Eural(
            code = "200201",
            description = "Biodegradable waste"
        )

        euralRepository.saveAll(listOf(eural1, eural2, eural3))

        // When & Then
        securedMockMvc.get("/eural")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[?(@.code == '200301')].description").value("Municipal waste"))
            .andExpect(jsonPath("$[?(@.code == '200101')].description").value("Paper and cardboard"))
            .andExpect(jsonPath("$[?(@.code == '200201')].description").value("Biodegradable waste"))
    }

    @Test
    fun `should return eurals with correct structure`() {
        // Given
        val eural = Eural(
            code = "150101",
            description = "Paper and cardboard packaging"
        )
        euralRepository.save(eural)

        // When & Then
        securedMockMvc.get("/eural")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].code").value("150101"))
            .andExpect(jsonPath("$[0].description").value("Paper and cardboard packaging"))
    }

    @Test
    fun `should require authentication to access eurals endpoint`() {
        // Given
        val eural = Eural(
            code = "170201",
            description = "Wood"
        )
        euralRepository.save(eural)

        // When & Then - Request without authentication should fail
        mockMvc.perform(
            get("/eural")
        )
            .andExpect(status().isUnauthorized)
    }

    // POST /eural - Create Eural Tests

    @Test
    fun `should create eural with admin role`() {
        // Given
        val newEural = Eural(
            code = "160214",
            description = "Discarded equipment other than those mentioned in 16 02 09 to 16 02 13"
        )
        val euralJson = objectMapper.writeValueAsString(newEural)

        // When & Then
        securedMockMvc.post("/eural", euralJson)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("160214"))
            .andExpect(jsonPath("$.description").value("Discarded equipment other than those mentioned in 16 02 09 to 16 02 13"))

        // Verify it was saved
        val savedEural = euralRepository.findById("160214")
        assert(savedEural.isPresent)
        assert(savedEural.get().description == "Discarded equipment other than those mentioned in 16 02 09 to 16 02 13")
    }

    @Test
    fun `should require authentication to create eural`() {
        // Given
        val newEural = Eural(
            code = "160215",
            description = "Hazardous components removed from discarded equipment"
        )
        val euralJson = objectMapper.writeValueAsString(newEural)

        // When & Then - Request without authentication should fail
        mockMvc.perform(
            post("/eural")
                .contentType(MediaType.APPLICATION_JSON)
                .content(euralJson)
        )
            .andExpect(status().isUnauthorized)
    }

    // PUT /eural/{code} - Update Eural Tests

    @Test
    fun `should update eural with admin role`() {
        // Given - Create an existing eural
        val existingEural = Eural(
            code = "170203",
            description = "Plastic"
        )
        euralRepository.save(existingEural)

        // Updated eural
        val updatedEural = Eural(
            code = "170203",
            description = "Plastic (updated description)"
        )
        val euralJson = objectMapper.writeValueAsString(updatedEural)

        // When & Then
        securedMockMvc.put("/eural/170203", euralJson)
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("170203"))
            .andExpect(jsonPath("$.description").value("Plastic (updated description)"))

        // Verify it was updated
        val savedEural = euralRepository.findById("170203")
        assert(savedEural.isPresent)
        assert(savedEural.get().description == "Plastic (updated description)")
    }

    @Test
    fun `should return error when updating eural with mismatched code`() {
        // Given - Create an existing eural
        val existingEural = Eural(
            code = "170204",
            description = "Glass, plastic and wood containing or contaminated with dangerous substances"
        )
        euralRepository.save(existingEural)

        // Try to update with different code in body
        val updatedEural = Eural(
            code = "999999",
            description = "Wrong code"
        )
        val euralJson = objectMapper.writeValueAsString(updatedEural)

        // When & Then
        securedMockMvc.put("/eural/170204", euralJson)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should return error when updating non-existent eural`() {
        // Given
        val updatedEural = Eural(
            code = "999999",
            description = "Non-existent code"
        )
        val euralJson = objectMapper.writeValueAsString(updatedEural)

        // When & Then
        securedMockMvc.put("/eural/999999", euralJson)
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `should require authentication to update eural`() {
        // Given
        val existingEural = Eural(
            code = "170205",
            description = "Iron and steel"
        )
        euralRepository.save(existingEural)

        val updatedEural = Eural(
            code = "170205",
            description = "Iron and steel (updated)"
        )
        val euralJson = objectMapper.writeValueAsString(updatedEural)

        // When & Then - Request without authentication should fail
        mockMvc.perform(
            put("/eural/170205")
                .contentType(MediaType.APPLICATION_JSON)
                .content(euralJson)
        )
            .andExpect(status().isUnauthorized)
    }

    // DELETE /eural/{code} - Delete Eural Tests

    @Test
    fun `should delete eural with admin role`() {
        // Given
        val eural = Eural(
            code = "170206",
            description = "Mixed metals"
        )
        euralRepository.save(eural)

        // Verify it exists
        assert(euralRepository.findById("170206").isPresent)

        // When & Then
        securedMockMvc.delete("/eural/170206")
            .andExpect(status().isOk)

        // Verify it was deleted
        assert(euralRepository.findById("170206").isEmpty)
    }

    @Test
    fun `should require authentication to delete eural`() {
        // Given
        val eural = Eural(
            code = "170207",
            description = "Mixed construction and demolition waste"
        )
        euralRepository.save(eural)

        // When & Then - Request without authentication should fail
        mockMvc.perform(
            delete("/eural/170207")
        )
            .andExpect(status().isUnauthorized)

        // Verify it was NOT deleted
        assert(euralRepository.findById("170207").isPresent)
    }
}
