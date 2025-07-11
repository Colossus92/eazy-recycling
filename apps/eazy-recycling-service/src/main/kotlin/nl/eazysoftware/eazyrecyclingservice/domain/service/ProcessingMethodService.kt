package nl.eazysoftware.eazyrecyclingservice.domain.service

import nl.eazysoftware.eazyrecyclingservice.repository.ProcessingMethodRepository
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethod
import org.springframework.stereotype.Service

@Service
class ProcessingMethodService(
    private val processingMethodRepository: ProcessingMethodRepository
) {

    fun findAll(): List<ProcessingMethod> =
        processingMethodRepository.findAll()

}