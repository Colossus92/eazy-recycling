package nl.eazysoftware.springtemplate.repository.entity.transport

import jakarta.persistence.*
import nl.eazysoftware.springtemplate.repository.entity.driver.Driver
import nl.eazysoftware.springtemplate.repository.entity.truck.Truck
import nl.eazysoftware.springtemplate.repository.entity.waybill.CompanyDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import java.time.LocalDateTime
import java.util.*

enum class TransportType {
    EXCHANGE,
    PICKUP,
    EMPTY,
    DELIVERY,
    WAYBILL,
}

@Entity
@Table(name = "transports")
data class TransportDto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    val transportType: TransportType? = null,

    val containerType: String? = null,

    @ManyToOne(cascade = [CascadeType.ALL])
    @JoinColumn(name = "customer_id", referencedColumnName = "id")
    val customer: CompanyDto? = null,

    @ManyToOne(optional = true)
    @JoinColumn(name = "waybill_id", referencedColumnName = "uuid", nullable = true)
    val waybill: WaybillDto? = null,

    @ManyToOne(cascade = [CascadeType.ALL], optional = true)
    @JoinColumn(name = "custom_origin_id", referencedColumnName = "id", nullable = true)
    val customOrigin: LocationDto? = null,

    val pickupDateTime: LocalDateTime? = null,

    @ManyToOne(cascade = [CascadeType.ALL], optional = true)
    @JoinColumn(name = "custom_destination_id", referencedColumnName = "id", nullable = true)
    val customDestination: LocationDto? = null,

    val deliveryDateTime: LocalDateTime? = null,

    @ManyToOne(optional = false)
    @JoinColumn(name = "truck_id", referencedColumnName = "license_plate", nullable = false)
    val truck: Truck,

    @ManyToOne(optional = false)
    @JoinColumn(name = "driver_id", referencedColumnName = "id", nullable = false)
    val driver: Driver,

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)