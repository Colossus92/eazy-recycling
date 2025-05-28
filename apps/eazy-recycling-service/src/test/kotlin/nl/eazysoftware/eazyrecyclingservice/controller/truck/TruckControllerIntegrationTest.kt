package nl.eazysoftware.eazyrecyclingservice.controller.truck

import com.fasterxml.jackson.databind.ObjectMapper
import nl.eazysoftware.eazyrecyclingservice.repository.TruckRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.truck.Truck
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TruckControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var truckRepository: TruckRepository

    @AfterEach
    fun cleanup() {
        truckRepository.deleteAll()
    }

    @Test
    fun `should successfully create a truck when license plate does not exist`() {
        // Given
        val truck = Truck(
            licensePlate = "ABC-123",
            brand = "Volvo",
            model = "FH16"
        )

        // When & Then
        mockMvc.perform(
            post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(truck))
        )
            .andExpect(status().isCreated)

        // Verify truck was saved in the database
        val savedTruck = truckRepository.findByIdOrNull("ABC-123")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isEqualTo("Volvo")
        assertThat(savedTruck?.model).isEqualTo("FH16")
    }

    @Test
    fun `should return conflict status when creating a truck with existing license plate`() {
        // Given
        val existingTruck = Truck(
            licensePlate = "XYZ-789",
            brand = "Mercedes",
            model = "Actros"
        )
        truckRepository.save(existingTruck)

        val duplicateTruck = Truck(
            licensePlate = "XYZ-789",
            brand = "Scania",
            model = "R450"
        )

        // When & Then
        mockMvc.perform(
            post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateTruck))
        )
            .andExpect(status().isConflict)

        // Verify the original truck was not modified
        val savedTruck = truckRepository.findByIdOrNull("XYZ-789")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isEqualTo("Mercedes")
        assertThat(savedTruck?.model).isEqualTo("Actros")
    }

    @Test
    fun `should handle truck creation with minimal required fields`() {
        // Given
        val minimalTruck = Truck(
            licensePlate = "MIN-123"
            // brand and model are nullable, so not setting them
        )

        // When & Then
        mockMvc.perform(
            post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(minimalTruck))
        )
            .andExpect(status().isCreated)

        // Verify truck was saved with null optional fields
        val savedTruck = truckRepository.findByIdOrNull("MIN-123")
        assertThat(savedTruck).isNotNull
        assertThat(savedTruck?.brand).isNull()
        assertThat(savedTruck?.model).isNull()
    }

    @Test
    fun `should handle case-insensitive license plate duplicates`() {
        // Given
        val existingTruck = Truck(
            licensePlate = "CASE-123",
            brand = "DAF",
            model = "XF"
        )
        truckRepository.save(existingTruck)

        val duplicateTruck = Truck(
            licensePlate = "case-123", // Same as above but lowercase
            brand = "MAN",
            model = "TGX"
        )

        // When & Then
        mockMvc.perform(
            post("/trucks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(duplicateTruck))
        )
            .andExpect(status().isConflict)
    }
}
