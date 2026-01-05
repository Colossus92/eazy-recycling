package nl.eazysoftware.eazyrecyclingservice.application.usecase.weightticket

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import jakarta.persistence.EntityNotFoundException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import nl.eazysoftware.eazyrecyclingservice.domain.model.address.Location
import nl.eazysoftware.eazyrecyclingservice.domain.model.waste.Consignor
import nl.eazysoftware.eazyrecyclingservice.domain.model.weightticket.WeightTicket
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Companies
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WasteStreams
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.WeightTickets
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.toJavaInstant

interface CompleteWeightTicket {
  fun handle(cmd: CompleteWeightTicketCommand)
}

data class CompleteWeightTicketCommand(
  val weightTicketNumber: Long,
)

@Service
class CompleteWeightTicketService(
  private val weightTickets: WeightTickets,
  private val companies: Companies,
  private val wasteStreams: WasteStreams,
  private val supabaseClient: SupabaseClient,
) : CompleteWeightTicket {

  private val logger = LoggerFactory.getLogger(javaClass)
  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  override fun handle(cmd: CompleteWeightTicketCommand) {
    val weightTicket = weightTickets.findByNumber(cmd.weightTicketNumber)
      ?: throw EntityNotFoundException("Weegbon met nummer ${cmd.weightTicketNumber} bestaat niet")

    weightTicket.complete()
    weightTickets.save(weightTicket)

    // Trigger PDF generation asynchronously
    triggerPdfGeneration(weightTicket)
  }

  private fun triggerPdfGeneration(weightTicket: WeightTicket) {
    val pdfData = buildPdfData(weightTicket)
    
    coroutineScope.launch {
      try {
        logger.info("Triggering PDF generation for weight ticket ${weightTicket.id.number}")

        supabaseClient.functions.invoke(
          function = "weight-ticket-pdf-generator",
          body = pdfData
        )

        logger.info("PDF generation triggered successfully for weight ticket ${weightTicket.id.number}")
      } catch (e: Exception) {
        // Log error but don't fail the completion process
        logger.error("Failed to trigger PDF generation for weight ticket ${weightTicket.id.number}", e)
      }
    }
  }

  private fun buildPdfData(weightTicket: WeightTicket): WeightTicketPdfRequest {
    val dateFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")
      .withZone(ZoneId.of("Europe/Amsterdam"))

    // Fetch consignor company details
    val consignorParty = when (val consignor = weightTicket.consignorParty) {
      is Consignor.Company -> companies.findById(consignor.id)?.let { company ->
        PartyData(
          name = company.name,
          streetName = company.address.streetName.value,
          buildingNumber = company.address.buildingNumber,
          postalCode = company.address.postalCode.value,
          city = company.address.city.value
        )
      }
      is Consignor.Person -> PartyData(name = "Particulier")
    }

    // Fetch carrier company details
    val carrierParty = weightTicket.carrierParty?.let { carrierId ->
      companies.findById(carrierId)?.let { company ->
        PartyData(
          name = company.name,
          streetName = company.address.streetName.value,
          buildingNumber = company.address.buildingNumber,
          postalCode = company.address.postalCode.value,
          city = company.address.city.value
        )
      }
    }

    // Build pickup location data
    val pickupLocation = weightTicket.pickupLocation?.let { buildLocationData(it) }

    // Build delivery location data  
    val deliveryLocation = weightTicket.deliveryLocation?.let { buildLocationData(it) }

    // Build lines with waste type names and calculate totals
    val wasteStreamNumbers = weightTicket.lines.getLines().mapNotNull { it.waste }
    val wasteStreamMap = wasteStreams.findAllByNumber(wasteStreamNumbers)
      .associateBy { it.wasteStreamNumber }

    val lines = weightTicket.lines.getLines().map { line ->
      val wasteTypeName = line.waste?.let { wasteStreamMap[it]?.wasteType?.name } ?: "Onbekend"
      WeightTicketLineData(
        wasteTypeName = wasteTypeName,
        weightValue = line.weight.value.setScale(2, RoundingMode.HALF_UP).toDouble(),
        weightUnit = line.weight.unit.name.lowercase()
      )
    }

    // Calculate all weights in backend (business logic)
    val weging1 = weightTicket.getTotalLinesWeight().setScale(2, RoundingMode.HALF_UP).toDouble()
    val weging2 = weightTicket.secondWeighing?.value?.setScale(2, RoundingMode.HALF_UP)?.toDouble() ?: 0.0
    val grossWeight = (BigDecimal.valueOf(weging1) - BigDecimal.valueOf(weging2))
      .setScale(2, RoundingMode.HALF_UP).toDouble()
    val tarraWeight = weightTicket.tarraWeight?.value?.setScale(2, RoundingMode.HALF_UP)?.toDouble() ?: 0.0
    val nettoWeight = (BigDecimal.valueOf(grossWeight) - BigDecimal.valueOf(tarraWeight))
      .setScale(2, RoundingMode.HALF_UP).toDouble()

    return WeightTicketPdfRequest(
      weightTicket = WeightTicketData(
        id = weightTicket.id.id.toString(),
        number = weightTicket.id.number,
        truckLicensePlate = weightTicket.truckLicensePlate?.value ?: "",
        reclamation = weightTicket.reclamation,
        direction = weightTicket.direction.name,
        weightedAt = weightTicket.weightedAt?.let { dateFormatter.format(it.toJavaInstant()) },
        createdAt = weightTicket.createdAt?.let { dateFormatter.format(it.toJavaInstant()) } ?: "",
        weging1 = weging1,
        weging2 = weging2,
        grossWeight = grossWeight,
        tarraWeight = tarraWeight,
        nettoWeight = nettoWeight,
        weightUnit = weightTicket.tarraWeight?.unit?.name?.lowercase() ?: "kg"
      ),
      lines = lines,
      consignorParty = consignorParty,
      carrierParty = carrierParty,
      pickupLocation = pickupLocation,
      deliveryLocation = deliveryLocation
    )
  }

  private fun buildLocationData(location: Location): LocationData {
    return when (location) {
      is Location.DutchAddress -> LocationData(
        streetName = location.streetName(),
        buildingNumber = location.buildingNumber(),
        postalCode = location.postalCode().value,
        city = location.city()
      )
      is Location.Company -> LocationData(
        name = location.name,
        streetName = location.address.streetName.value,
        buildingNumber = location.address.buildingNumber,
        postalCode = location.address.postalCode.value,
        city = location.address.city.value
      )
      is Location.ProjectLocationSnapshot -> LocationData(
        streetName = location.streetName(),
        buildingNumber = location.buildingNumber(),
        postalCode = location.postalCode().value,
        city = location.city().value
      )
      is Location.ProximityDescription -> LocationData(
        name = location.description,
        postalCode = location.postalCodeDigits,
        city = location.city.value
      )
      is Location.NoLocation -> LocationData()
    }
  }
}

@Serializable
data class WeightTicketPdfRequest(
  val weightTicket: WeightTicketData,
  val lines: List<WeightTicketLineData>,
  val consignorParty: PartyData?,
  val carrierParty: PartyData?,
  val pickupLocation: LocationData?,
  val deliveryLocation: LocationData?
)

@Serializable
data class WeightTicketData(
  val id: String,
  val number: Long,
  val truckLicensePlate: String,
  val reclamation: String?,
  val direction: String,
  val weightedAt: String?,
  val createdAt: String,
  val weging1: Double,
  val weging2: Double,
  val grossWeight: Double,
  val tarraWeight: Double,
  val nettoWeight: Double,
  val weightUnit: String
)

@Serializable
data class WeightTicketLineData(
  val wasteTypeName: String,
  val weightValue: Double,
  val weightUnit: String
)

@Serializable
data class PartyData(
  val name: String? = null,
  val streetName: String? = null,
  val buildingNumber: String? = null,
  val postalCode: String? = null,
  val city: String? = null
)

@Serializable
data class LocationData(
  val name: String? = null,
  val streetName: String? = null,
  val buildingNumber: String? = null,
  val postalCode: String? = null,
  val city: String? = null
)
