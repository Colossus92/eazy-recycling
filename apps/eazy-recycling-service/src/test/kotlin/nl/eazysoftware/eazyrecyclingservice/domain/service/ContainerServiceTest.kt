package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentCaptor
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

@ExtendWith(MockitoExtension::class)
class ContainerServiceTest {

    @Mock
    private lateinit var wasteContainerRepository: WasteContainerRepository

    @Mock
    private lateinit var wasteContainerMapper: WasteContainerMapper

    @Mock
    private lateinit var companyRepository: CompanyRepository

    @Captor
    private lateinit var wasteContainerDtoCaptor: ArgumentCaptor<WasteContainerDto>

    private lateinit var wasteContainerService: WasteContainerService

    @BeforeEach
    fun setUp() {
        wasteContainerService = WasteContainerService(
            wasteContainerRepository,
            wasteContainerMapper,
            companyRepository,
        )
    }

    @Test
    fun `updateContainer should set company but not address when companyId is provided`() {
        // Given
        val containerId = UUID.randomUUID()
        val companyId = UUID.randomUUID()
        val existingContainerDto = WasteContainerDto(
            uuid = containerId,
            id = "container-123",
            notes = "Some notes"
        )
        val company = CompanyDto(
            id = companyId,
            name = "Test Company",
            address = AddressDto(
                streetName = "Test Street",
                buildingName = "Test Building",
                buildingNumber = "123",
                postalCode = "1234 AB",
                city = "Test City",
                country = "Test Country"
            )
        )
        val containerToUpdate = WasteContainer(
            uuid = containerId,
            id = "container-123",
            location = WasteContainer.ContainerLocation(
                companyId = companyId,
                companyName = "Test Company",
                address = null
            ),
            notes = "Updated notes"
        )

        // When
        `when`(wasteContainerRepository.findById(containerId)).thenReturn(Optional.of(existingContainerDto))
        `when`(companyRepository.findById(companyId)).thenReturn(Optional.of(company))

        wasteContainerService.updateContainer(containerId, containerToUpdate)

        // Then
        verify(wasteContainerRepository).save(wasteContainerDtoCaptor.capture())
        val savedDto = wasteContainerDtoCaptor.value
        
        assertThat(savedDto.company).isEqualTo(company)
        assertThat(savedDto.address).isNull()
    }

    @Test
    fun `updateContainer should set address but not company when companyId is not provided`() {
        // Given
        val containerId = UUID.randomUUID()
        val address = AddressDto(
            streetName = "Test Street",
            buildingName = "Test Building",
            buildingNumber = "123",
            postalCode = "1234 AB",
            city = "Test City",
            country = "Test Country"
        )
        val existingContainerDto = WasteContainerDto(
            uuid = containerId,
            id = "container-123",
            notes = "Some notes"
        )
        val containerToUpdate = WasteContainer(
            uuid = containerId,
            id = "container-123",
            location = WasteContainer.ContainerLocation(
                companyId = null,
                companyName = null,
                address = address
            ),
            notes = "Updated notes"
        )

        // When
        `when`(wasteContainerRepository.findById(containerId)).thenReturn(Optional.of(existingContainerDto))

        wasteContainerService.updateContainer(containerId, containerToUpdate)

        // Then
        verify(wasteContainerRepository).save(wasteContainerDtoCaptor.capture())
        val savedDto = wasteContainerDtoCaptor.value
        
        assertThat(savedDto.company).isNull()
        assertThat(savedDto.address).isEqualTo(address)
    }

    @Test
    fun `updateContainer should throw EntityNotFoundException when company not found`() {
        // Given
        val containerId = UUID.randomUUID()
        val nonExistentCompanyId = UUID.randomUUID()
        val existingContainerDto = WasteContainerDto(
            uuid = containerId,
            id = "container-123",
            notes = "Some notes"
        )
        val containerToUpdate = WasteContainer(
            uuid = containerId,
            id = "container-123",
            location = WasteContainer.ContainerLocation(
                companyId = nonExistentCompanyId,
                companyName = "Non-existent Company",
                address = null
            ),
            notes = "Updated notes"
        )

        // When
        `when`(wasteContainerRepository.findById(containerId)).thenReturn(Optional.of(existingContainerDto))
        `when`(companyRepository.findById(nonExistentCompanyId)).thenReturn(Optional.empty())

        // Then
        assertThrows(EntityNotFoundException::class.java) {
            wasteContainerService.updateContainer(containerId, containerToUpdate)
        }
    }
}
