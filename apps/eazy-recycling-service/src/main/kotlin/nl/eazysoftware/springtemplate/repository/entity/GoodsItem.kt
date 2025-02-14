package nl.eazysoftware.springtemplate.repository.entity

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id

@Entity
data class GoodsItem(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    val wasteStreamNumber: String,
    val netNetWeight: Int,
    val unit: String,
    val quantity: Int,
    val name: String,
    val euralCode: String,
    val containerNumber: String,
) {

}
