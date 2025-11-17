package nl.eazysoftware.eazyrecyclingservice.controller.truck

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.TruckDto
import nl.eazysoftware.eazyrecyclingservice.test.config.BaseIntegrationTest
import nl.eazysoftware.eazyrecyclingservice.test.util.SecuredMockMvc
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TruckControllerIntegrationTest : BaseIntegrationTest() {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var truckRepository: TruckRepository

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
    }

    @AfterEach
    fun cleanup() {
        truckRepository.deleteAll()
    }

    @Test
    fun `should successfully create a truck when license plate does not exist`() {
        // Given
        val truck = TruckDto(
            licensePlate = "ABC-123",
            brand = "Volvo",
            model = "FH16"
        )

        // When & Then
        securedMockMvc.post(
            "/trucks",
            objectMapper.writeValueAsString(truck)
        ).andExpect(status().isCreated)

        // Verify truck was saved in the database
        val savedTruck = truckRepository.findByIdOrNull("ABC-123")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isEqualTo("Volvo")
        assertThat(savedTruck?.model).isEqualTo("FH16")
    }

    @Test
    fun `should return conflict status when creating a truck with existing license plate`() {
        // Given
        val existingTruck = TruckDto(
            licensePlate = "XYZ-789",
            brand = "Mercedes",
            model = "Actros"
        )
        truckRepository.save(existingTruck)

        val duplicateTruck = TruckDto(
            licensePlate = "XYZ-789",
            brand = "Scania",
            model = "R450"
        )

        // When & Then
        securedMockMvc.post(
            "/trucks",
            objectMapper.writeValueAsString(duplicateTruck)
        ).andExpect(status().isConflict)

        // Verify the original truck was not modified
        val savedTruck = truckRepository.findByIdOrNull("XYZ-789")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isEqualTo("Mercedes")
        assertThat(savedTruck?.model).isEqualTo("Actros")
    }

    @Test
    fun `should handle truck creation with minimal required fields`() {
        // Given
        val minimalTruck = TruckDto(
            licensePlate = "MIN-123"
            // brand and model are nullable, so not setting them
        )

        // When & Then
        securedMockMvc.post(
            "/trucks",
            objectMapper.writeValueAsString(minimalTruck)
        ).andExpect(status().isCreated)

        // Verify truck was saved with null optional fields
        val savedTruck = truckRepository.findByIdOrNull("MIN-123")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isNull()
        assertThat(savedTruck?.model).isNull()
    }

    @Test
    fun `should handle case-insensitive license plate duplicates`() {
        // Given
        val existingTruck = TruckDto(
            licensePlate = "CASE-123",
            brand = "DAF",
            model = "XF"
        )
        truckRepository.save(existingTruck)

        val duplicateTruck = TruckDto(
            licensePlate = "case-123", // Same as above but lowercase
            brand = "MAN",
            model = "TGX"
        )

        // When & Then
        securedMockMvc.post(
            "/trucks",
            objectMapper.writeValueAsString(duplicateTruck)
        ).andExpect(status().isConflict)
    }

    @Test
    fun `should get all trucks`() {
        // Given
        val truck1 = TruckDto(
            licensePlate = "GET-ALL-1",
            brand = "Volvo",
            model = "FH16"
        )
        val truck2 = TruckDto(
            licensePlate = "GET-ALL-2",
            brand = "Mercedes",
            model = "Actros"
        )
        truckRepository.saveAll(listOf(truck1, truck2))

        // When & Then
        securedMockMvc.get("/trucks")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.licensePlate == 'GET-ALL-1')]").exists())
            .andExpect(jsonPath("$[?(@.licensePlate == 'GET-ALL-2')]").exists())
    }

    @Test
    fun `should get truck by license plate`() {
        // Given
        val truck = TruckDto(
            licensePlate = "GET-ONE",
            brand = "Scania",
            model = "R450"
        )
        truckRepository.save(truck)

        // When & Then
        securedMockMvc.get("/trucks/GET-ONE")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.licensePlate").value("GET-ONE"))
            .andExpect(jsonPath("$.brand").value("Scania"))
            .andExpect(jsonPath("$.model").value("R450"))
    }

    @Test
    fun `should return not found when getting truck with non-existent license plate`() {
        // When & Then
        securedMockMvc.get("/trucks/NON-EXISTENT")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete truck by license plate`() {
        // Given
        val truck = TruckDto(
            licensePlate = "DELETE-ME",
            brand = "MAN",
            model = "TGX"
        )
        truckRepository.save(truck)

        // When & Then
        securedMockMvc.delete("/trucks/DELETE-ME")
            .andExpect(status().isNoContent)

        // Verify truck was deleted
        assertThat(truckRepository.findByIdOrNull("DELETE-ME")).isNull()
    }

    @Test
    fun `should update truck`() {
        // Given
        val originalTruck = TruckDto(
            licensePlate = "UPDATE-ME",
            brand = "Volvo",
            model = "FH16"
        )
        truckRepository.save(originalTruck)

        val updatedTruck = TruckDto(
            licensePlate = "UPDATE-ME", // Same license plate
            brand = "Volvo Updated",
            model = "FH16 Updated"
        )

        // When & Then
        securedMockMvc.put(
            "/trucks/UPDATE-ME",
            objectMapper.writeValueAsString(updatedTruck)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.licensePlate").value("UPDATE-ME"))
            .andExpect(jsonPath("$.brand").value("Volvo Updated"))
            .andExpect(jsonPath("$.model").value("FH16 Updated"))

        // Verify truck was updated in the database
        val savedTruck = truckRepository.findByIdOrNull("UPDATE-ME")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isEqualTo("Volvo Updated")
        assertThat(savedTruck?.model).isEqualTo("FH16 Updated")
    }

    @Test
    fun `should return not found when updating non-existent truck`() {
        // Given
        val truck = TruckDto(
            licensePlate = "NON-EXISTENT",
            brand = "Brand",
            model = "Model"
        )

        // When & Then
        securedMockMvc.put(
            "/trucks/NON-EXISTENT",
            objectMapper.writeValueAsString(truck)
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return bad request when updating truck with mismatched license plate`() {
        // Given
        val originalTruck = TruckDto(
            licensePlate = "ORIGINAL",
            brand = "Original Brand",
            model = "Original Model"
        )
        truckRepository.save(originalTruck)

        val truckWithDifferentLicensePlate = TruckDto(
            licensePlate = "DIFFERENT", // Different from path variable
            brand = "New Brand",
            model = "New Model"
        )

        // When & Then
        securedMockMvc.put(
            "/trucks/ORIGINAL",
            objectMapper.writeValueAsString(truckWithDifferentLicensePlate)
        )
            .andExpect(status().isBadRequest)

        // Verify original truck was not modified
        val savedTruck = truckRepository.findByIdOrNull("ORIGINAL")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isEqualTo("Original Brand")
        assertThat(savedTruck?.model).isEqualTo("Original Model")
    }
}
