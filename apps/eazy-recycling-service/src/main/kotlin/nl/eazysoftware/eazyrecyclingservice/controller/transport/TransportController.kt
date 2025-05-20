package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.controller.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

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
    fun assignWaybillTransport(@RequestBody request: CreateWaybillTransportRequest): TransportDto {
        return transportService.assignWaybillTransport(request.waybillId, request.licensePlate, request.driverId)
    }

    @PostMapping("/container")
    fun createContainerTransport(@RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.createContainerTransport(request)
    }

    @PutMapping(path = ["/{id}"])
    fun updateTransport(@PathVariable id: UUID, @RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.updateTransport(id, request)
    }

    @GetMapping("/planning/{pickupDate}")
    fun getPlanningByDate(
        @PathVariable pickupDate: LocalDate,
        @RequestParam(required = false) truckId: String? = null,
        @RequestParam(required = false) driverId: UUID? = null,
        @RequestParam(required = false) status: String? = null
    ): PlanningView {
        return transportService.getPlanningByDate(pickupDate, truckId, driverId, status)
    }

    @DeleteMapping(path = ["/{id}"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTransport(@PathVariable("id") id: UUID) {
        transportService.deleteTransport(id)
    }
}