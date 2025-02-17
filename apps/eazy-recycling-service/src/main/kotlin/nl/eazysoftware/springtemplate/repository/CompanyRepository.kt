package nl.eazysoftware.springtemplate.repository

import nl.eazysoftware.springtemplate.repository.entity.waybill.CompanyDto
import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface CompanyRepository: JpaRepository<CompanyDto, UUID> {
    fun findByChamberOfCommerceIdAndVihbId(chamberOfCommerceId: String?, vihbId: String?): CompanyDto?
}