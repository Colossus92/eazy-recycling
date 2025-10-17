package nl.eazysoftware.eazyrecyclingservice.application.usecase.address

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface CreateProjectLocation  {
  fun handle(cmd: CreateProjectLocationCommand)
}

data class CreateProjectLocationCommand(
  val companyId: CompanyId,
  val address: Address
)

@Service
class CreateProjectLocationService(
  private val projectLocations: ProjectLocations,
  private val companyRepository: CompanyRepository, //TODO replace with domain ports
) : CreateProjectLocation {

  @Transactional
  override fun handle(cmd: CreateProjectLocationCommand) {
    if (projectLocations.existsByCompanyIdAndPostalCodeAndBuildingNumber(
        cmd.companyId,
        cmd.address.postalCode,
        cmd.address.buildingNumber,
      )
    ) {
      throw DuplicateKeyException("Er bestaat al een vestiging op dit adres (postcode en huisnummer) voor dit bedrijf.")
    }
    val company = companyRepository.findById(cmd.companyId.uuid)
      .orElseThrow { EntityNotFoundException("Bedrijf met id ${cmd.companyId.uuid} niet gevonden") }

    val location = Location.ProjectLocation(
      companyId = cmd.companyId,
      address = cmd.address,
    )

    projectLocations.create(location)
  }
}
