package nl.eazysoftware.eazyrecyclingservice.application.query

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.UUID

data class WasteStreamDetailView(
    val wasteStreamNumber: String,
    val wasteType: WasteTypeView,
    val collectionType: String,
    val pickupLocation: PickupLocationView?,
    val deliveryLocation: DeliveryLocationView,
    val consignorParty: ConsignorView,
    val pickupParty: CompanyView,
    val dealerParty: CompanyView?,
    val collectorParty: CompanyView?,
    val brokerParty: CompanyView?
)

data class WasteTypeView(
    val name: String,
    val euralCode: EuralCodeView,
    val processingMethod: ProcessingMethodView
)

data class EuralCodeView(
    val code: String,
    val description: String?
)

data class ProcessingMethodView(
    val code: String,
    val description: String?
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = PickupLocationView.DutchAddressView::class, name = "dutch_address"),
    JsonSubTypes.Type(value = PickupLocationView.ProximityDescriptionView::class, name = "proximity")
)
sealed class PickupLocationView {
    data class DutchAddressView(
        val streetName: String,
        val postalCode: String,
        val buildingNumber: String,
        val buildingNumberAddition: String?,
        val city: String,
        val country: String
    ) : PickupLocationView()

    data class ProximityDescriptionView(
        val postalCodeDigits: String,
        val city: String,
        val description: String,
        val country: String
    ) : PickupLocationView()
}

data class DeliveryLocationView(
    val processorPartyId: String,
    val processor: CompanyView?
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = ConsignorView.CompanyConsignorView::class, name = "company"),
    JsonSubTypes.Type(value = ConsignorView.PersonConsignorView::class, name = "person")
)
sealed class ConsignorView {
    data class CompanyConsignorView(val company: CompanyView) : ConsignorView()
    data object PersonConsignorView : ConsignorView()
}

data class CompanyView(
    val id: UUID,
    val name: String,
    val chamberOfCommerceId: String?,
    val vihbId: String?,
    val processorId: String?,
    val address: AddressView
)

data class AddressView(
    val street: String,
    val houseNumber: String,
    val houseNumberAddition: String?,
    val postalCode: String,
    val city: String,
    val country: String
)
