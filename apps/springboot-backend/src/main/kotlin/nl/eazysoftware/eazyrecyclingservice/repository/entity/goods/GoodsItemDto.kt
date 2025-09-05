package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.*

@Entity
@Table(name = "goods_items")
data class GoodsItemDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val wasteStreamNumber: String?,
    val netNetWeight: Int,
    val unit: String,
    val quantity: Int,
    val name: String,
    val euralCode: String,
    val processingMethodCode: String,
)
