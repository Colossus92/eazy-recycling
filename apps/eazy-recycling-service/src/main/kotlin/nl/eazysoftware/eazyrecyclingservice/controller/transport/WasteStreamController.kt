package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.WasteStreamService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

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
    fun createWasteStream(@RequestBody wasteStreamDto: WasteStreamDto): WasteStreamDto {
        return wasteStreamService.createWasteStream(wasteStreamDto)
    }

    @PutMapping("/{number}")
    fun updateWasteStream(@PathVariable number: String, @RequestBody wasteStreamDto: WasteStreamDto): WasteStreamDto {
        return wasteStreamService.updateWasteStream(number, wasteStreamDto)
    }

    @DeleteMapping("/{number}")
    fun deleteWasteStream(@PathVariable number: String) {
        return wasteStreamService.deleteWasteStream(number)
    }
}