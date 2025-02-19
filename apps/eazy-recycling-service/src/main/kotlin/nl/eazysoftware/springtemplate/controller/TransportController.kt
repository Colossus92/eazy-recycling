package nl.eazysoftware.springtemplate.controller

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

    @PostMapping("/waybill")
    fun assignWaybillTransport(@RequestBody request: CreateWaybillTransportRequest): TransportDto {
        return transportService.assignWaybillTransport(request.waybillId, request.licensePlate, request.driverId)
    }

    @PostMapping("/container")
    fun createContainerTransport(@RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.assignContainerTransport(request)
    }

    @GetMapping("/{pickupDate}")
    fun getTransportByDateSortedByTruck(@PathVariable("pickupDate") pickupDate: LocalDate): Map<String, List<TransportDto>> {
        return transportService.getTransportByDateSortedByTruck(pickupDate)
    }
}