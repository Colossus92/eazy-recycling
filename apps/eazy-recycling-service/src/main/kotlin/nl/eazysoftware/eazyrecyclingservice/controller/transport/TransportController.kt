package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/transport")
class TransportController(
    val transportService: TransportService
) {

    @GetMapping
    fun getAllTransports(): List<TransportDto> {
        return transportService.getAllTransports()
    }

    @GetMapping(path = ["/{id}"])
    fun getTransportById(@PathVariable id: UUID): TransportDto {
        return transportService.getTransportById(id)
    }

    @PostMapping("/waybill")
    fun assignWaybillTransport(@RequestBody request: AssignWaybillTransportRequest): TransportDto {
        return transportService.assignWaybillTransport(request.waybillId, request.licensePlate, request.driverId)
    }

    @PostMapping("/container")
    fun createContainerTransport(@RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.createContainerTransport(request)
    }

    @PutMapping(path = ["/container/{id}"])
    fun updateContainerTransport(@PathVariable id: UUID, @RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.updateContainerTransport(id, request)
    }

    @PostMapping("/waste")
    fun createWasteTransport(@RequestBody request: CreateWasteTransportRequest): TransportDto {
        return transportService.createWasteTransport(request)
    }

    @PutMapping(path = ["/waste/{id}"])
    fun updateWasteTransport(@PathVariable id: UUID, @RequestBody request: CreateWasteTransportRequest): TransportDto {
        return transportService.updateWasteTransport(id, request)
    }

    @DeleteMapping(path = ["/{id}"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTransport(@PathVariable("id") id: UUID) {
        transportService.deleteTransport(id)
    }
}