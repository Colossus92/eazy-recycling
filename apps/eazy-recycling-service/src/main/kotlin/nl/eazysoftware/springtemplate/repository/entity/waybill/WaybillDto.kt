package nl.eazysoftware.springtemplate.repository.entity.waybill

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "waybills")
data class WaybillDto(
    val id: String,
    @Id val uuid: UUID,
    val note: String,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "consignee_party_id", referencedColumnName = "id")
    val consigneeParty: CompanyDto,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "consignor_party_id", referencedColumnName = "id")
    val consignorParty: CompanyDto,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "carrier_party_id", referencedColumnName = "id")
    val carrierParty: CompanyDto,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "pickup_party_id", referencedColumnName = "id")
    val pickupParty: CompanyDto,
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "goods_item_id", referencedColumnName = "id")
    val goodsItem: GoodsItemDto,
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "delivery_location_id", referencedColumnName = "id")
    val deliveryLocation: LocationDto,
    val deliveryDateTime: LocalDateTime,
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "pickup_location_id", referencedColumnName = "id")
    val pickupLocation: LocationDto,
    val pickupDateTime: LocalDateTime,
    val licensePlate: String,

    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null
) {
    @PrePersist
    fun prePersist() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now()
        }
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}