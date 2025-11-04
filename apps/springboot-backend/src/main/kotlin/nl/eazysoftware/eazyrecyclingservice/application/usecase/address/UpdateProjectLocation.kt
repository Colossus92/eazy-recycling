package nl.eazysoftware.eazyrecyclingservice.application.usecase.address

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface UpdateProjectLocation {
  fun handle(cmd: UpdateProjectLocationCommand)
}

data class UpdateProjectLocationCommand(
  val companyId: CompanyId,
  val projectLocationId: UUID,
  val address: Address
)

@Service
class UpdateProjectLocationService(
  private val projectLocations: ProjectLocations,
  private val companyRepository: CompanyRepository, // Todo: use domain port when available
) : UpdateProjectLocation {

  @Transactional
  override fun handle(cmd: UpdateProjectLocationCommand) {
    companyRepository.findByIdOrNull(cmd.companyId.uuid)
      ?: throw EntityNotFoundException("Bedrijf met id ${cmd.companyId.uuid} niet gevonden")
    val projectLocation = projectLocations.findById(cmd.projectLocationId)
      ?: throw EntityNotFoundException("Vestiging met id ${cmd.projectLocationId} niet gevonden")

    if (projectLocation.companyId != cmd.companyId) {
      throw IllegalArgumentException("Vestiging met id ${cmd.projectLocationId} is niet van bedrijf met id ${cmd.companyId.uuid}")
    }

    projectLocation.updateAddress(cmd.address)

    projectLocations.update(projectLocation)
  }
}
