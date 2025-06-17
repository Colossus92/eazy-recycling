package nl.eazysoftware.eazyrecyclingservice.repository.entity.transport

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.UUID

data class SignaturesDto(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val consignorSignature: String? = null,
    val consignorEmail: String? = null,
    val consignorSignedAt: LocalDateTime? = null,

    val pickupSignature: String? = null,
    val pickupEmail: String? = null,
    val pickupSignedAt: LocalDateTime? = null,

    val carrierSignature: String? = null,
    val carrierEmail: String? = null,
    val carrierSignedAt: LocalDateTime? = null,

    val consigneeSignature: String? = null,
    val consigneeEmail: String? = null,
    val consigneeSignedAt: LocalDateTime? = null,
)
