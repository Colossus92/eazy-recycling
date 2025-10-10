package nl.eazysoftware.eazyrecyclingservice.application.usecase

import nl.eazysoftware.eazyrecyclingservice.domain.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.model.WeightTicketId
import nl.eazysoftware.eazyrecyclingservice.domain.model.WeightTicketStatus
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import nl.eazysoftware.eazyrecyclingservice.domain.transport.LicensePlate
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Goods
import nl.eazysoftware.eazyrecyclingservice.domain.waste.WasteStreamNumber
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Weight
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

interface CreateWeightTicket {
  fun handle(cmd: CreateWeightTicketCommand): WeightTicketResult
}

/**
 * Command for creating a weight ticket.
 * Contains GoodsCommand with only waste stream reference - full WasteStream will be fetched in the service.
 */
data class CreateWeightTicketCommand(
  val carrierParty: CompanyId,
  val consignorParty: CompanyId,
  val pickupParty: CompanyId,
  val truckId: LicensePlate?,
  val goods: List<GoodsCommand>,
  val reclamation: String,
  val note: Note?,
  val loadingAddress: Address,
  val unloadingAddress: Address
)

/**
 * Command for goods, containing only the waste stream number.
 * The service will fetch the complete WasteStream and validate it.
 */
data class GoodsCommand(
  val wasteStreamNumber: WasteStreamNumber,
  val weight: Weight
)

data class WeightTicketResult(val id: WeightTicketId)

@Service
class CreateWeightTicketService(
  private val weightTicketRepo: WeightTickets,
  private val wasteStreamRepo: WasteStreams,
  private val clock: Clock
) : CreateWeightTicket {

  @Transactional
  override fun handle(cmd: CreateWeightTicketCommand): WeightTicketResult {
    val id = weightTicketRepo.nextId()

    // Fetch and validate WasteStreams for all goods
    val goods = cmd.goods.map { goodsCmd ->
      val wasteStream = wasteStreamRepo.findByNumber(goodsCmd.wasteStreamNumber)
        ?: throw IllegalArgumentException(
          "Afvalstroom met nummer ${goodsCmd.wasteStreamNumber.number} niet gevonden"
        )

      require(cmd.consignorParty == wasteStream.consignorParty) {
        "Afzender weging (${cmd.consignorParty}) komt niet overeen met afzender afvalstroomnummer: ${wasteStream.consignorParty}"
      }

      Goods(
        id = 0L, // Will be assigned by persistence layer
        waste = wasteStream,
        weight = goodsCmd.weight
      )
    }

    val now = clock.instant().atZone(clock.zone)
    val ticket = WeightTicket(
      id = id,
      carrierParty = cmd.carrierParty,
      consignorParty = cmd.consignorParty,
      truck = cmd.truckId ?: throw IllegalArgumentException("Truck ID is verplicht"),
      goods = goods,
      reclamation = cmd.reclamation,
      note = cmd.note,
      status = WeightTicketStatus.DRAFT,
      createdAt = now,
      updatedAt = null,
      weightedAt = now
    )

    weightTicketRepo.save(ticket)
    return WeightTicketResult(id)
  }
}
