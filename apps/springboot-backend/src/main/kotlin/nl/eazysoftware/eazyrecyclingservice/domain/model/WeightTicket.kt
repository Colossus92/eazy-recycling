package nl.eazysoftware.eazyrecyclingservice.domain.model

import io.github.jan.supabase.auth.user.UserInfo
import nl.eazysoftware.eazyrecyclingservice.domain.address.Address
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.Person
import nl.eazysoftware.eazyrecyclingservice.domain.model.misc.Note
import nl.eazysoftware.eazyrecyclingservice.domain.transport.Truck
import nl.eazysoftware.eazyrecyclingservice.domain.waste.Goods
import java.time.ZonedDateTime

class WeightTicket(
  val id: WeightTicketId,
//  val orderId: OrderId
  carrierParty: CompanyId,
  consignorParty: CompanyId,
  consigneeParty: CompanyId,
  pickupParty: CompanyId,
  truck: Truck,
  driver: Person,
  goods: List<Goods>,
  /**
   * Small not for display on a printed weight ticket
   */
  reclamation: String,
  /**
   * Larger note, not displayed on a printed weight ticket
   */
  note: Note?,
  loadingAddress: Address,
  unloadingAddress: Address,
  status: WeightTicketStatus,
  createdAt: ZonedDateTime,
  updatedAt: ZonedDateTime?,
  weightedAt: ZonedDateTime
) {

}

enum class WeightTicketStatus {
  DRAFT,
  PROCESSED,
  COMPLETED,
  CANCELLED,
}

data class WeightTicketId(
  val number: Int
)
