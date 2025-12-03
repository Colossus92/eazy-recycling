package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

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
import java.util.*

interface CreateCompany {
  fun handle(cmd: CreateCompanyCommand): CompanyResult
}

@Service
class CreateCompanyService(
  private val companies: Companies,
  private val exactOnlineSync: ExactOnlineSync,
  private val companySyncRepository: CompanySyncRepository,
) : CreateCompany {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Transactional
  override fun handle(cmd: CreateCompanyCommand): CompanyResult {
    // Check for conflicts with active companies
    cmd.chamberOfCommerceId?.let { kvk ->
      if (companies.existsByChamberOfCommerceId(kvk)) {
        throw DuplicateKeyException("KVK nummer $kvk is al in gebruik.")
      }
    }

    cmd.vihbNumber?.let { vihb ->
      if (companies.existsByVihbNumber(vihb.value)) {
        throw DuplicateKeyException("VIHB nummer ${vihb.value} is al in gebruik.")
      }
    }

    // Check for conflicts with soft-deleted companies
    cmd.chamberOfCommerceId?.let { kvk ->
      companies.findDeletedByChamberOfCommerceId(kvk)?.let { deletedCompany ->
        throw SoftDeletedCompanyConflictException(
          deletedCompanyId = deletedCompany.companyId,
          conflictField = "chamberOfCommerceId",
          conflictValue = kvk,
          message = "KVK nummer $kvk is gekoppeld aan een verwijderd bedrijf."
        )
      }
    }

    cmd.vihbNumber?.let { vihb ->
      companies.findDeletedByVihbNumber(vihb.value)?.let { deletedCompany ->
        throw SoftDeletedCompanyConflictException(
          deletedCompanyId = deletedCompany.companyId,
          conflictField = "vihbNumber",
          conflictValue = vihb.value,
          message = "VIHB nummer ${vihb.value} is gekoppeld aan een verwijderd bedrijf."
        )
      }
    }

    cmd.processorId?.let { processorId ->
      companies.findDeletedByProcessorId(processorId.number)?.let { deletedCompany ->
        throw SoftDeletedCompanyConflictException(
          deletedCompanyId = deletedCompany.companyId,
          conflictField = "processorId",
          conflictValue = processorId.number,
          message = "Verwerkersnummer ${processorId.number} is gekoppeld aan een verwijderd bedrijf."
        )
      }
    }

    val company = Company(
      companyId = CompanyId(UUID.randomUUID()),
      name = cmd.name,
      chamberOfCommerceId = cmd.chamberOfCommerceId,
      vihbNumber = cmd.vihbNumber,
      processorId = cmd.processorId,
      address = cmd.address,
      phone = cmd.phone,
      email = cmd.email,
      roles = cmd.roles,
    )

    val savedCompany = companies.create(company)

    // Sync to Exact Online - catch exceptions to prevent rollback of company creation
    try {
      exactOnlineSync.syncCompany(savedCompany)
    } catch (e: Exception) {
      logger.error("Failed to sync company ${savedCompany.companyId.uuid} to Exact Online", e)

      // Persist failed sync record
      val syncDto = CompanySyncDto(
        companyId = savedCompany.companyId.uuid,
        syncStatus = SyncStatus.FAILED,
        syncedFromSourceAt = Instant.now(),
        syncErrorMessage = "${e::class.simpleName}: ${e.message}",
      )
      companySyncRepository.save(syncDto)
    }

    return CompanyResult(companyId = savedCompany.companyId.uuid)
  }
}
