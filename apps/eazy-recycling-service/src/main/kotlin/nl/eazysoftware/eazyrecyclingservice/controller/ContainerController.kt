package nl.eazysoftware.eazyrecyclingservice.controller

import nl.eazysoftware.eazyrecyclingservice.domain.service.ContainerService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.container.Container
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/containers")
class ContainerController(
    private val containerService: ContainerService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTruck(@RequestBody container: Container) {
        containerService.createContainer(container)
    }

    @GetMapping
    fun getAllContainers(): List<Container> {
        return containerService.getAllContainers()
    }

    @GetMapping("/{id}")
    fun getContainerByLicensePlate(@PathVariable id: UUID): Container {
        return containerService.getContainerById(id)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteContainer(@PathVariable id: UUID) {
        containerService.deleteContainer(id)
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateContainer(@PathVariable id: UUID, @RequestBody container: Container): Container {
        return containerService.updateContainer(id, container)
    }
}