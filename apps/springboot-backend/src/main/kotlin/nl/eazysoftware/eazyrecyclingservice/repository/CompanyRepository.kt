package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyDto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CompanyRepository: JpaRepository<CompanyDto, UUID> {

    fun findByProcessorId(processorId: String): CompanyDto?
}
