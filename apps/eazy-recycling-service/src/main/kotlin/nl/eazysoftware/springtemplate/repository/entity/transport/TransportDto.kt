package nl.eazysoftware.springtemplate.repository.entity.transport

import jakarta.persistence.*
import nl.eazysoftware.springtemplate.repository.entity.driver.Driver
import nl.eazysoftware.springtemplate.repository.entity.truck.Truck
import nl.eazysoftware.springtemplate.repository.entity.waybill.LocationDto
import nl.eazysoftware.springtemplate.repository.entity.waybill.WaybillDto
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "transports")
data class TransportDto(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "waybill_id", referencedColumnName = "uuid", nullable = true)
    val waybill: WaybillDto? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "custom_origin_id", referencedColumnName = "id", nullable = true)
    val customOrigin: LocationDto? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "custom_destination_id", referencedColumnName = "id", nullable = true)
    val customDestination: LocationDto? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "truck_id", referencedColumnName = "license_plate", nullable = false)
    val truck: Truck,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", referencedColumnName = "id", nullable = false)
    val driver: Driver,

    @Column(nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now()
)