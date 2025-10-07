package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ROLE_ADMIN
import nl.eazysoftware.eazyrecyclingservice.domain.service.EuralService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.Eural
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/eural")
class EuralController(
  private val euralService: EuralService
) {


  @PreAuthorize(HAS_ANY_ROLE)
  @GetMapping
  fun getEural() =
    euralService.getEurals()

  @PreAuthorize(HAS_ROLE_ADMIN)
  @PostMapping
  fun createEural(@RequestBody eural: Eural) =
    euralService.createEural(eural)
}
