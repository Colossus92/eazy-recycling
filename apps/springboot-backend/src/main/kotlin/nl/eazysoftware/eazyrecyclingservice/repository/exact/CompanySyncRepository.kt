package nl.eazysoftware.eazyrecyclingservice.repository.exact

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface CompanySyncRepository : JpaRepository<CompanySyncDto, UUID> {
    fun findByCompanyId(companyId: UUID): CompanySyncDto?
}
