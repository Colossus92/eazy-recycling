package nl.eazysoftware.eazyrecyclingservice.repository.entity.goods

import jakarta.persistence.Embeddable

@Embeddable
data class TransportGoodsDto(

    val wasteStreamNumber: String,
    val netNetWeight: Double,
    val unit: String,
    val quantity: Int,
)
