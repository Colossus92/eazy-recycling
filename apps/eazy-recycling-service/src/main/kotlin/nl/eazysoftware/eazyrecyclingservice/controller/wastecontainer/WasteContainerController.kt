package nl.eazysoftware.eazyrecyclingservice.controller.wastecontainer

import nl.eazysoftware.eazyrecyclingservice.domain.model.WasteContainer
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteContainerService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/containers")
class WasteContainerController(
    private val wasteContainerService: WasteContainerService,
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createContainer(@RequestBody container: CreateContainerRequest) {
        wasteContainerService.createContainer(container)
    }

    @GetMapping
    fun getAllContainers(): List<WasteContainer> {
        return wasteContainerService.getAllContainers()
    }

    @GetMapping("/{id}")
    fun getContainerByLicensePlate(@PathVariable id: UUID): WasteContainer {
        return wasteContainerService.getContainerById(id)
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteContainer(@PathVariable id: UUID) {
        wasteContainerService.deleteContainer(id)
    }

    @PutMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    fun updateContainer(@PathVariable id: UUID, @RequestBody container: WasteContainer): WasteContainer {
        return wasteContainerService.updateContainer(id, container)
    }
}