package nl.eazysoftware.eazyrecyclingservice.domain.model.material

import kotlin.time.Instant

data class MaterialGroup(
  val id: Long?,
  val code: String,
  val name: String,
  val description: String?,
  val createdAt: Instant? = null,
  val createdBy: String? = null,
  val updatedAt: Instant? = null,
  val updatedBy: String? = null,
)
