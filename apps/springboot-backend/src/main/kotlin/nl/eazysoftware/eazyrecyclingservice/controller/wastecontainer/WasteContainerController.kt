package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteContainerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/containers")
class WasteContainerController(
  private val wasteContainerService: WasteContainerService,
) {

  @PostMapping
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.CREATED)
  fun createContainer(@RequestBody container: CreateContainerRequest) {
    wasteContainerService.createContainer(container)
  }

  @GetMapping
  @PreAuthorize(HAS_ANY_ROLE)
  fun getAllContainers(): List<WasteContainerView> {
    return wasteContainerService.getAllContainers()
      .map { it.toView() }
  }

  @GetMapping("/{id}")
  @PreAuthorize(HAS_ANY_ROLE)
  fun getContainerById(@PathVariable id: UUID): WasteContainerView {
    return wasteContainerService.getContainerById(id).toView()

  }

  @DeleteMapping("/{id}")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteContainer(@PathVariable id: UUID) {
    wasteContainerService.deleteContainer(id)
  }

  @PutMapping("/{id}")
  @PreAuthorize(HAS_ANY_ROLE)
  @ResponseStatus(HttpStatus.OK)
  fun updateContainer(@PathVariable id: UUID, @RequestBody request: WasteContainerRequest): WasteContainerView {
    return wasteContainerService.updateContainer(id, request.toDomain()).toView()
  }

  data class WasteContainerRequest(
    val uuid: UUID,
    val id: String,
    val location: WasteContainer.ContainerLocation?,
    val notes: String?,
  ) {
    fun toDomain() = WasteContainer(
      wasteContainerId = WasteContainerId(uuid),
      id = id,
      location = location,
      notes = notes
    )
  }

}
