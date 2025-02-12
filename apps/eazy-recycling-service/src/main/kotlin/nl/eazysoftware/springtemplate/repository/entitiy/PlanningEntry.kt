package nl.eazysoftware.springtemplate.repository.entitiy

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import java.time.LocalDateTime
import java.util.UUID

@Entity
data class PlanningEntry(
    val id: String,
    @Id val uuid: UUID,
    val note: String,
    @ManyToOne
    @JoinColumn(name = "consignee_party_id", referencedColumnName = "id")
    val consigneeParty: Company,
    @ManyToOne
    @JoinColumn(name = "consignor_party_id", referencedColumnName = "id")
    val consignorParty: Company,
    @ManyToOne
    @JoinColumn(name = "carrier_party_id", referencedColumnName = "id")
    val carrierParty: Company,
    @ManyToOne
    @JoinColumn(name = "pickup_party_id", referencedColumnName = "id")
    val pickupParty: Company,
    @OneToOne
    @JoinColumn(name = "goods_item_id", referencedColumnName = "id")
    val goodsItem: GoodsItem,
    @OneToOne
    @JoinColumn(name = "delivery_location_id", referencedColumnName = "id")
    val deliveryLocation: Location,
    val deliveryDateTime: LocalDateTime,
    @OneToOne
    @JoinColumn(name = "pickup_location_id", referencedColumnName = "id")
    val pickupLocation: Location,
    val pickupDateTime: LocalDateTime,
    val licensePlate: String,
) {
}