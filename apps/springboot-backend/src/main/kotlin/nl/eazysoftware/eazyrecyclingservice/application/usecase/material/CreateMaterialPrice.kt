package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

interface CreateMaterialPrice {
    fun handle(cmd: MaterialPriceCommand): MaterialPrice
}

@Service
class CreateMaterialPriceService(
    private val materialPrices: MaterialPrices,
    private val materials: Materials
) : CreateMaterialPrice {

    @Transactional
    override fun handle(cmd: MaterialPriceCommand): MaterialPrice {
        val now = Clock.System.now()
        
        val material = materials.getMaterialById(cmd.materialId)
            ?: throw IllegalArgumentException("Material with id ${cmd.materialId} not found")

        val price = MaterialPrice(
            id = null,
            materialId = cmd.materialId,
            materialCode = material.code,
            materialName = material.name,
            price = cmd.price,
            currency = cmd.currency,
            validFrom = now,
            validTo = null
        )

        return materialPrices.createPrice(price)
    }
}
