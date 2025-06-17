package nl.eazysoftware.eazyrecyclingservice.repository.entity.transport

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "signatures")
data class SignaturesDto(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val transportId: UUID? = null,

    var consignorSignature: String? = null,
    var consignorEmail: String? = null,
    var consignorSignedAt: ZonedDateTime? = null,

    var pickupSignature: String? = null,
    var pickupEmail: String? = null,
    var pickupSignedAt: ZonedDateTime? = null,

    var carrierSignature: String? = null,
    var carrierEmail: String? = null,
    var carrierSignedAt: ZonedDateTime? = null,

    var consigneeSignature: String? = null,
    var consigneeEmail: String? = null,
    var consigneeSignedAt: ZonedDateTime? = null,
)
