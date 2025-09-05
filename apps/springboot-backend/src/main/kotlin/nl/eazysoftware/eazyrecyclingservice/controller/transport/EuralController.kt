package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.EuralService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/eural")
@PreAuthorize(HAS_ANY_ROLE)
class EuralController(
    private val euralService: EuralService
) {


    @GetMapping
    fun getEural(): List<Eural> {
        return euralService.getEurals()
    }
}