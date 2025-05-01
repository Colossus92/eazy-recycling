package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.controller.CreateContainerRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class WasteContainerService(
    private val wasteContainerRepository: WasteContainerRepository,
    private val wasteContainerMapper: WasteContainerMapper,
    private val companyRepository: CompanyRepository,
) {

    @Transactional
    fun createContainer(container: CreateContainerRequest) {
        wasteContainerRepository.save(requestToDto(container))
    }

    fun getAllContainers(): List<WasteContainer> {
        return wasteContainerRepository.findAll()
            .map { it -> wasteContainerMapper.toDomain(it) }
    }

    fun getContainerById(id: UUID): WasteContainer {
        return wasteContainerRepository.findById(id)
            .orElseThrow { (EntityNotFoundException("Container with id $id not found")) }
            .let { wasteContainerMapper.toDomain(it) }
    }

    @Transactional
    fun updateContainer(id: UUID, container: WasteContainer): WasteContainer {
        wasteContainerRepository.findById(id)
            .orElseThrow { (EntityNotFoundException("Container with id $id not found")) }

        val dto = toDto(container)

        wasteContainerRepository.save(dto)

        return container
    }

    fun deleteContainer(id: UUID) {
        wasteContainerRepository.deleteById(id)
    }

    private fun toDto(container: WasteContainer): WasteContainerDto {
        val dto = WasteContainerDto(
            uuid = container.uuid,
            id = container.id,
            notes = container.notes,
        )

        setLocationDetails(dto, container.location.companyId, container.location.address)

        return dto
    }

    private fun requestToDto(container: CreateContainerRequest): WasteContainerDto {
        val dto = WasteContainerDto(
            id = container.id,
            notes = container.notes,
        )

        setLocationDetails(dto, container.location.companyId, container.location.address)

        return dto
    }

    private fun setLocationDetails(
        dto: WasteContainerDto,
        companyId: UUID?,
        address: AddressDto?
    ) {
        companyId?.run {
            val company: CompanyDto = companyRepository.findById(this)
                .orElseThrow { EntityNotFoundException("Company with id $this not found") }
            dto.company = company
        } ?: run {
            dto.address = address
        }
    }
}