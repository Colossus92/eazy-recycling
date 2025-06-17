package nl.eazysoftware.eazyrecyclingservice.controller.transport.signature

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.CreateSignatureRequest
import nl.eazysoftware.eazyrecyclingservice.domain.service.SignatureService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/transport/{id}/signature")
class SignatureController(
    val signatureService: SignatureService,
) {

    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping
    fun getSignatureStatuses(@PathVariable id: UUID) = signatureService.getSignatureStatuses(id)

    @PreAuthorize(HAS_ANY_ROLE)
    @PostMapping
    fun saveSignature(@PathVariable id: UUID, @RequestBody request: CreateSignatureRequest) = signatureService.saveSignature(id, request)
}
