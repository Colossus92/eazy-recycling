package nl.eazysoftware.eazyrecyclingservice.application.usecase.material

import nl.eazysoftware.eazyrecyclingservice.domain.ports.out.MaterialPrices
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

interface DeleteMaterialPrice {
    fun handle(id: Long)
}

@Service
class DeleteMaterialPriceService(
    private val materialPrices: MaterialPrices
) : DeleteMaterialPrice {

    @Transactional
    override fun handle(id: Long) {
        materialPrices.deletePrice(id)
    }
}
