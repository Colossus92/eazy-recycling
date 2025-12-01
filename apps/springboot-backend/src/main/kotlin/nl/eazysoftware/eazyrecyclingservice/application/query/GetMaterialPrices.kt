package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import org.springframework.stereotype.Service

interface GetMaterialPrices {
    fun getAllActive(): List<MaterialPrice>
    fun getById(id: Long): MaterialPrice?
    fun getActiveByMaterialId(materialId: Long): List<MaterialPrice>
}

@Service
class GetMaterialPricesService(
    private val materialPrices: MaterialPrices
) : GetMaterialPrices {

    override fun getAllActive(): List<MaterialPrice> {
        return materialPrices.getAllActivePrices()
    }

    override fun getById(id: Long): MaterialPrice? {
        return materialPrices.getPriceById(id)
    }

    override fun getActiveByMaterialId(materialId: Long): List<MaterialPrice> {
        return materialPrices.getActivePricesByMaterialId(materialId)
    }
}
