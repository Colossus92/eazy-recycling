package nl.eazysoftware.eazyrecyclingservice.application.usecase.company

import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId

data class SoftDeletedCompanyConflictException(
  val deletedCompanyId: CompanyId,
  val conflictField: String,
  val conflictValue: String,
  override val message: String
) : RuntimeException(message)
