package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.repository.entity.waybill.CompanyDto
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "goods")
data class GoodsDto(
    /**
     * Waybill ID field
     */
    val id: String,

    /**
     * Waybill UUID field
     */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val uuid: UUID? = null,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "goods_item_id", referencedColumnName = "id")
    val goodsItem: GoodsItemDto,

    /**
     * The classification of the consignor, this can be:
     *      1 = Ontdoener
     *      2 = Ontvanger
     *      3 = Handelaar
     *      4 = Bemiddelaar
     */
    @Column(name = "consignor_classification", nullable = false)
    val consignorClassification: Int,

    /**
     * The party receiving the goods after the transport is finished.
     */
    @ManyToOne
    @JoinColumn(name = "consignee_party_id", referencedColumnName = "id")
    val consigneeParty: CompanyDto,

    /**
     * The party actually disposing the goods, this does not have to be the same company or location at the pickup location.
     */
    @ManyToOne
    @JoinColumn(name = "pickup_party_id", referencedColumnName = "id")
    val pickupParty: CompanyDto,

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
