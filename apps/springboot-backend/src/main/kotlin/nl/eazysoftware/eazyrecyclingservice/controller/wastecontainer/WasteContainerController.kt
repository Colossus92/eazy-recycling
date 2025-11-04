package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.application.query.mappers.WasteContainerViewMapper
import nl.eazysoftware.eazyrecyclingservice.application.usecase.wastestream.toDomain
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.model.wastecontainer.WasteContainerId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ProjectLocations
import nl.eazysoftware.eazyrecyclingservice.domain.service.CompanyService
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteContainerService
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/containers")
class WasteContainerController(
  private val wasteContainerService: WasteContainerService,
  private val companyService: CompanyService,
  private val projectLocations: ProjectLocations,
  private val wasteContainerViewMapper: WasteContainerViewMapper,
) {

  @PostMapping
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.CREATED)
  fun createContainer(@RequestBody container: WasteContainerRequest) {
    wasteContainerService.createContainer(toDomain(container))
  }

  @GetMapping
  @PreAuthorize(HAS_ANY_ROLE)
  fun getAllContainers(): List<WasteContainerView> {
    return wasteContainerService.getAllContainers()
      .map { wasteContainerViewMapper.map(it) }
  }

  @GetMapping("/{id}")
  @PreAuthorize(HAS_ANY_ROLE)
  fun getContainerById(@PathVariable id: String): WasteContainerView {
    return wasteContainerViewMapper.map(wasteContainerService.getContainerById(id))

  }

  @DeleteMapping("/{id}")
  @PreAuthorize(HAS_ADMIN_OR_PLANNER)
  @ResponseStatus(HttpStatus.NO_CONTENT)
  fun deleteContainer(@PathVariable id: String) {
    wasteContainerService.deleteContainer(id)
  }

  @PutMapping("/{id}")
  @PreAuthorize(HAS_ANY_ROLE)
  @ResponseStatus(HttpStatus.OK)
  fun updateContainer(@PathVariable id: String, @RequestBody request: WasteContainerRequest): WasteContainerView {
    return wasteContainerViewMapper.map(wasteContainerService.updateContainer(id, toDomain(request)))
  }

  fun toDomain(request: WasteContainerRequest) = WasteContainer(
    wasteContainerId = WasteContainerId(request.id),
    location = request.location?.toCommand()?.toDomain(companyService, projectLocations),
    notes = request.notes
  )
}
