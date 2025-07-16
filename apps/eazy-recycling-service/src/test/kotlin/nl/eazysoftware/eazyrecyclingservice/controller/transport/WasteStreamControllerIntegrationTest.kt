package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.repository.WasteStreamRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WasteStreamControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var wasteStreamRepository: WasteStreamRepository

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        wasteStreamRepository.deleteAll()
    }

    @AfterEach
    fun cleanup() {
        wasteStreamRepository.deleteAll()
    }

    @Test
    fun `should get all waste streams`() {
        // Given
        val wasteStreamDto1 = WasteStreamDto(
            number = "WS-001",
            name = "Plastic"
        )
        val wasteStreamDto2 = WasteStreamDto(
            number = "WS-002",
            name = "Paper"
        )
        wasteStreamRepository.saveAll(listOf(wasteStreamDto1, wasteStreamDto2))

        // When & Then
        securedMockMvc.get("/waste-streams")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.number == 'WS-001')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Plastic')]").exists())
            .andExpect(jsonPath("$[?(@.number == 'WS-002')]").exists())
            .andExpect(jsonPath("$[?(@.name == 'Paper')]").exists())
    }

    @Test
    fun `should create waste stream`() {
        // Given
        val wasteStreamDto = WasteStreamDto(
            number = "WS-003",
            name = "Glass"
        )

        // When & Then
        securedMockMvc.post(
            "/waste-streams",
            objectMapper.writeValueAsString(wasteStreamDto)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.number").value("WS-003"))
            .andExpect(jsonPath("$.name").value("Glass"))

        // Verify waste stream was saved in the database
        val savedWasteStream = wasteStreamRepository.findById("WS-003")
        assertThat(savedWasteStream).isPresent
        assertThat(savedWasteStream.get().number).isEqualTo("WS-003")
        assertThat(savedWasteStream.get().name).isEqualTo("Glass")
    }

    @Test
    fun `should update waste stream`() {
        // Given
        val originalWasteStreamDto = WasteStreamDto(
            number = "WS-004",
            name = "Metal"
        )
        wasteStreamRepository.save(originalWasteStreamDto)

        val updatedWasteStreamDto = WasteStreamDto(
            number = "WS-004",
            name = "Scrap Metal"
        )

        // When & Then
        securedMockMvc.put(
            "/waste-streams/WS-004",
            objectMapper.writeValueAsString(updatedWasteStreamDto)
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.number").value("WS-004"))
            .andExpect(jsonPath("$.name").value("Scrap Metal"))

        // Verify waste stream was updated in the database
        val savedWasteStream = wasteStreamRepository.findById("WS-004")
        assertThat(savedWasteStream).isPresent
        assertThat(savedWasteStream.get().number).isEqualTo("WS-004")
        assertThat(savedWasteStream.get().name).isEqualTo("Scrap Metal")
    }

    @Test
    fun `should return error when updating waste stream with mismatched number`() {
        // Given
        val originalWasteStreamDto = WasteStreamDto(
            number = "WS-005",
            name = "Organic"
        )
        wasteStreamRepository.save(originalWasteStreamDto)

        val updatedWasteStreamDto = WasteStreamDto(
            number = "WS-006", // Different number than the path variable
            name = "Updated Organic"
        )

        // When & Then
        securedMockMvc.put(
            "/waste-streams/WS-005",
            objectMapper.writeValueAsString(updatedWasteStreamDto)
        )
            .andExpect(status().isBadRequest)

        // Verify waste stream was not updated in the database
        val savedWasteStream = wasteStreamRepository.findById("WS-005")
        assertThat(savedWasteStream).isPresent
        assertThat(savedWasteStream.get().name).isEqualTo("Organic")
    }

    @Test
    fun `should return not found when updating non-existent waste stream`() {
        // Given
        val nonExistentWasteStreamDto = WasteStreamDto(
            number = "WS-999",
            name = "Non-existent"
        )

        // When & Then
        securedMockMvc.put(
            "/waste-streams/WS-999",
            objectMapper.writeValueAsString(nonExistentWasteStreamDto)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete waste stream`() {
        // Given
        val wasteStreamDto = WasteStreamDto(
            number = "WS-007",
            name = "Textile"
        )
        wasteStreamRepository.save(wasteStreamDto)

        // When & Then
        securedMockMvc.delete("/waste-streams/WS-007")
            .andExpect(status().isOk)

        // Verify waste stream was deleted from the database
        val deletedWasteStream = wasteStreamRepository.findById("WS-007")
        assertThat(deletedWasteStream).isEmpty
    }
    
    @Test
    fun `should return error when creating waste stream with existing number`() {
        // Given
        val existingWasteStreamDto = WasteStreamDto(
            number = "WS-008",
            name = "Existing Stream"
        )
        wasteStreamRepository.save(existingWasteStreamDto)
        
        val duplicateWasteStreamDto = WasteStreamDto(
            number = "WS-008",
            name = "Duplicate Stream"
        )
        
        // When & Then
        securedMockMvc.post(
            "/waste-streams",
            objectMapper.writeValueAsString(duplicateWasteStreamDto)
        )
            .andExpect(status().isBadRequest)
            
        // Verify original waste stream is unchanged
        val savedWasteStream = wasteStreamRepository.findById("WS-008")
        assertThat(savedWasteStream).isPresent
        assertThat(savedWasteStream.get().name).isEqualTo("Existing Stream")
    }
}
