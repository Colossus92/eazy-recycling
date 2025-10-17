package nl.eazysoftware.eazyrecyclingservice.application.usecase.address

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
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
) : UpdateProjectLocation {

  @Transactional
  override fun handle(cmd: UpdateProjectLocationCommand) {
    val updatedLocation = Location.ProjectLocation(
      id = cmd.projectLocationId,
      companyId = cmd.companyId,
      address = cmd.address
    )

    projectLocations.update(updatedLocation)
  }
}
