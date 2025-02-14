package nl.eazysoftware.springtemplate.repository.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class PlanningEntry(
    val id: String,
    @Id val uuid: UUID,
    val note: String,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "consignee_party_id", referencedColumnName = "id")
    val consigneeParty: Company,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "consignor_party_id", referencedColumnName = "id")
    val consignorParty: Company,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "carrier_party_id", referencedColumnName = "id")
    val carrierParty: Company,
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "pickup_party_id", referencedColumnName = "id")
    val pickupParty: Company,
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "goods_item_id", referencedColumnName = "id")
    val goodsItem: GoodsItem,
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "delivery_location_id", referencedColumnName = "id")
    val deliveryLocation: Location,
    val deliveryDateTime: LocalDateTime,
    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "pickup_location_id", referencedColumnName = "id")
    val pickupLocation: Location,
    val pickupDateTime: LocalDateTime,
    val licensePlate: String,
) {
}