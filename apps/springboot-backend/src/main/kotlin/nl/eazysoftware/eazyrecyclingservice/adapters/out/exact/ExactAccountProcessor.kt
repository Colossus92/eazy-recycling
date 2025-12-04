package nl.eazysoftware.eazyrecyclingservice.adapters.out.exact

import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.City
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.DutchPostalCode
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.StreetName
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Company
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Email
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.PhoneNumber
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncDto
import nl.eazysoftware.eazyrecyclingservice.repository.exact.CompanySyncRepository
import nl.eazysoftware.eazyrecyclingservice.repository.exact.SyncStatus
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.*

/**
 * Result of processing an Exact account.
 */
sealed class ProcessResult {
  data object Created : ProcessResult()
  data object Updated : ProcessResult()
  data object Conflict : ProcessResult()
  data object PendingReview : ProcessResult()
}

/**
 * Helper component for processing individual Exact accounts in separate transactions.
 * This allows constraint violations to be caught and handled without rolling back the entire sync.
 */
@Component
class ExactAccountProcessor(
  private val companySyncRepository: CompanySyncRepository,
  private val companies: Companies,
  private val conflictHandler: ExactConflictHandler
) {
  private val logger = LoggerFactory.getLogger(javaClass)

  /**
   * Process a single Exact account in its own transaction.
   * Uses REQUIRES_NEW to ensure each account is processed independently.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  fun processAccount(
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    streetName: String,
    buildingNumber: String,
    buildingNumberAddition: String?
  ): ProcessResult {
    // Step 1: Try to find by exact_guid (direct link - highest confidence)
    val syncByGuid = companySyncRepository.findByExactGuid(account.ID)
    val syncByGuidCompanyId = syncByGuid?.companyId
    if (syncByGuid != null && syncByGuidCompanyId != null) {
      val existingCompany = companies.findById(CompanyId(syncByGuidCompanyId))
      if (existingCompany != null) {
        return updateExistingCompany(
          existingCompany,
          account,
          syncByGuid,
          streetName,
          buildingNumber,
          buildingNumberAddition
        )
      }
    }

    // Step 2: No exact_guid match found - create new company
    // Note: We no longer match by KVK number since Exact Online allows duplicate KVK numbers.
    // Each Exact account creates its own company record, linked by exact_guid. See ADR-0018.
    return createNewCompany(account, streetName, buildingNumber, buildingNumberAddition)
  }

  /**
   * Update an existing company with data from Exact Online.
   */
  private fun updateExistingCompany(
    existingCompany: Company,
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    syncRecord: CompanySyncDto,
    streetName: String,
    buildingNumber: String,
    buildingNumberAddition: String?
  ): ProcessResult {
    val updatedCompany = existingCompany.copy(
      code = account.Code?.trim(),
      name = account.Name,
      chamberOfCommerceId = account.ChamberOfCommerce,
      address = Address(
        streetName = StreetName(streetName),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        postalCode = DutchPostalCode(account.Postcode ?: "0000AA"),
        city = City(account.City ?: ""),
        country = account.Country ?: "NL"
      ),
      phone = account.Phone?.let { PhoneNumber(it) },
      email = account.Email?.let { Email(it) },
      isSupplier = account.IsSupplier ?: false,
      isCustomer = account.Status == "C"
    )

    try {
      companies.update(updatedCompany)
      companies.flush() // Flush to detect constraint violations immediately
    } catch (e: DataIntegrityViolationException) {
      // Constraint violation during update - create a conflict record
      val constraintMessage = extractConstraintMessage(e)
      logger.warn(
        "Constraint violation updating company {} ({}) from Exact GUID {}: {}",
        existingCompany.name,
        existingCompany.companyId.uuid,
        account.ID,
        constraintMessage
      )

      val conflictSync = syncRecord.copy(
        exactGuid = account.ID,
        syncStatus = SyncStatus.CONFLICT,
        syncedFromSourceAt = Instant.now(),
        conflictDetails = conflictHandler.buildConflictDetails(
          conflictType = "UPDATE_CONSTRAINT_VIOLATION",
          account = account,
          matchedCompany = existingCompany,
          extraDetails = mapOf(
            "errorMessage" to constraintMessage,
            "existingCompanyId" to existingCompany.companyId.uuid.toString()
          )
        ),
        updatedAt = Instant.now()
      )
      companySyncRepository.save(conflictSync)
      return ProcessResult.Conflict
    }

    // Update sync record
    val updatedSync = syncRecord.copy(
      exactGuid = account.ID,
      syncStatus = SyncStatus.OK,
      syncedFromSourceAt = Instant.now(),
      syncErrorMessage = null,
      conflictDetails = null,
      updatedAt = Instant.now()
    )
    companySyncRepository.save(updatedSync)

    logger.debug(
      "Updated company {} ({}) from Exact GUID {}",
      updatedCompany.name,
      existingCompany.companyId.uuid,
      account.ID
    )
    return ProcessResult.Updated
  }

  /**
   * Create a new company from Exact Online data.
   */
  private fun createNewCompany(
    account: ExactOnlineSyncAdapter.ExactSyncAccount,
    streetName: String,
    buildingNumber: String,
    buildingNumberAddition: String?
  ): ProcessResult {
    val newCompanyId = CompanyId(UUID.randomUUID())
    val company = Company(
      companyId = newCompanyId,
      code = account.Code?.trim(),
      name = account.Name,
      chamberOfCommerceId = account.ChamberOfCommerce,
      vihbNumber = null, // Not available from Exact
      processorId = null, // Not available from Exact
      address = Address(
        streetName = StreetName(streetName),
        buildingNumber = buildingNumber,
        buildingNumberAddition = buildingNumberAddition,
        postalCode = DutchPostalCode(account.Postcode ?: "0000AA"),
        city = City(account.City ?: ""),
        country = account.Country ?: "NL"
      ),
      roles = emptyList(), // Roles are managed locally
      phone = account.Phone?.let { PhoneNumber(it) },
      email = account.Email?.let { Email(it) },
      isSupplier = account.IsSupplier ?: false,
      isCustomer = account.Status == "C"
    )

    try {
      companies.create(company)
      companies.flush() // Flush to detect constraint violations immediately
    } catch (e: DataIntegrityViolationException) {
      // Constraint violation during create - create a conflict record without company
      val constraintMessage = extractConstraintMessage(e)
      logger.warn(
        "Constraint violation creating company from Exact account {} ({}): {}",
        account.Name,
        account.ID,
        constraintMessage
      )
      return conflictHandler.createValidationConflict(account, constraintMessage, "CREATE_CONSTRAINT_VIOLATION")
    }

    // Create or update sync record linking our company ID to the Exact GUID
    // Check if there's an existing sync record (e.g., from a previous conflict) and update it
    val existingSync = companySyncRepository.findByExactGuid(account.ID)

    val syncDto = existingSync?.copy(
        companyId = newCompanyId.uuid,
        exactGuid = account.ID,
        syncStatus = SyncStatus.OK,
        syncedFromSourceAt = Instant.now(),
        syncErrorMessage = null,
        conflictDetails = null,
        updatedAt = Instant.now()
    )
        ?: CompanySyncDto(
          companyId = newCompanyId.uuid,
          exactGuid = account.ID,
          syncStatus = SyncStatus.OK,
          syncedFromSourceAt = Instant.now()
        )
    companySyncRepository.save(syncDto)

    logger.debug("Created company {} ({}) from Exact GUID {}", company.name, newCompanyId.uuid, account.ID)
    return ProcessResult.Created
  }


  /**
   * Extract a user-friendly constraint violation message from the exception.
   */
  private fun extractConstraintMessage(e: DataIntegrityViolationException): String {
    val message = e.message ?: return "Database constraint violation"

    // Try to extract the constraint name and details
    return when {
      message.contains("companies_chamber_of_commerce_id_key") ->
        "KVK-nummer is al in gebruik door een ander bedrijf"
      message.contains("companies_vihb_id_key") ->
        "VIHB-nummer is al in gebruik door een ander bedrijf"
      message.contains("duplicate key") -> {
        // Extract the key value if possible
        val keyMatch = Regex("""Key \((\w+)\)=\(([^)]+)\)""").find(message)
        if (keyMatch != null) {
          val (field, value) = keyMatch.destructured
          "Waarde '$value' voor veld '$field' is al in gebruik"
        } else {
          "Dubbele waarde gevonden"
        }
      }
      else -> "Database constraint violation: ${e.mostSpecificCause.message}"
    }
  }
}
