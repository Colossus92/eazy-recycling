package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.ProcessingMethodService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethod
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/processing-methods")
@PreAuthorize(HAS_ANY_ROLE)
class ProcessingMethodController(
    private val processingMethodService: ProcessingMethodService
) {


    @GetMapping
    fun getProcessingMethods(): List<ProcessingMethod> {
        return processingMethodService.findAll()
    }
}