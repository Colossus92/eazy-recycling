package nl.eazysoftware.eazyrecyclingservice.domain.model.material

import java.util.*
import kotlin.time.Instant

data class Material(
  val id: UUID?,
  val code: String,
  val name: String,
  val materialGroupId: UUID?,
  val unitOfMeasure: String,
  val vatCode: String,
  val purchaseAccountNumber: String?,
  val salesAccountNumber: String?,
  val status: String,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
)
