package nl.eazysoftware.springtemplate.domain.mapper

import jakarta.persistence.EntityManager
import nl.eazysoftware.springtemplate.repository.CompanyRepository
import nl.eazysoftware.springtemplate.repository.WaybillRepository
import nl.eazysoftware.springtemplate.repository.entity.waybill.CompanyDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class WaybillService(
    val waybillRepository: WaybillRepository,
    val companyRepository: CompanyRepository,
    val entityManager: EntityManager,
) {
    fun getUnassignedWaybillsByDate(pickupDate: LocalDate): List<WaybillDto> {
        val start = pickupDate.atStartOfDay()
        val end = pickupDate.atTime(23, 59, 59)
        return waybillRepository.findUnassignedWaybills(start, end)
    }

    fun findAll(): List<WaybillDto> {
        return waybillRepository.findAll()
    }

    @Transactional
    fun save(waybillDto: WaybillDto): WaybillDto {
        val consignee = findCompany(waybillDto.consigneeParty)
        val consignor = findCompany(waybillDto.consignorParty)
        val carrier = findCompany(waybillDto.carrierParty)
        val pickup = findCompany(waybillDto.pickupParty)

        val updatedWaybill = waybillDto.copy(
            consigneeParty = consignee,
            consignorParty = consignor,
            carrierParty = carrier,
            pickupParty = pickup
        )

        return entityManager.merge(updatedWaybill)
    }

    fun findCompany(company: CompanyDto): CompanyDto {
        return companyRepository.findByChamberOfCommerceIdAndVihbId(company.chamberOfCommerceId, company.vihbId)
            ?: company
    }
}