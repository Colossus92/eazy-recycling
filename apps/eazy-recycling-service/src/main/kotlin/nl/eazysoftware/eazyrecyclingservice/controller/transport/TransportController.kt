package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.controller.CreateContainerTransportRequest
import nl.eazysoftware.eazyrecyclingservice.controller.CreateWaybillTransportRequest
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
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
        return transportService.createContainerTransport(request)
    }

    @PutMapping
    fun updateTransport(@RequestBody request: CreateContainerTransportRequest): TransportDto {
        return transportService.updateTransport(request)
    }

    @GetMapping("/planning/{pickupDate}")
    fun getPlanningByDate(@PathVariable("pickupDate") pickupDate: LocalDate): PlanningView {
        return transportService.getPlanningByDate(pickupDate)
    }
}