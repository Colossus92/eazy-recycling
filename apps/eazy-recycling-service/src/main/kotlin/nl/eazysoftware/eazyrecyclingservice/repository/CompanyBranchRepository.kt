package nl.eazysoftware.eazyrecyclingservice.repository

import nl.eazysoftware.eazyrecyclingservice.repository.entity.company.CompanyBranchDto
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.*

interface CompanyBranchRepository: JpaRepository<CompanyBranchDto, UUID> {
    
    @Query("SELECT COUNT(b) > 0 FROM CompanyBranchDto b WHERE b.company.id = :companyId AND b.address.postalCode = :postalCode AND b.address.buildingNumber = :buildingNumber")
    fun existsByCompanyIdAndPostalCodeAndBuildingNumber(
        @Param("companyId") companyId: UUID,
        @Param("postalCode") postalCode: String,
        @Param("buildingNumber") buildingNumber: String
    ): Boolean
}