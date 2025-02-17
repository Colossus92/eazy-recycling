package nl.eazysoftware.springtemplate.controller

import jakarta.websocket.server.PathParam
import nl.eazysoftware.springtemplate.domain.mapper.TransportService
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/transport")
class TransportController(
    val transportService: TransportService
) {

    @GetMapping
    fun getAllTransports(): List<TransportDto> {
        return transportService.getAllTransports()
    }

    @PostMapping
    fun assignTransport(@RequestBody request: CreateTransportRequest): TransportDto {
        return transportService.assignTransport(request.waybillId, request.licensePlate, request.driverId)
    }

    @GetMapping("/{pickupDate}")
    fun getTransportByDateSortedByTruck(@PathVariable("pickupDate") pickupDate: LocalDate): Map<String, List<TransportDto>> {
        return transportService.getTransportByDateSortedByTruck(pickupDate)
    }
}