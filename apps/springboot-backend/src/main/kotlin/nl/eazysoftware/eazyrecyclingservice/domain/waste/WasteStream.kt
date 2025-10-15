package nl.eazysoftware.eazyrecyclingservice.domain.waste

import kotlinx.datetime.Clock
import nl.eazysoftware.eazyrecyclingservice.domain.model.company.CompanyId
import kotlinx.datetime.Instant

class WasteStream(
  val wasteStreamNumber: WasteStreamNumber,
  var wasteType: WasteType,
  var collectionType: WasteCollectionType = WasteCollectionType.DEFAULT,
  var pickupLocation: PickupLocation,
  var deliveryLocation: DeliveryLocation,
  /**
   * Dutch: Afzender
   */
  var consignorParty: Consignor,
  /**
   * Dutch: Ontdoener
   */
  var pickupParty: CompanyId,
  /**
   * Dutch: Handelaar
   */
  var dealerParty: CompanyId? = null,
  /**
   * Dutch: Inzamelaar
   */
  var collectorParty: CompanyId? = null,
  /**
   * Dutch: Bemiddelaar
   */
  var brokerParty: CompanyId? = null,

  var lastActivityAt: Instant = Clock.System.now(),

  var status: WasteStreamStatus = WasteStreamStatus.DRAFT,
) {
  init {
    validateBusinessRules(
      collectionType = collectionType,
      pickupLocation = pickupLocation,
      deliveryLocation = deliveryLocation,
      consignorParty = consignorParty,
      collectorParty = collectorParty,
      brokerParty = brokerParty
    )
  }

  private fun validateBusinessRules(
    collectionType: WasteCollectionType,
    pickupLocation: PickupLocation,
    deliveryLocation: DeliveryLocation,
    consignorParty: Consignor,
    collectorParty: CompanyId?,
    brokerParty: CompanyId?
  ) {
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

    require(collectionType == WasteCollectionType.DEFAULT || collectorParty != null) {
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

  /**
   * Updates the waste stream with new values.
   * Only allowed when status is DRAFT.
   * Parameters default to current field values, allowing partial updates.
   *
   * @throws IllegalStateException if the waste stream is not in DRAFT status
   */
  fun update(
    wasteType: WasteType = this.wasteType,
    collectionType: WasteCollectionType = this.collectionType,
    pickupLocation: PickupLocation = this.pickupLocation,
    deliveryLocation: DeliveryLocation = this.deliveryLocation,
    consignorParty: Consignor = this.consignorParty,
    pickupParty: CompanyId = this.pickupParty,
    dealerParty: CompanyId? = this.dealerParty,
    collectorParty: CompanyId? = this.collectorParty,
    brokerParty: CompanyId? = this.brokerParty
  ) {
    check(status == WasteStreamStatus.DRAFT) {
      "Afvalstroom kan alleen worden gewijzigd als de status DRAFT is. Huidige status: $status"
    }

    // Validate new state before applying changes
    validateBusinessRules(
      collectionType = collectionType,
      pickupLocation = pickupLocation,
      deliveryLocation = deliveryLocation,
      consignorParty = consignorParty,
      collectorParty = collectorParty,
      brokerParty = brokerParty
    )

    // Apply changes
    this.wasteType = wasteType
    this.collectionType = collectionType
    this.pickupLocation = pickupLocation
    this.deliveryLocation = deliveryLocation
    this.consignorParty = consignorParty
    this.pickupParty = pickupParty
    this.dealerParty = dealerParty
    this.collectorParty = collectorParty
    this.brokerParty = brokerParty
    this.lastActivityAt = Clock.System.now()
  }

  /**
   * Activates the waste stream.
   * Only allowed when status is DRAFT.
   *
   * @throws IllegalStateException if the waste stream is not in DRAFT status
   */
  fun activate() {
    check(status == WasteStreamStatus.DRAFT) {
      "Afvalstroom kan alleen worden geactiveerd vanuit DRAFT status. Huidige status: $status"
    }
    status = WasteStreamStatus.ACTIVE
  }

  /**
   * Marks the waste stream as deleted (inactive).
   * Can be called from DRAFT or ACTIVE status.
   *
   * @throws IllegalStateException if the waste stream is already INACTIVE
   */
  fun delete() {
    check(status != WasteStreamStatus.INACTIVE) {
      "Afvalstroom is al inactief en kan niet opnieuw worden verwijderd"
    }
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
