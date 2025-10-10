package nl.eazysoftware.eazyrecyclingservice.controller.transport

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.ProcessingMethodService
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/processing-methods")
@PreAuthorize(HAS_ANY_ROLE)
class ProcessingMethodController(
  private val processingMethodService: ProcessingMethodService
) {

  @GetMapping
  fun getProcessingMethods() =
    processingMethodService.findAll()

  @PostMapping
  fun createProcessingMethod(@RequestBody processingMethod: ProcessingMethodDto) =
    processingMethodService.create(processingMethod)

  @PutMapping("/{code}")
  fun updateProcessingMethod(@PathVariable code: String, @RequestBody processingMethod: ProcessingMethodDto) =
    processingMethodService.update(code, processingMethod)

  @DeleteMapping("/{code}")
  fun deleteProcessingMethod(@PathVariable code: String) =
    processingMethodService.delete(code)
}
