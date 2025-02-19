package nl.eazysoftware.springtemplate.repository.entity.goods

import jakarta.persistence.*
import nl.eazysoftware.springtemplate.repository.entity.waybill.CompanyDto
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "goods")
data class GoodsDto(
    val id: String,

    @Id val uuid: UUID,
    val note: String,

    @OneToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "goods_item_id", referencedColumnName = "id")
    val goodsItem: GoodsItemDto,

    /**
     * The party actually disposing the goods
     */
    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "consignee_party_id", referencedColumnName = "id")
    val consigneeParty: CompanyDto,

    @ManyToOne(cascade = [CascadeType.ALL])
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
