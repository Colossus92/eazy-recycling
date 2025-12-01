package nl.eazysoftware.eazyrecyclingservice.domain.model.material

import kotlin.time.Instant

data class Material(
  val id: Long?,
  val code: String,
  val name: String,
  val materialGroupId: Long,
  val unitOfMeasure: String,
  val vatCode: String,
  val status: String,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
)
