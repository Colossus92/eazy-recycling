package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

interface CreateMaterialPrice {
    fun handle(cmd: MaterialPriceCommand): MaterialPriceResult
}

@Service
class CreateMaterialPriceService(
    private val materialPrices: MaterialPrices
) : CreateMaterialPrice {

    @Transactional
    override fun handle(cmd: MaterialPriceCommand): MaterialPriceResult {
        val now = Clock.System.now()

        val price = MaterialPrice(
            id = null,
            materialId = cmd.materialId,
            price = cmd.price,
            currency = cmd.currency,
            validFrom = now,
            validTo = null
        )

        val savedPrice = materialPrices.createPrice(price)

        return MaterialPriceResult(
            id = savedPrice.id!!,
            materialId = savedPrice.materialId,
            price = savedPrice.price,
            currency = savedPrice.currency,
            validFrom = savedPrice.validFrom.toString(),
            validTo = savedPrice.validTo?.toString()
        )
    }
}
