package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

interface CreateMaterialPrice {
    fun handle(cmd: MaterialPriceCommand): MaterialPrice
}

@Service
class CreateMaterialPriceService(
    private val materialPrices: MaterialPrices
) : CreateMaterialPrice {

    @Transactional
    override fun handle(cmd: MaterialPriceCommand): MaterialPrice {
        val now = Clock.System.now()

        val price = MaterialPrice(
            id = null,
            materialId = cmd.materialId,
            price = cmd.price,
            currency = cmd.currency,
            validFrom = now,
            validTo = null
        )

        return materialPrices.createPrice(price)
    }
}
