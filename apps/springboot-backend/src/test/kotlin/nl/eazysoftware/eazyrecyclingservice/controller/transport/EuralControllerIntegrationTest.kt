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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

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
}
