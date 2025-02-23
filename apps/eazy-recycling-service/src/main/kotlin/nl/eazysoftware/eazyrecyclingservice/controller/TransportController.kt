package nl.eazysoftware.eazyrecyclingservice.controller

import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
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

    @PutMapping
    fun updateTransport(@RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.updateTransport(request)
    }

    @GetMapping("/{pickupDate}")
    fun getTransportByDateSortedByTruck(@PathVariable("pickupDate") pickupDate: LocalDate): Map<String, List<TransportDto>> {
        return transportService.getTransportByDateSortedByTruck(pickupDate)
    }
}