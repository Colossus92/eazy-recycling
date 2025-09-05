package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.ProcessingMethod
import org.springframework.data.jpa.repository.JpaRepository

interface ProcessingMethodRepository: JpaRepository<ProcessingMethod, String>