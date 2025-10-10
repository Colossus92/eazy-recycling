package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethodDto
import org.springframework.data.jpa.repository.JpaRepository

interface ProcessingMethodRepository: JpaRepository<ProcessingMethodDto, String>
