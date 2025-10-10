package nl.eazysoftware.eazyrecyclingservice.repository.weightticket

import jakarta.persistence.*
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStream
import nl.eazysoftware.eazyrecyclingservice.repository.entity.goods.WasteStreamDto
import java.math.BigDecimal

@Entity
@Table(name = "weight_ticket_goods")
data class WeightTicketGoodsDto(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "weight_ticket_id", nullable = false)
    val weightTicket: WeightTicketDto,

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waste_stream_number", nullable = false)
    val wasteStream: WasteStreamDto,

    @Column(name = "weight", nullable = false, precision = 10, scale = 2)
    val weight: BigDecimal,

    @Column(name = "unit", nullable = false)
    val unit: String = "Kg"
)
