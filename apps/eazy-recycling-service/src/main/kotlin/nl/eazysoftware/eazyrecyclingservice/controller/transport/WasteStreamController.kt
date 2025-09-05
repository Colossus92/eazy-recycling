package nl.eazysoftware.eazyrecyclingservice.controller.transport

import jakarta.validation.Valid
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteStreamService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/waste-streams")
@PreAuthorize(HAS_ANY_ROLE)
class WasteStreamController(
    private val wasteStreamService: WasteStreamService,
) {


    @GetMapping
    fun getWasteStreams(): List<WasteStreamDto> {
        return wasteStreamService.getWasteStreams()
    }

    @PostMapping
    fun createWasteStream(@Valid @RequestBody wasteStreamDto: WasteStreamDto): WasteStreamDto {
        return wasteStreamService.createWasteStream(wasteStreamDto)
    }

    @PutMapping("/{number}")
    fun updateWasteStream(@PathVariable number: String, @Valid @RequestBody wasteStreamDto: WasteStreamDto): WasteStreamDto {
        return wasteStreamService.updateWasteStream(number, wasteStreamDto)
    }

    @DeleteMapping("/{number}")
    fun deleteWasteStream(@PathVariable number: String) {
        return wasteStreamService.deleteWasteStream(number)
    }
}