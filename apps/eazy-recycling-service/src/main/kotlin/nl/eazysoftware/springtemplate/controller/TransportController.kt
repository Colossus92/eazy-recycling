package nl.eazysoftware.springtemplate.controller

import jakarta.websocket.server.PathParam
import nl.eazysoftware.springtemplate.domain.mapper.TransportService
import nl.eazysoftware.springtemplate.repository.entity.transport.TransportDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
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

    @GetMapping("/{pickupDate}")
    fun getTransportByDateSortedByTruck(@PathParam("pickupDate") pickupDate: LocalDate): Map<String, List<TransportDto>> {
        return transportService.getTranspsortByDateSortedByTruck(pickupDate)
    }
}