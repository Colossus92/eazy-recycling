package nl.eazysoftware.eazyrecyclingservice.domain.waste

import kotlinx.datetime.Clock
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import kotlinx.datetime.Instant

data class WasteStream(
  val wasteStreamNumber: WasteStreamNumber,
  val wasteType: WasteType,
  val collectionType: WasteCollectionType = WasteCollectionType.DEFAULT,
  val pickupLocation: PickupLocation,
  val deliveryLocation: DeliveryLocation,
  /**
   * Dutch: Afzender
   */
  val consignorParty: Consignor,
  /**
   * Dutch: Ontdoener
   */
  val pickupParty: CompanyId,
  /**
   * Dutch: Handelaar
   */
  val dealerParty: CompanyId? = null,
  /**
   * Dutch: Inzamelaar
   */
  val collectorParty: CompanyId? = null,
  /**
   * Dutch: Bemiddelaar
   */
  val brokerParty: CompanyId? = null,

  val lastActivityAt: Instant = Clock.System.now(),

  var status: WasteStreamStatus = WasteStreamStatus.DRAFT,
) {
  init {
    require(collectorParty == null || brokerParty == null) {
      "Een afvalstroomnummer kan niet zowel een handelaar als een bemiddelaar hebben"
    }

    // Origin location validation: either has location with default collection + company,
    // or no location with non-default collection or person
    val hasPickupLocation = pickupLocation !is PickupLocation.NoPickupLocation
    val isDefaultCollection = collectionType == WasteCollectionType.DEFAULT
    val isCompanyConsignor = consignorParty is Consignor.Company

    require(
      (hasPickupLocation && isDefaultCollection && isCompanyConsignor) ||
      (!hasPickupLocation && (!isDefaultCollection || !isCompanyConsignor))
    ) {
      if (hasPickupLocation) {
        "Locatie van herkomst is alleen toegestaan bij normale inzameling en zakelijke ontdoener"
      } else {
        "Locatie van herkomst is verplicht bij normale inzameling en zakelijke ontdoener"
      }
    }

    require( collectionType == WasteCollectionType.DEFAULT || collectorParty != null) {
      "Als er RouteInzameling of InzamelaarsRegeling wordt toegepast dan moet de inzamelaar zijn gevuld"
    }

    require(consignorParty is Consignor.Company || collectionType == WasteCollectionType.DEFAULT) {
      "Als de ontdoener een particulier is dan mag route inzameling en inzamelaarsregeling niet worden toegepast"
    }

    require(wasteStreamNumber.number.substring(0, 5) == deliveryLocation.processorPartyId.number) {
      "De eerste 5 posities van het Afvalstroomnummer moeten gelijk zijn aan de LocatieOntvangst."
    }
  }

  fun getEffectiveStatus(): EffectiveStatus {
    return EffectiveStatusPolicy.compute(status, lastActivityAt, Clock.System.now())
  }

  fun putInactive() {
    status = WasteStreamStatus.INACTIVE
  }
}

sealed interface Consignor {
  data class Company(val id: CompanyId) : Consignor
  data object Person : Consignor
}

data class DeliveryLocation(
  val processorPartyId: ProcessorPartyId,
)

data class ProcessorPartyId(
  val number: String
) {
  init {
    require(number.length == 5) {
      "Het verwerkersnummer moet exact 5 tekens lang zijn, maar is: $number"
    }
  }
}


enum class WasteCollectionType {
  DEFAULT,
  ROUTE,
  COLLECTORS_SCHEME,
}

data class WasteStreamNumber(
  val number: String,
) {
  init {
    require(number.length == 12) {
      "Een afvalstroomnummer dient 12 tekens lang te zijn"
    }
  }
}
