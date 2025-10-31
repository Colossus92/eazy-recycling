package nl.eazysoftware.eazyrecyclingservice.domain.service

import jakarta.persistence.EntityNotFoundException
import jakarta.transaction.Transactional
import nl.eazysoftware.eazyrecyclingservice.controller.request.AddressRequest
import nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer.CreateContainerRequest
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.WasteContainerRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerDto
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.WasteContainerMapper
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.AddressDto
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.util.*

@Service
class WasteContainerService(
  private val wasteContainerRepository: WasteContainerRepository,
  private val wasteContainerMapper: WasteContainerMapper,
  private val companyRepository: CompanyRepository,
) {

  @Transactional
  fun createContainer(container: CreateContainerRequest) {
    if (wasteContainerRepository.existsById(container.id)) {
      throw DuplicateKeyException("Container met kenmerk ${container.id} bestaat al")
    }
    wasteContainerRepository.save(requestToDto(container))
  }

  fun getAllContainers(): List<WasteContainer> {
    return wasteContainerRepository.findAll()
      .map { wasteContainerMapper.toDomain(it) }
      .sortedBy { it.wasteContainerId.id }
  }

  fun getContainerById(id: String): WasteContainer {
    return wasteContainerRepository.findById(id)
      .orElseThrow { (EntityNotFoundException("Container with id $id not found")) }
      .let { wasteContainerMapper.toDomain(it) }
  }

  @Transactional
  fun updateContainer(id: String, container: WasteContainer): WasteContainer {
    wasteContainerRepository.findById(id)
      .orElseThrow { (EntityNotFoundException("Container with id $id not found")) }

    val dto = toDto(container)

    wasteContainerRepository.save(dto)

    return container
  }

  fun deleteContainer(id: String) {
    wasteContainerRepository.deleteById(id)
  }

  private fun toDto(container: WasteContainer): WasteContainerDto {
    val dto = WasteContainerDto(
      id = container.wasteContainerId.id,
      notes = container.notes,
    )

    setLocationDetails(dto, container.location?.companyId, container.location?.address)

    return dto
  }

  private fun requestToDto(container: CreateContainerRequest): WasteContainerDto {
    val dto = WasteContainerDto(
      id = container.id,
      notes = container.notes,
    )

    val address = container.location?.address?.let { toAddressDto(it) }
    setLocationDetails(dto, container.location?.companyId, address)

    return dto
  }

  private fun toAddressDto(address: AddressRequest): AddressDto = AddressDto(
    streetName = address.streetName,
    buildingName = address.buildingNumberAddition,
    buildingNumber = address.buildingNumber,
    postalCode = address.postalCode,
    city = address.city,
    country = address.country
  )

  private fun setLocationDetails(
    dto: WasteContainerDto,
    companyId: UUID?,
    address: AddressDto?
  ) {
    companyId?.run {
      val company: CompanyDto = companyRepository.findById(this)
        .orElseThrow { EntityNotFoundException("Bedrijf met id $this niet gevonden") }
      dto.company = company
    } ?: run {
      dto.address = address
    }
  }
}
