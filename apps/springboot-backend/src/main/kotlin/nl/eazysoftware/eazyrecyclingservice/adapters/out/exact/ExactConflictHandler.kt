package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Handles conflict creation and management for Exact Online sync operations.
 */
@Component
class ExactConflictHandler(
  private val companySyncRepository: CompanySyncRepository
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Create or update a conflict record for an Exact account that cannot be imported.
   * Uses REQUIRES_NEW to ensure the conflict is saved even if the parent transaction rolls back.
   * First checks if there's an existing sync record with the same exactGuid
   * and updates it instead of creating a new one.
   * Returns ProcessResult.Conflict.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun createValidationConflict(
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    errorMessage: String,
    conflictType: String = "VALIDATION_ERROR"
  ): ProcessResult {
    saveOrUpdateConflict(
      account = account,
      companyId = null, // No company could be created
      conflictDetails = buildConflictDetailsWithoutCompany(
        conflictType = conflictType,
        account = account,
        errorMessage = errorMessage,
        extraDetails = mapOf(
          "chamberOfCommerce" to account.ChamberOfCommerce
        )
      )
    )
    logger.warn("Created/updated validation conflict for Exact account {} ({}): {}", account.Name, account.ID, errorMessage)
    return ProcessResult.Conflict
  }

  /**
   * Save or update a conflict/pending review record.
   * First checks if there's an existing sync record with the same exactGuid
   * and updates it instead of creating a new one.
   */
  fun saveOrUpdateConflict(
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    companyId: UUID?,
    conflictDetails: Map<String, Any>,
    syncStatus: SyncStatus = SyncStatus.CONFLICT
  ) {
    // Try to find existing sync record by exactGuid
    val existingSync = companySyncRepository.findByExactGuid(account.ID)

    val syncRecord = if (existingSync != null) {
      // Update existing record
      existingSync.copy(
        companyId = companyId ?: existingSync.companyId,
        exactGuid = account.ID,
        syncStatus = syncStatus,
        syncedFromSourceAt = Instant.now(),
        conflictDetails = conflictDetails,
        updatedAt = Instant.now()
      )
    } else {
      // Create new record
      CompanySyncDto(
        companyId = companyId,
        exactGuid = account.ID,
        syncStatus = syncStatus,
        syncedFromSourceAt = Instant.now(),
        conflictDetails = conflictDetails,
      )
    }

    companySyncRepository.save(syncRecord)
  }

  /**
   * Build conflict details map with standard fields for display in the frontend.
   * Includes exactName, exactAddress, matchedCompanyName, matchedCompanyAddress when available.
   */
  fun buildConflictDetails(
    conflictType: String,
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    matchedCompany: Company,
    extraDetails: Map<String, Any?> = emptyMap()
  ): Map<String, Any> {
    val details = mutableMapOf<String, Any>(
      "conflictType" to conflictType,
      "exactName" to account.Name
    )

    if (account.Code != null) details["code"] = account.Code

    // Build Exact address from available fields
    buildExactAddress(account, details)

    // Add matched company info
    details["matchedCompanyName"] = matchedCompany.name

    // Build matched company address
    val matchedAddressParts = listOfNotNull(
      matchedCompany.address.streetName.value.takeIf { it.isNotBlank() },
      matchedCompany.address.buildingNumber.takeIf { it.isNotBlank() },
      matchedCompany.address.postalCode.value.takeIf { it.isNotBlank() },
      matchedCompany.address.city.value.takeIf { it.isNotBlank() }
    )
    if (matchedAddressParts.isNotEmpty()) {
      details["matchedCompanyAddress"] = matchedAddressParts.joinToString(" ")
    }

    // Add any extra details
    extraDetails.forEach { (key, value) ->
      if (value != null) {
        details[key] = value
      }
    }

    return details
  }

  /**
   * Build conflict details map without a matched company (for validation errors during creation).
   */
  fun buildConflictDetailsWithoutCompany(
    conflictType: String,
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    errorMessage: String,
    extraDetails: Map<String, Any?> = emptyMap()
  ): Map<String, Any> {
    val details = mutableMapOf<String, Any>(
      "conflictType" to conflictType,
      "exactName" to account.Name,
      "errorMessage" to errorMessage
    )

    // Build Exact address from available fields
    buildExactAddress(account, details)

    // Add any extra details
    extraDetails.forEach { (key, value) ->
      if (value != null) {
        details[key] = value
      }
    }

    return details
  }

  private fun buildExactAddress(
      account: ExactOnlineSyncAdapter.ExactSyncAccount,
      details: MutableMap<String, Any>
  ) {
    val exactAddressParts = listOfNotNull(
      account.AddressLine1?.takeIf { it.isNotBlank() },
      account.Postcode?.takeIf { it.isNotBlank() },
      account.City?.takeIf { it.isNotBlank() }
      )
    if (exactAddressParts.isNotEmpty()) {
      details["exactAddress"] = exactAddressParts.joinToString(", ")
      }
  }
}
