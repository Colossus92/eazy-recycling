package nl.eazysoftware.eazyrecyclingservice.controller.transport

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

private const val WASTE_STREAM_NUMBER = "08123ABCDEFG"

//TODO enable again
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
    private lateinit var wasteStreamRepository: WasteStreams

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
        wasteStreamRepository.deleteAll()
    }

    @AfterEach
    fun cleanup() {
        wasteStreamRepository.deleteAll()
    }

//    @Test
//    fun `should get all waste streams`() {
//        // Given
//        val wasteStreamDto1 = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Plastic"
//        )
//        val wasteStreamDto2 = WasteStreamDto(
//            number = "08123ZYXWVUT",
//            name = "Paper"
//        )
//        wasteStreamRepository.saveAll(listOf(wasteStreamDto1, wasteStreamDto2))
//
//        // When & Then
//        securedMockMvc.get("/waste-streams")
//            .andExpect(status().isOk)
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$").isArray)
//            .andExpect(jsonPath("$.length()").value(2))
//            .andExpect(jsonPath("$[?(@.number == '08123ABCDEFG')]").exists())
//            .andExpect(jsonPath("$[?(@.name == 'Plastic')]").exists())
//            .andExpect(jsonPath("$[?(@.number == '08123ZYXWVUT')]").exists())
//            .andExpect(jsonPath("$[?(@.name == 'Paper')]").exists())
//    }
//
//    @Test
//    fun `should create waste stream`() {
//        // Given
//        val wasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Glass"
//        )
//
//        // When & Then
//        securedMockMvc.post(
//            "/waste-streams",
//            objectMapper.writeValueAsString(wasteStreamDto)
//        )
//            .andExpect(status().isOk)
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$.number").value(WASTE_STREAM_NUMBER))
//            .andExpect(jsonPath("$.name").value("Glass"))
//
//        // Verify waste stream was saved in the database
//        val savedWasteStream = wasteStreamRepository.findById(WASTE_STREAM_NUMBER)
//        assertThat(savedWasteStream).isPresent
//        assertThat(savedWasteStream.get().number).isEqualTo(WASTE_STREAM_NUMBER)
//        assertThat(savedWasteStream.get().name).isEqualTo("Glass")
//    }
//
//    @Test
//    fun `should update waste stream`() {
//        // Given
//        val originalWasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Metal"
//        )
//        wasteStreamRepository.save(originalWasteStreamDto)
//
//        val updatedWasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Scrap Metal"
//        )
//
//        // When & Then
//        securedMockMvc.put(
//            "/waste-streams/$WASTE_STREAM_NUMBER",
//            objectMapper.writeValueAsString(updatedWasteStreamDto)
//        )
//            .andExpect(status().isOk)
//            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
//            .andExpect(jsonPath("$.number").value(WASTE_STREAM_NUMBER))
//            .andExpect(jsonPath("$.name").value("Scrap Metal"))
//
//        // Verify waste stream was updated in the database
//        val savedWasteStream = wasteStreamRepository.findById(WASTE_STREAM_NUMBER)
//        assertThat(savedWasteStream).isPresent
//        assertThat(savedWasteStream.get().number).isEqualTo(WASTE_STREAM_NUMBER)
//        assertThat(savedWasteStream.get().name).isEqualTo("Scrap Metal")
//    }
//
//    @Test
//    fun `should return error when updating waste stream with mismatched number`() {
//        // Given
//        val originalWasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Organic"
//        )
//        wasteStreamRepository.save(originalWasteStreamDto)
//
//        val updatedWasteStreamDto = WasteStreamDto(
//            number = "08123ABCDEFGI", // Different number than the path variable
//            name = "Updated Organic"
//        )
//
//        // When & Then
//        securedMockMvc.put(
//            "/waste-streams/$WASTE_STREAM_NUMBER",
//            objectMapper.writeValueAsString(updatedWasteStreamDto)
//        )
//            .andExpect(status().isBadRequest)
//
//        // Verify waste stream was not updated in the database
//        val savedWasteStream = wasteStreamRepository.findById(WASTE_STREAM_NUMBER)
//        assertThat(savedWasteStream).isPresent
//        assertThat(savedWasteStream.get().name).isEqualTo("Organic")
//    }
//
//    @Test
//    fun `should return not found when updating non-existent waste stream`() {
//        // Given
//        val nonExistentWasteStreamDto = WasteStreamDto(
//            number = "12001ABCDEFG",
//            name = "Non-existent"
//        )
//
//        // When & Then
//        securedMockMvc.put(
//            "/waste-streams/12001ABCDEFG",
//            objectMapper.writeValueAsString(nonExistentWasteStreamDto)
//        )
//            .andExpect(status().isNotFound)
//    }
//
//    @Test
//    fun `should delete waste stream`() {
//        // Given
//        val wasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Textile"
//        )
//        wasteStreamRepository.save(wasteStreamDto)
//
//        // When & Then
//        securedMockMvc.delete("/waste-streams/$WASTE_STREAM_NUMBER")
//            .andExpect(status().isOk)
//
//        // Verify waste stream was deleted from the database
//        val deletedWasteStream = wasteStreamRepository.findById(WASTE_STREAM_NUMBER)
//        assertThat(deletedWasteStream).isEmpty
//    }
//
//    @Test
//    fun `should return error when creating waste stream with existing number`() {
//        // Given
//        val existingWasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Existing Stream"
//        )
//        wasteStreamRepository.save(existingWasteStreamDto)
//
//        val duplicateWasteStreamDto = WasteStreamDto(
//            number = WASTE_STREAM_NUMBER,
//            name = "Duplicate Stream"
//        )
//
//        // When & Then
//        securedMockMvc.post(
//            "/waste-streams",
//            objectMapper.writeValueAsString(duplicateWasteStreamDto)
//        )
//            .andExpect(status().isBadRequest)
//
//        // Verify original waste stream is unchanged
//        val savedWasteStream = wasteStreamRepository.findById(WASTE_STREAM_NUMBER)
//        assertThat(savedWasteStream).isPresent
//        assertThat(savedWasteStream.get().name).isEqualTo("Existing Stream")
//    }
//
//    @Test
//    fun `should return bad request when creating waste stream with invalid number`() {
//        // Given
//        val wasteStreamDto = WasteStreamDto(
//            number = "invalid_number",
//            name = "Invalid Stream"
//        )
//
//        // When & Then
//        securedMockMvc.post(
//            "/waste-streams",
//            objectMapper.writeValueAsString(wasteStreamDto)
//        )
//            .andExpect(status().isBadRequest)
//    }
}
