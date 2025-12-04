package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

import jakarta.persistence.EntityNotFoundException
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.ExactOnlineSync
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface UpdateCompany {
  fun handle(cmd: UpdateCompanyCommand): CompanyResult
  fun handleRestore(companyId: CompanyId, cmd: UpdateCompanyCommand): CompanyResult
}

@Service
class UpdateCompanyService(
  private val companies: Companies,
  private val exactOnlineSync: ExactOnlineSync,
  private val companySyncRepository: CompanySyncRepository,
) : UpdateCompany {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun handle(cmd: UpdateCompanyCommand): CompanyResult {
    // Verify company exists
    val existingCompany = companies.findById(cmd.companyId)
      ?: throw EntityNotFoundException("Bedrijf met id ${cmd.companyId.uuid} niet gevonden")

    // Validate uniqueness constraints for fields that changed (VIHB only - KVK duplicates are allowed, see ADR-0018)
    cmd.vihbNumber?.let { vihb ->
      if (vihb != existingCompany.vihbNumber && companies.existsByVihbNumber(vihb.value)) {
        throw DuplicateKeyException("VIHB nummer ${vihb.value} is al in gebruik.")
      }
    }

    val updatedCompany = Company(
      companyId = cmd.companyId,
      name = cmd.name,
      chamberOfCommerceId = cmd.chamberOfCommerceId,
      vihbNumber = cmd.vihbNumber,
      processorId = cmd.processorId,
      address = cmd.address,
      phone = cmd.phone,
      email = cmd.email,
      roles = cmd.roles,
    )

    companies.update(updatedCompany)

    // Sync to Exact Online - catch exceptions to prevent rollback of company update
    try {
      exactOnlineSync.updateCompany(updatedCompany)
    } catch (e: Exception) {
      logger.error("Failed to sync company ${updatedCompany.companyId.uuid} to Exact Online", e)

      // Update or create failed sync record
      val existingSync = companySyncRepository.findByCompanyId(updatedCompany.companyId.uuid)
      if (existingSync != null) {
        val updatedSync = existingSync.copy(
          syncStatus = SyncStatus.FAILED,
          syncedFromSourceAt = Instant.now(),
          syncErrorMessage = "${e::class.simpleName}: ${e.message}",
          updatedAt = Instant.now()
        )
        companySyncRepository.save(updatedSync)
      } else {
        // No existing sync record - create one with failed status
        val syncDto = CompanySyncDto(
          companyId = updatedCompany.companyId.uuid,
          syncStatus = SyncStatus.FAILED,
          syncedFromSourceAt = Instant.now(),
          syncErrorMessage = "${e::class.simpleName}: ${e.message}",
        )
        companySyncRepository.save(syncDto)
      }
    }

    return CompanyResult(companyId = cmd.companyId.uuid)
  }

  @Transactional
  override fun handleRestore(companyId: CompanyId, cmd: UpdateCompanyCommand): CompanyResult {
    // First, restore the soft-deleted company
    companies.restore(companyId)

    // Then update it with the new data
    return handle(cmd)
  }
}
