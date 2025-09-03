package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
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
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class WasteContainerControllerIntegrationTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    private lateinit var securedMockMvc: SecuredMockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var wasteContainerRepository: WasteContainerRepository

    @Autowired
    private lateinit var companyRepository: CompanyRepository

    @BeforeEach
    fun setup() {
        securedMockMvc = SecuredMockMvc(mockMvc)
    }

    @AfterEach
    fun cleanup() {
        wasteContainerRepository.deleteAll()
        companyRepository.deleteAll()
    }

    @Test
    fun `should successfully create a container with address`() {
        // Given
        val address = AddressRequest(
            streetName = "Test Street",
            buildingNumber = "123",
            city = "Test City",
            postalCode = "1234AB",
            country = "Test Country"
        )

        val containerRequest = CreateContainerRequest(
            id = "CONTAINER-001",
            location = CreateContainerRequest.ContainerLocation(
                companyId = null,
                companyName = null,
                address = address
            ),
            notes = "Test container with address"
        )

        // When & Then
        securedMockMvc.post(
            "/containers",
            objectMapper.writeValueAsString(containerRequest)
        )
            .andExpect(status().isCreated)

        // Verify container was saved in the database
        val savedContainers = wasteContainerRepository.findAll()
        assertThat(savedContainers).hasSize(1)
        assertThat(savedContainers[0].id).isEqualTo("CONTAINER-001")
        assertThat(savedContainers[0].notes).isEqualTo("Test container with address")
        assertThat(savedContainers[0].address).isNotNull
        assertThat(savedContainers[0].address?.streetName).isEqualTo("Test Street")
        assertThat(savedContainers[0].company).isNull()
    }

    @Test
    fun `should successfully create a container with company reference`() {
        // Given
        val company = CompanyDto(
            name = "Test Company",
            address = AddressDto(
                streetName = "Company Street",
                buildingNumber = "456",
                city = "Company City",
                postalCode = "5678CD",
                country = "Company Country"
            )
        )
        companyRepository.save(company)

        val containerRequest = CreateContainerRequest(
            id = "CONTAINER-002",
            location = CreateContainerRequest.ContainerLocation(
                companyId = company.id,
                companyName = company.name,
                address = null
            ),
            notes = "Test container with company"
        )

        // When & Then
        securedMockMvc.post(
            "/containers",
            objectMapper.writeValueAsString(containerRequest)
        )
            .andExpect(status().isCreated)

        // Verify container was saved in the database
        val savedContainers = wasteContainerRepository.findAll()
        assertThat(savedContainers).hasSize(1)
        assertThat(savedContainers[0].id).isEqualTo("CONTAINER-002")
        assertThat(savedContainers[0].notes).isEqualTo("Test container with company")
        assertThat(savedContainers[0].address).isNull()
        assertThat(savedContainers[0].company).isNotNull
        assertThat(savedContainers[0].company?.id).isEqualTo(company.id)
    }

    @Test
    fun `should get all containers`() {
        // Given
        val container1 = WasteContainerDto(
            id = "GET-ALL-1",
            notes = "First test container"
        )
        container1.address = AddressDto(
            streetName = "First Street",
            buildingNumber = "1",
            city = "First City",
            postalCode = "1111AA",
            country = "First Country"
        )

        val container2 = WasteContainerDto(
            id = "GET-ALL-2",
            notes = "Second test container"
        )
        container2.address = AddressDto(
            streetName = "Second Street",
            buildingNumber = "2",
            city = "Second City",
            postalCode = "2222BB",
            country = "Second Country"
        )

        wasteContainerRepository.saveAll(listOf(container1, container2))

        // When & Then
        securedMockMvc.get("/containers")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[?(@.id == 'GET-ALL-1')]").exists())
            .andExpect(jsonPath("$[?(@.id == 'GET-ALL-2')]").exists())
    }

    @Test
    fun `should get container by id`() {
        // Given
        val container = WasteContainerDto(
            id = "GET-ONE",
            notes = "Test container to retrieve"
        )
        container.address = AddressDto(
            streetName = "Retrieve Street",
            buildingNumber = "123",
            city = "Retrieve City",
            postalCode = "3333CC",
            country = "Retrieve Country"
        )
        val containerId = wasteContainerRepository.save(container).uuid!!

        // When & Then
        securedMockMvc.get("/containers/$containerId")
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("GET-ONE"))
            .andExpect(jsonPath("$.notes").value("Test container to retrieve"))
            .andExpect(jsonPath("$.location.address.streetName").value("Retrieve Street"))
    }

    @Test
    fun `should return not found when getting container with non-existent id`() {
        // When & Then
        val nonExistentId = UUID.randomUUID()
        securedMockMvc.get("/containers/$nonExistentId")
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should delete container by id`() {
        // Given
        val container = WasteContainerDto(
            id = "DELETE-ME",
            notes = "Test container to delete"
        )
        container.address = AddressDto(
            streetName = "Delete Street",
            buildingNumber = "123",
            city = "Delete City",
            postalCode = "4444DD",
            country = "Delete Country"
        )
        val containerId = wasteContainerRepository.save(container).uuid!!

        // When & Then
        securedMockMvc.delete("/containers/$containerId")
            .andExpect(status().isNoContent)

        // Verify container was deleted
        assertThat(wasteContainerRepository.findByIdOrNull(containerId)).isNull()
    }

    @Test
    fun `should update container`() {
        // Given
        val originalContainer = WasteContainerDto(
            id = "UPDATE-ME",
            notes = "Original notes"
        )
        originalContainer.address = AddressDto(
            streetName = "Original Street",
            buildingNumber = "123",
            city = "Original City",
            postalCode = "5555EE",
            country = "Original Country"
        )
        val containerId = wasteContainerRepository.save(originalContainer).uuid!!

        val updatedContainer = WasteContainer(
            uuid = containerId,
            id = "UPDATE-ME",
            location = WasteContainer.ContainerLocation(
                companyId = null,
                companyName = null,
                address = AddressDto(
                    streetName = "Updated Street",
                    buildingNumber = "456",
                    city = "Updated City",
                    postalCode = "6666FF",
                    country = "Updated Country"
                )
            ),
            notes = "Updated notes"
        )

        // When & Then
        securedMockMvc.put(
            "/containers/$containerId",
            objectMapper.writeValueAsString(updatedContainer)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("UPDATE-ME"))
            .andExpect(jsonPath("$.notes").value("Updated notes"))
            .andExpect(jsonPath("$.location.address.streetName").value("Updated Street"))

        // Verify container was updated in the database
        val savedContainer = wasteContainerRepository.findByIdOrNull(containerId)
        assertThat(savedContainer).isNotNull
        assertThat(savedContainer?.id).isEqualTo("UPDATE-ME")
        assertThat(savedContainer?.notes).isEqualTo("Updated notes")
        assertThat(savedContainer?.address?.streetName).isEqualTo("Updated Street")
    }

    @Test
    fun `should return not found when updating non-existent container`() {
        // Given
        val nonExistentId = UUID.randomUUID()
        val container = WasteContainer(
            uuid = nonExistentId,
            id = "NON-EXISTENT",
            location = WasteContainer.ContainerLocation(
                companyId = null,
                companyName = null,
                address = AddressDto(
                    streetName = "Non-existent Street",
                    buildingNumber = "789",
                    city = "Non-existent City",
                    postalCode = "7777GG",
                    country = "Non-existent Country"
                )
            ),
            notes = "Non-existent container"
        )

        // When & Then
        securedMockMvc.put(
            "/containers/$nonExistentId",
            objectMapper.writeValueAsString(container)
        )
            .andExpect(status().isNotFound)
    }
}
