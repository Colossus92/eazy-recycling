package nl.eazysoftware.eazyrecyclingservice.controller.transport

import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethod
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProcessingMethodControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var processingMethodRepository: ProcessingMethodRepository

    private lateinit var testProcessingMethods: List<ProcessingMethod>

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)

        // Create test processing methods
        testProcessingMethods = listOf(
            ProcessingMethod(
                code = "R01",
                description = "Use as fuel or other means to generate energy"
            ),
            ProcessingMethod(
                code = "R02",
                description = "Solvent reclamation/regeneration"
            ),
            ProcessingMethod(
                code = "R03",
                description = "Recycling/reclamation of organic substances which are not used as solvents"
            )
        )

        processingMethodRepository.saveAll(testProcessingMethods)
    }

    @AfterEach
    fun cleanup() {
        processingMethodRepository.deleteAll()
    }

    @Test
    fun `GET processing-methods should return all processing methods with 200 OK`() {
        // When & Then
        securedMockMvc.get("/processing-methods")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(testProcessingMethods.size))
            .andExpect(jsonPath("$[0].code").value("R01"))
            .andExpect(jsonPath("$[0].description").value("Use as fuel or other means to generate energy"))
            .andExpect(jsonPath("$[1].code").value("R02"))
            .andExpect(jsonPath("$[1].description").value("Solvent reclamation/regeneration"))
            .andExpect(jsonPath("$[2].code").value("R03"))
            .andExpect(jsonPath("$[2].description").value("Recycling/reclamation of organic substances which are not used as solvents"))
    }

    @Test
    fun `GET processing-methods should return empty list when no processing methods exist`() {
        // Given - clean up all processing methods
        processingMethodRepository.deleteAll()

        // When & Then
        securedMockMvc.
            get("/processing-methods")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.length()").value(0))
    }
}
