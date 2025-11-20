package nl.eazysoftware.eazyrecyclingservice.domain.ports.out

import nl.eazysoftware.eazyrecyclingservice.domain.model.material.MaterialPrice

interface MaterialPrices {
    fun getAllActivePrices(): List<MaterialPrice>
    fun getPriceById(id: Long): MaterialPrice?
    fun getActivePricesByMaterialId(materialId: Long): List<MaterialPrice>
    fun createPrice(price: MaterialPrice): MaterialPrice
    fun updatePrice(id: Long, price: MaterialPrice): MaterialPrice
    fun deletePrice(id: Long)
}
