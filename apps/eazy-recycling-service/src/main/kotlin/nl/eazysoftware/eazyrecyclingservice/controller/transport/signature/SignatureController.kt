package nl.eazysoftware.eazyrecyclingservice.controller.transport.signature

import jakarta.validation.Valid
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.CreateSignatureRequest
import nl.eazysoftware.eazyrecyclingservice.domain.service.WaybillDocumentService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/transport/{id}/waybill")
class SignatureController(
    val waybillDocumentService: WaybillDocumentService,
) {

    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping("/signature-status")
    fun getSignatureStatuses(@PathVariable id: UUID) = waybillDocumentService.getSignatureStatuses(id)

    @PreAuthorize(HAS_ANY_ROLE)
    @PostMapping("/signature")
    fun saveSignature(@PathVariable id: UUID, @Valid @RequestBody request: CreateSignatureRequest) =
        waybillDocumentService.saveSignature(id, request)

}