package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.application.query.GetTransportById
import nl.eazysoftware.eazyrecyclingservice.application.query.TransportDetailView
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ADMIN_OR_PLANNER
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.model.Roles
import nl.eazysoftware.eazyrecyclingservice.domain.service.TransportService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.transport.TransportDto
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.util.*

@RestController
@RequestMapping("/transport")
class TransportController(
    val transportService: TransportService,
    val getTransportById: GetTransportById
) {

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @GetMapping
    fun getAllTransports(): List<TransportDto> {
        return transportService.getAllTransports()
    }

    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping(path = ["/{id}"])
    fun getTransportById(@PathVariable id: UUID): TransportDetailView {
        val transport = getTransportById.execute(id)

        checkAuthorization(transport)

        return transport
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @DeleteMapping(path = ["/{id}"])
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    fun deleteTransport(@PathVariable("id") id: UUID) {
        transportService.deleteTransport(id)
    }

    @PreAuthorize(HAS_ANY_ROLE)
    @PostMapping(path = ["/{id}/finished"])
    @Transactional
    fun markTransportAsFinished(@PathVariable id: UUID, @RequestBody request: TransportFinishedRequest): TransportDto {
        val transport = transportService.getTransportById(id)

        checkAuthorization(transport)

        return transportService.markTransportAsFinished(id, request.hours, request.driverNote)
    }

    @PreAuthorize(HAS_ADMIN_OR_PLANNER)
    @PutMapping(path = ["/{id}/driver"])
    @Transactional
    fun updateTransportDriver(@PathVariable id: UUID, @RequestBody request: UpdateDriverRequest): TransportDto {
        val transport = transportService.getTransportById(id)
        val status = transport.getStatus()

        if (status != TransportDto.Status.PLANNED && status != TransportDto.Status.UNPLANNED) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Chauffeur kan alleen worden gewijzigd voor transporten met status gepland of ongepland"
            )
        }

        return transportService.updateTransportDriver(id, request.driverId)
    }

    data class TransportFinishedRequest(
      val hours: Double,
      val driverNote: String,
    )

    data class UpdateDriverRequest(
      val driverId: UUID?
    )

    private fun checkAuthorization(transport: TransportDetailView) {
        val authentication = SecurityContextHolder.getContext().authentication

        when (authentication) {
            is JwtAuthenticationToken -> {
                val userIdFromToken = authentication.token.subject
                val isAdminOrPlanner = authentication.authorities.any {
                    it.authority == Roles.ADMIN || it.authority == Roles.PLANNER
                }

                // If user is not admin/planner and not the driver of this transport
                if (!isAdminOrPlanner && transport.driver?.id?.toString() != userIdFromToken) {
                    throw ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Geen toegang: je hebt geen toegang tot dit transport"
                    )
                }
            }
            else -> throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Geen toegang: Je moet ingelogd zijn om dit transport te zien"
            )
        }
    }

    private fun checkAuthorization(transport: TransportDto) {
        val authentication = SecurityContextHolder.getContext().authentication

        when (authentication) {
            is JwtAuthenticationToken -> {
                val userIdFromToken = authentication.token.subject
                val isAdminOrPlanner = authentication.authorities.any {
                    it.authority == Roles.ADMIN || it.authority == Roles.PLANNER
                }

                // If user is not admin/planner and not the driver of this transport
                if (!isAdminOrPlanner && transport.driver?.id?.toString() != userIdFromToken) {
                    throw ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Geen toegang: je hebt geen toegang tot dit transport"
                    )
                }
            }
            else -> throw ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Geen toegang: Je moet ingelogd zijn om dit transport te zien"
            )
        }
    }
}
