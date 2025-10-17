package nl.eazysoftware.eazyrecyclingservice.application.usecase.address

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

interface DeleteProjectLocation {
  fun handle(cmd: DeleteProjectLocationCommand)
}

data class DeleteProjectLocationCommand(
  val companyId: CompanyId,
  val projectLocationId: UUID
)

@Service
class DeleteProjectLocationService(
  private val projectLocations: ProjectLocations,
) : DeleteProjectLocation {

  @Transactional
  override fun handle(cmd: DeleteProjectLocationCommand) {
    val branch = projectLocations.findById(cmd.projectLocationId)
      ?: throw EntityNotFoundException("Vestiging met id ${cmd.projectLocationId} niet gevonden")

    if (branch.companyId != cmd.companyId) {
      throw IllegalArgumentException("Vestiging met id ${cmd.projectLocationId} is niet van bedrijf met id ${cmd.companyId.uuid}")
    }

    projectLocations.deleteById(cmd.projectLocationId)
  }
}
