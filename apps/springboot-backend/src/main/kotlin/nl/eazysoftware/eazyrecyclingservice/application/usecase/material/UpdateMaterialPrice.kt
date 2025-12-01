package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.Materials
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.time.Clock

interface UpdateMaterialPrice {
    fun handle(id: Long, cmd: MaterialPriceCommand): MaterialPrice
}

@Service
class UpdateMaterialPriceService(
    private val materialPrices: MaterialPrices,
    private val materials: Materials
) : UpdateMaterialPrice {

    @Transactional
    override fun handle(id: Long, cmd: MaterialPriceCommand): MaterialPrice {
        val material = materials.getMaterialById(cmd.materialId)
            ?: throw IllegalArgumentException("Material with id ${cmd.materialId} not found")
        
        val price = MaterialPrice(
            id = null,
            materialId = cmd.materialId,
            materialCode = material.code,
            materialName = material.name,
            price = cmd.price,
            currency = cmd.currency,
            validFrom = Clock.System.now(),
            validTo = null
        )

        return materialPrices.updatePrice(id, price)
    }
}
