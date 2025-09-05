package nl.eazysoftware.eazyrecyclingservice.repository.entity.transport

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "signatures")
data class SignaturesDto(

    @Id
    @Column(name = "transport_id")
    val transportId: UUID,

    var consignorEmail: String? = null,
    var consignorSignedAt: ZonedDateTime? = null,

    var pickupEmail: String? = null,
    var pickupSignedAt: ZonedDateTime? = null,

    var carrierEmail: String? = null,
    var carrierSignedAt: ZonedDateTime? = null,

    var consigneeEmail: String? = null,
    var consigneeSignedAt: ZonedDateTime? = null,
)
