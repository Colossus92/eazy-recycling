package nl.eazysoftware.eazyrecyclingservice.application.query

import nl.eazysoftware.eazyrecyclingservice.application.usecase.material.MaterialPriceResult
import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice
import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import org.springframework.stereotype.Service

interface GetMaterialPrices {
    fun getAllActive(): List<MaterialPriceResult>
    fun getById(id: Long): MaterialPriceResult?
    fun getActiveByMaterialId(materialId: Long): List<MaterialPriceResult>
}

@Service
class GetMaterialPricesService(
    private val materialPrices: MaterialPrices
) : GetMaterialPrices {

    override fun getAllActive(): List<MaterialPriceResult> {
        return materialPrices.getAllActivePrices().map { it.toResult() }
    }

    override fun getById(id: Long): MaterialPriceResult? {
        return materialPrices.getPriceById(id)?.toResult()
    }

    override fun getActiveByMaterialId(materialId: Long): List<MaterialPriceResult> {
        return materialPrices.getActivePricesByMaterialId(materialId).map { it.toResult() }
    }

    private fun MaterialPrice.toResult() = MaterialPriceResult(
        id = this.id!!,
        materialId = this.materialId,
        price = this.price,
        currency = this.currency,
        validFrom = this.validFrom.toString(),
        validTo = this.validTo?.toString()
    )
}
